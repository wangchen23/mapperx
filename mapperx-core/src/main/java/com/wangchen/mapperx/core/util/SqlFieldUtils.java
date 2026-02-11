package com.wangchen.mapperx.core.util;


import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.LogicDelete;
import com.wangchen.mapperx.core.annotation.PrimaryKey;
import com.wangchen.mapperx.core.conditions.Condition;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SqlFieldUtils
 *
 * @author chenwang
 * @date 2026/1/27 16:49
 **/
public class SqlFieldUtils {

    /**
     * 生成 INSERT 的字段列表：`"user_name, create_time"`
     */
    public static String buildInsertColumns(Object params, boolean selective) {
        List<?> extractList = MybatisUtils.extractList(params);
        Object entity;
        if (extractList == null || extractList.isEmpty()) {
            entity = params;
        } else {
            entity = extractList.get(0);
        }
        MetaObject meta = SystemMetaObject.forObject(entity);
        Map<String, Field> fields = ClassUtils.getFieldMap(entity.getClass());
        List<String> columns = new ArrayList<>();

        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }

            String fieldName = field.getName();
            Object value = meta.getValue(fieldName);

            // 主键：只有非 null 才插入（无论 selective）
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (value != null) {
                    columns.add(getColumnName(field));
                }
                continue;
            }

            // 普通字段：selective 时跳过 null
            if (!selective || value != null) {
                columns.add(getColumnName(field));
            }
        }
        columns = columns.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return "(" + String.join(", ", columns) + ")";
    }

    /**
     * 生成 INSERT 的 VALUES 占位符：`"#{userName}, #{createTime}"`
     */
    public static String buildInsertValues(Object params, boolean selective) {
        List<?> list = MybatisUtils.extractList(params);
        if (list == null || list.isEmpty()) {
            return buildSingleValue(params, selective, null);
        }
        String rowTemplate = buildSingleValue(list.get(0), selective, "item");
        return buildBatchValues(rowTemplate);
    }

    private static String buildSingleValue(Object entity, boolean selective, String prefix) {
        MetaObject meta = SystemMetaObject.forObject(entity);
        Map<String, Field> fields = ClassUtils.getFieldMap(entity.getClass());
        List<String> values = new ArrayList<>();

        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }

            String fieldName = field.getName();
            Object value = meta.getValue(fieldName);
            boolean isPk = field.isAnnotationPresent(PrimaryKey.class);

            if (isPk) {
                if (value != null) {
                    values.add(placeholder(fieldName, prefix));
                }
                continue;
            }

            if (!selective || value != null) {
                values.add(placeholder(fieldName, prefix));
            }
        }
        return "(" + String.join(", ", values) + ")";
    }

    private static String buildBatchValues(String rowTemplate) {
        return String.format("<foreach collection=\"list\" item=\"item\" separator=\",\">%s</foreach>", rowTemplate);
    }


    /**
     * 统一生成 SET 子句：
     * - 若 params 是单个实体 → 普通 SET（如 name = #{name}）
     * - 若 params 是 List 实体 → 批量 CASE WHEN SET
     */
    public static String buildSetClause(Object params, boolean selective, String prefix) {
        List<?> list = MybatisUtils.extractList(params);
        if (list == null || list.isEmpty()) {
            // 单条更新
            return buildSetClauseForSingle(params, selective, prefix);
        }
        return buildCaseWhenSetClause(list, selective);
    }

    /**
     * 为 UpdateSpec 生成 SET 子句
     */
    public static String buildSetClauseForUpdateSpec(UpdateSpec<?> updateSpec) {
        if (updateSpec == null) {
            throw new IllegalArgumentException("updateSpec cannot be null");
        }
        Map<String, Object> updates = updateSpec.getUpdates();
        if (updates.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }
        List<String> setItems = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                setItems.add(columnName + " = NULL");
            } else {
                // 注意：MyBatis 中 Map 的动态 key 必须用 ['key'] 语法
                setItems.add(columnName + " = " + placeholder(columnName, "updateSpec.updates"));

            }
        }

        return String.join(", ", setItems);
    }

    /**
     * 统一生成 WHERE 条件：
     * - 单个实体 → id = #{id} [AND del_flag = 0]
     * - List 实体 → id IN (<foreach...>) [AND del_flag = 0]
     */
    public static String buildWhereClause(Object params) {
        List<?> list = MybatisUtils.extractList(params);
        if (list == null || list.isEmpty()) {
            return buildWhereClauseForSingle(params);
        }
        return buildWhereClauseForBatchInternal(list);
    }


    private static String buildSetClauseForSingle(Object entity, boolean selective, String prefix) {
        MetaObject meta = SystemMetaObject.forObject(entity);
        Map<String, Field> fields = ClassUtils.getFieldMap(entity.getClass());
        List<String> sets = new ArrayList<>();

        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                continue;
            }

            String fieldName = field.getName();
            Object value = meta.getValue(fieldName);

            if (!selective || value != null) {
                String columnName = getColumnName(field);
                String placeholder = placeholder(fieldName, prefix);
                sets.add(columnName + " = " + placeholder);
            }
        }
        if (sets.isEmpty()) {
            throw new IllegalArgumentException("No updatable fields in " + entity.getClass().getSimpleName());
        }
        return String.join(", ", sets);
    }


    private static String buildWhereClauseForSingle(Object entity) {
        MetaObject meta = SystemMetaObject.forObject(entity);
        Map<String, Field> fields = ClassUtils.getFieldMap(entity.getClass());
        String primaryKeyCondition = null;
        String logicDeleteCondition = null;

        // 主键
        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }
            String fieldName = field.getName();
            String columnName = getColumnName(field);
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                Object value = meta.getValue(fieldName);
                if (value == null) {
                    throw new IllegalArgumentException("Primary key '" + fieldName + "' is null");
                }
                primaryKeyCondition = columnName + " = #{" + fieldName + "}";
                continue;
            }

            if (logicDeleteCondition == null && field.isAnnotationPresent(LogicDelete.class)) {
                logicDeleteCondition = buildLogicDeleteCondition(field, columnName);
            }
        }
        if (primaryKeyCondition == null) {
            throw new IllegalArgumentException("No primary key found for WHERE clause");
        }
        return logicDeleteCondition == null ? primaryKeyCondition : primaryKeyCondition + " AND " + logicDeleteCondition;
    }

    private static String buildCaseWhenSetClause(List<?> entities, boolean selective) {
        Object first = entities.get(0);
        Map<String, Field> fields = ClassUtils.getFieldMap(first.getClass());

        String pkFieldName = null, pkColumnName = null;
        List<Field> updatableFields = new ArrayList<>();

        // 一次遍历：找主键 + 收集可更新字段
        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }

            String fieldName = field.getName();
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (pkFieldName != null) {
                    throw new UnsupportedOperationException("Composite primary key is not supported.");
                }
                pkFieldName = fieldName;
                pkColumnName = getColumnName(field);
                continue;
            }

            // selective 模式：跳过第一个实体中为 null 的字段
            if (selective && SystemMetaObject.forObject(first).getValue(fieldName) == null) {
                continue;
            }
            updatableFields.add(field);
        }

        if (pkFieldName == null) {
            throw new IllegalArgumentException("No @PrimaryKey found");
        }
        if (updatableFields.isEmpty()) {
            throw new IllegalArgumentException("No updatable fields");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < updatableFields.size(); i++) {
            Field field = updatableFields.get(i);
            String col = getColumnName(field);
            if (col == null) {
                continue;
            }
            if (i > 0) {
                sb.append(",\n  ");
            }

            sb.append(col).append(" = CASE ").append(pkColumnName);

            for (int j = 0; j < entities.size(); j++) {
                sb.append("\n    WHEN #{list[").append(j).append("].").append(pkFieldName).append("} ").append("THEN #{list[").append(j).append("].").append(field.getName()).append("}");
            }
            sb.append("\n    ELSE ").append(col).append("\n  END");
        }

        return sb.toString();
    }

    private static String buildWhereClauseForBatchInternal(List<?> entities) {
        Object first = entities.get(0);
        Map<String, Field> fields = ClassUtils.getFieldMap(first.getClass());

        String pkFieldName = null, pkColumnName = null;
        String logicDelete = null;

        // 一次遍历：找主键 + 逻辑删除
        for (Field field : fields.values()) {
            if (isIgnoredField(field)) {
                continue;
            }

            String fieldName = field.getName();
            String columnName = getColumnName(field);

            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (pkFieldName != null) {
                    throw new UnsupportedOperationException("Composite primary key is not supported.");
                }
                pkFieldName = fieldName;
                pkColumnName = columnName;
            } else if (logicDelete == null && field.isAnnotationPresent(LogicDelete.class)) {
                logicDelete = buildLogicDeleteCondition(field, columnName);
            }
        }

        if (pkFieldName == null) {
            throw new IllegalArgumentException("No @PrimaryKey found");
        }

        StringBuilder where = new StringBuilder();
        where.append(pkColumnName).append(" IN (<foreach collection=\"list\" item=\"item\" separator=\",\">").append("#{item.").append(pkFieldName).append("}</foreach>)");

        if (logicDelete != null) {
            where.append(" AND ").append(logicDelete);
        }
        return where.toString();
    }

    public static String buildLogicColumns(MappedStatement ms) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        List<Field> fields = ClassUtils.getFieldsByAnnotation(entityClass, LogicDelete.class);
        if (fields.size() != 1) {
            throw new IllegalStateException("Entity [" + entityClass.getSimpleName() + "] must have exactly one @LogicDelete field");
        }
        Field field = fields.get(0);
        String columnName = getColumnName(field);
        LogicDelete anno = field.getAnnotation(LogicDelete.class);
        return columnName + " = " + anno.deleted();
    }

    public static String buildWhereId(MappedStatement ms, Object entity, boolean selective) {
        if (entity instanceof ConditionWrapper<?>) {
            ConditionWrapper<?> condition = (ConditionWrapper<?>) entity;
            return buildWhereClause(condition, null);
        }
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        List<Field> pkFields = ClassUtils.getFieldsByAnnotation(entityClass, PrimaryKey.class);
        Field pkField = pkFields.get(0);
        String columnName = getColumnName(pkField);
        List<?> extractList = MybatisUtils.extractList(entity);
        if (extractList == null && entity != null) {
            return columnName + " = #{param}";
        }
        if ((extractList == null || extractList.isEmpty())) {
            return selective ? "1=0" : "1=1";
        }
        return columnName + " IN (<foreach collection=\"list\" item=\"item\" separator=\",\">" + "#{item}</foreach>)";
    }

    // ------------------ 内部辅助方法 ------------------

    private static String buildLogicDeleteCondition(Field field, String columnName) {
        LogicDelete logicDelete = field.getAnnotation(LogicDelete.class);
        if (logicDelete != null) {
            return columnName + " = " + logicDelete.normal();
        }
        return null;
    }

    private static String placeholder(String fieldName, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "#{" + fieldName + "}";
        }
        return "#{" + prefix + "." + fieldName + "}";
    }

    public static boolean isIgnoredField(Field field) {
        Column col = field.getAnnotation(Column.class);
        return col != null && col.ignore();
    }

    /**
     * 获取字段对应的数据库列名
     *
     * @param field 字段
     * @return 列名；若标注 @Column(ignore=true) 则返回 null
     */
    public static String getColumnName(Field field) {
        Column col = field.getAnnotation(Column.class);
        if (col != null && !col.value().trim().isEmpty()) {
            return col.value();
        }
        return MybatisUtils.camelToUnderline(field.getName());
    }

    /**
     * 将 ConditionWrapper 转换为 WHERE SQL 片段（支持 AND 连接）
     */
    public static String buildWhereClause(ConditionWrapper<?> wrapper, String prefix) {
        List<String> sqlParts = new ArrayList<>();
        List<Condition> conditions = wrapper.getConditions();
        String paramPrefixTemplate;
        if (prefix == null || prefix.isEmpty()) {
            paramPrefixTemplate = "conditions[%d]";
        } else {
            paramPrefixTemplate = prefix + ".conditions[%d]";
        }
        for (int i = 0; i < conditions.size(); i++) {
            String actualParamPrefix = String.format(paramPrefixTemplate, i);
            String sql = buildConditionSql(conditions.get(i), actualParamPrefix);
            sqlParts.add(sql);
        }

        return sqlParts.isEmpty() ? "1=1" : String.join(" AND ", sqlParts);
    }


    // 递归构建单个 Condition 的 SQL
    private static String buildConditionSql(Condition cond, String paramPrefix) {
        if (cond instanceof Condition.Simple) {
            Condition.Simple s = (Condition.Simple) cond;
            if (s.value == null) {
                if ("=".equals(s.operator)) {
                    return s.column + " IS NULL";
                } else if ("<>".equals(s.operator) || "!=".equals(s.operator)) {
                    return s.column + " IS NOT NULL";
                }
            }
            return s.column + " " + s.operator + " #{" + paramPrefix + ".value}";

        } else if (cond instanceof Condition.Null) {
            Condition.Null n = (Condition.Null) cond;
            return n.column + (n.isNull ? " IS NULL" : " IS NOT NULL");

        } else if (cond instanceof Condition.In) {
            Condition.In in = (Condition.In) cond;
            if (!in.values.iterator().hasNext()) {
                return in.not ? "1=1" : "1=0";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(in.column).append(in.not ? " NOT IN (" : " IN (");

            int idx = 0;
            for (Object ignored : in.values) {
                if (idx > 0) {
                    sb.append(", ");
                }
                sb.append("#{").append(paramPrefix).append(".values[").append(idx).append("]}");
                idx++;
            }
            sb.append(")");
            return sb.toString();

        } else if (cond instanceof Condition.Group) {
            Condition.Group g = (Condition.Group) cond;
            if (g.children.isEmpty()) {
                return "1=1";
            }

            List<String> childSqls = new ArrayList<>();
            for (int j = 0; j < g.children.size(); j++) {
                String childParam = paramPrefix + ".children[" + j + "]";
                childSqls.add(buildConditionSql(g.children.get(j), childParam));
            }

            String logic = g.logic.toUpperCase();
            return "(" + String.join(" " + logic + " ", childSqls) + ")";

        } else {
            throw new UnsupportedOperationException("Unsupported condition: " + cond.getClass());
        }
    }
}
