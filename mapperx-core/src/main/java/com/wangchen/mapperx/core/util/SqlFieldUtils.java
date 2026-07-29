package com.wangchen.mapperx.core.util;


import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.LogicDelete;
import com.wangchen.mapperx.core.annotation.PrimaryKey;
import com.wangchen.mapperx.core.conditions.Condition;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
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
 **/
public class SqlFieldUtils {

    /**
     * 生成 INSERT 的字段列表：`"user_name, create_time"`
     */
    public static String buildInsertColumns(Object entity, boolean selective) {
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
    public static String buildInsertValues(Object entity, boolean selective) {
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
                    values.add(placeholder(fieldName, null));
                }
                continue;
            }

            if (!selective || value != null) {
                values.add(placeholder(fieldName, null));
            }
        }
        return "(" + String.join(", ", values) + ")";
    }


    /**
     * 统一生成 SET 子句：如 name = #{name}
     */
    public static String buildSetClause(Object entity, boolean selective, String prefix) {
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
     * 生成 WHERE 条件：id = #{id}
     */
    public static String buildWhereId(Object entity) {
        MetaObject meta = SystemMetaObject.forObject(entity);
        Map<String, Field> fields = ClassUtils.getFieldMap(entity.getClass());

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
                return columnName + " = #{" + fieldName + "}";
            }
        }
        return "";
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

        if (sqlParts.isEmpty()) {
            return "";
        }
        return " AND " + String.join(" AND ", sqlParts);
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
