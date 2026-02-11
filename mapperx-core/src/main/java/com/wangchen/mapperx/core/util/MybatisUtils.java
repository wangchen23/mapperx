package com.wangchen.mapperx.core.util;


import com.wangchen.mapperx.core.annotation.LogicDelete;
import com.wangchen.mapperx.core.annotation.Table;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MybatisUtils
 *
 * @author chenwang
 * @date 2026/1/15 16:44
 **/
public class MybatisUtils {

    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> COLUMN_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> DELETE_SQL_CACHE = new ConcurrentHashMap<>();


    /**
     * 获取表名：优先从 @Table 注解，其次 ResultMap，最后 SQL 解析（降级）
     */
    public static String getTableNameByMs(MappedStatement ms) {
        Class<?> entityClass = getEntityClassByMs(ms);
        // 1. 优先：@Table 注解
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.value().isEmpty()) {
            return table.value();
        }
        // 2. 降级：类名转下划线
        String cleanName = removeSuffix(entityClass.getSimpleName());
        return camelToUnderline(cleanName);
    }

    /**
     * 根据 ms 获取实体类
     */
    public static Class<?> getEntityClassByMs(MappedStatement ms) {
        //eg.   com.example.UserMapper.insert
        String mappedStatementId = ms.getId();
        int lastDot = mappedStatementId.lastIndexOf(".");
        // 获取 Mapper接口类名
        String mapperClassName = mappedStatementId.substring(0, lastDot);
        return resolveEntityClass(mapperClassName);
    }

    /**
     * 获取要查询的字段名 （数据库表真实字段名）
     */
    public static List<String> getColumnsByMs(MappedStatement ms) {
        String msId = ms.getId();
        if (COLUMN_NAME_CACHE.containsKey(msId)) {
            return COLUMN_NAME_CACHE.get(msId);
        }
        // 获取实体类
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        Map<String, Field> fieldMap = ClassUtils.getFieldMap(entityClass);
        List<String> rawColumns = new ArrayList<>();
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            Field field = entry.getValue();
            if (SqlFieldUtils.isIgnoredField(field)) {
                continue;
            }
            rawColumns.add(SqlFieldUtils.getColumnName(field));
        }
        COLUMN_NAME_CACHE.put(msId, rawColumns);
        return rawColumns;
    }

    /**
     * 获取逻辑删除条件
     */
    public static String getDeleteFilterSql(MappedStatement ms) {
        String msId = ms.getId();
        if (DELETE_SQL_CACHE.containsKey(msId)) {
            return DELETE_SQL_CACHE.get(msId);
        }
        // 获取实体类
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        Map<String, Field> fieldMap = ClassUtils.getFieldMap(entityClass);
        String sql = "";
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            Field field = entry.getValue();
            if (field.isAnnotationPresent(LogicDelete.class)) {
                LogicDelete annotation = field.getAnnotation(LogicDelete.class);
                String columnName = SqlFieldUtils.getColumnName(field);
                sql = " AND " + columnName + " = " + annotation.normal();
                DELETE_SQL_CACHE.put(msId, sql);
                break;
            }
        }
        return sql;
    }


    /**
     * 获取实体类
     */
    private static Class<?> resolveEntityClass(String mapperClassName) {
        Class<?> mapperClass = getClass(mapperClassName);
        Type[] interfaces = mapperClass.getGenericInterfaces();
        for (Type type : interfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Type rawType = pt.getRawType();
                if (rawType instanceof Class<?>) {
                    Type[] args = pt.getActualTypeArguments();
                    return (Class<?>) args[0];
                }
            }
        }
        return null;
    }

    /**
     * 根据类名或 MappedStatement 解析目标类
     *
     * @param className 类全限定名（可为空）
     * @return 目标实体类 Class 对象
     */
    public static Class<?> getClass(String className) {
        return CLASS_CACHE.computeIfAbsent(className, classNameParam -> {
            try {
                // 类加载逻辑（可根据你的实际需求调整）
                return Class.forName(classNameParam);
            } catch (ClassNotFoundException e) {
                // 转换为运行时异常（或按需处理）
                throw new RuntimeException("加载类失败: " + classNameParam, e);
            }
        });
    }

    /**
     * 驼峰转下划线 （如 "userName"）转换为小驼峰（如 "user_name"）
     */
    public static String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder(camelCase.length() + 4);
        sb.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将下划线命名（如 "user_name"）转换为小驼峰（如 "userName"）
     */
    public static String underlineToCamel(String underline) {
        if (underline == null || underline.isEmpty()) {
            return underline;
        }

        StringBuilder sb = new StringBuilder(underline.length());
        boolean nextUpperCase = false;

        for (int i = 0; i < underline.length(); i++) {
            char c = underline.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    sb.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String removeSuffix(String name) {
        for (String suffix : new String[]{"DO", "Dto", "DTO", "Entity", "Model", "PO", "Vo", "VO"}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    /**
     * 提取参数中的 List
     */
    @SuppressWarnings("unchecked")
    public static List<?> extractList(Object parameter) {
        if (parameter instanceof List) {
            return (List<?>) parameter;
        }
        if (parameter instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) parameter;
            // MyBatis 默认会把 collection/list 包装进 map
            Object list = map.get("list");
            if (list instanceof List) {
                return (List<?>) list;
            }
            list = map.get("collection");
            if (list instanceof List) {
                return (List<?>) list;
            }
        }
        return null;
    }

    /**
     * 校验列名是否合法，不合法则抛出 IllegalArgumentException
     */
    public static void validateColumnName(String columnName) {
        if (columnName == null || !columnName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid SQL column name: '" + columnName + "'");
        }
    }

}
