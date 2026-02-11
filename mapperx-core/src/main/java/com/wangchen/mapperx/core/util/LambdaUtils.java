package com.wangchen.mapperx.core.util;

import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.conditions.SFunction;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda 工具类（高性能 + 支持 @Column 注解 + 复用 ClassUtils）
 *
 * @author chenwang
 * @date 2026/1/29 15:55
 */
public class LambdaUtils {

    // 缓存：避免重复反射解析同一个 Lambda 类
    private static final Map<Class<?>, LambdaMeta> LAMBDA_CACHE = new ConcurrentHashMap<>(128);

    /**
     * 从 SFunction 提取 Java 字段名（如 User::getName → "name"）
     */
    public static <T, R> String getFieldName(SFunction<T, R> fn) {
        return getLambdaMeta(fn).fieldName;
    }

    /**
     * 从 SFunction 提取数据库列名（支持 @Column 注解，复用 ClassUtils）
     * 列名策略：@Column.value() > 驼峰转下划线（与 ClassUtils 一致）
     */
    public static <T, R> String getColumnName(SFunction<T, R> fn) {
        LambdaMeta meta = getLambdaMeta(fn);
        Class<?> clazz = meta.declaringClass;
        String fieldName = meta.fieldName;

        // ✅ 复用 ClassUtils 获取字段（带缓存 + 继承 + 过滤）
        Field field = ClassUtils.getFieldMap(clazz).get(fieldName);
        if (field == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found in class: " + clazz.getName());
        }

        Column columnAnno = field.getAnnotation(Column.class);

        // 安全：禁止使用 @Column(ignore = true) 的字段
        if (columnAnno != null && columnAnno.ignore()) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' in class '" + clazz.getSimpleName() +
                            "' is marked with @Column(ignore = true). Cannot be used in query conditions."
            );
        }

        // 优先使用 @Column("xxx") 显式映射
        if (columnAnno != null && !columnAnno.value().trim().isEmpty()) {
            return columnAnno.value();
        }

        // 默认：驼峰转下划线（与 ClassUtils.extractColumnNames 行为一致）
        return MybatisUtils.camelToUnderline(fieldName);
    }

    // ========================
    // 内部 Lambda 解析
    // ========================

    private static <T, R> LambdaMeta getLambdaMeta(SFunction<T, R> fn) {
        return LAMBDA_CACHE.computeIfAbsent(fn.getClass(), k -> parseLambda(fn));
    }

    private static <T, R> LambdaMeta parseLambda(SFunction<T, R> fn) {
        try {
            Method writeReplace = fn.getClass().getDeclaredMethod("writeReplace");
            boolean wasAccessible = writeReplace.isAccessible();
            writeReplace.setAccessible(true);
            SerializedLambda lambda;
            try {
                lambda = (SerializedLambda) writeReplace.invoke(fn);
            } finally {
                writeReplace.setAccessible(wasAccessible);
            }

            String implClassName = lambda.getImplClass().replace("/", ".");
            Class<?> declaringClass = Class.forName(implClassName);
            String getterName = lambda.getImplMethodName();
            String fieldName = extractFieldName(getterName);
            String setterName = "set" + capitalize(fieldName);

            return new LambdaMeta(declaringClass, fieldName, setterName);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse lambda expression. Use method reference like 'Entity::getField'.",
                    e
            );
        }
    }

    // ========================
    // 工具方法
    // ========================

    private static String extractFieldName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if ((methodName.startsWith("is") || methodName.startsWith("has")) && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        return methodName;
    }

    private static String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() == 1) {
            return str.toLowerCase();
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ========================
    // 内部缓存结构
    // ========================

    private static class LambdaMeta {
        final Class<?> declaringClass;
        final String fieldName;
        final String setterName;

        LambdaMeta(Class<?> declaringClass, String fieldName, String setterName) {
            this.declaringClass = declaringClass;
            this.fieldName = fieldName;
            this.setterName = setterName;
        }
    }
}