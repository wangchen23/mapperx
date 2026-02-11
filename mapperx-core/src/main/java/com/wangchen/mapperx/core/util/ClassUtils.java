package com.wangchen.mapperx.core.util;


import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.FieldFill;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassUtils - 基于 MyBatis SystemMetaObject 重构版
 *
 * @author chenwang
 * @date 2026/1/15 20:55
 */
public class ClassUtils {

    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取类中带有指定注解的所有字段（包含父类）
     */
    public static List<Field> getFieldsByAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Map<String, Field> fieldMap = getFieldMap(clazz);
        List<Field> result = new ArrayList<>();
        for (Field field : fieldMap.values()) {
            if (field.isAnnotationPresent(annotationClass)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * 设置对象字段值（自动处理 private / getter/setter / Map）
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        SystemMetaObject.forObject(target).setValue(fieldName, value);
    }


    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, ClassUtils::buildFieldMap);
    }

    private static Map<String, Field> buildFieldMap(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                // 父类字段优先级低于子类（只保留第一次出现的）
                if (!map.containsKey(f.getName())) {
                    map.put(f.getName(), f);
                }
            }
            c = c.getSuperclass();
        }
        return map;
    }

    /**
     * 对外公开的工具方法：获取类中指定名称的 public 方法（带缓存）
     */
    public static Method getPublicMethod(Class<?> clazz, String methodName) {
        return METHOD_CACHE.computeIfAbsent(clazz, ClassUtils::buildMethodMap).get(methodName);
    }

    private static Map<String, Method> buildMethodMap(Class<?> clazz) {
        // 包含父接口和 Object 的 public 方法
        Method[] methods = clazz.getMethods();
        Map<String, Method> map = new ConcurrentHashMap<>(methods.length);
        for (Method method : methods) {
            // 注意：如果有重载，后出现的方法会覆盖前面的！
            map.put(method.getName(), method);
        }
        return map;
    }

    /**
     * 判断字段在指定填充时机下是否需要填充
     */
    public static boolean shouldFill(Object target, String fieldName, FieldFill operationType) {
        if (target == null || fieldName == null) {
            return false;
        }

        Field field = getFieldMap(target.getClass()).get(fieldName);
        if (field == null || !field.isAnnotationPresent(Column.class)) {
            return false;
        }

        Column annotation = field.getAnnotation(Column.class);
        FieldFill fillType = annotation.fill();

        // 匹配逻辑
        if (fillType == FieldFill.INSERT_UPDATE) {
            return true;
        }
        return fillType == operationType;
    }
}