package com.wangchen.mapperx.core.util;

import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassUtils - 基于 MyBatis SystemMetaObject
 *
 * @author chenwang
 */
public class ClassUtils {

    // 字段缓存：Class -> (字段名 -> Field)
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    // 方法缓存：key 格式 "className#methodName" 或 "full.method.name"
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    // 字段按注解缓存：Class -> (AnnotationClass -> List<Field>)
    private static final Map<Class<?>, Map<Class<? extends Annotation>, java.util.List<Field>>> FIELD_BY_ANNOTATION_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取类中带有指定注解的所有字段（包含父类）
     */
    public static java.util.List<Field> getFieldsByAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Map<Class<? extends Annotation>, java.util.List<Field>> annotationMap = FIELD_BY_ANNOTATION_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        return annotationMap.computeIfAbsent(annotationClass, k -> {
            java.util.List<Field> result = new java.util.ArrayList<>();
            for (Field field : getFieldMap(clazz).values()) {
                if (field.isAnnotationPresent(k)) {
                    result.add(field);
                }
            }
            return result;
        });
    }

    /**
     * 获取字段 Map（包含父类所有字段）
     */
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
                // 子类字段覆盖父类字段
                map.putIfAbsent(f.getName(), f);
            }
            c = c.getSuperclass();
        }
        return map;
    }

    /**
     * 通过全路径获取静态方法（带缓存）
     *
     * @param fullMethodName 完整方法名，格式：com.xxx.Service.methodName
     * @param paramType      方法参数类型
     * @return 方法对象，未找到返回 null
     */
    public static Method getGlobalMethod(String fullMethodName, Class<?> paramType) {
        return METHOD_CACHE.computeIfAbsent(fullMethodName, k -> {
            int lastDotIndex = k.lastIndexOf('.');
            String className = k.substring(0, lastDotIndex);
            String methodName = k.substring(lastDotIndex + 1);
            try {
                Class<?> clazz = Class.forName(className);
                return clazz.getMethod(methodName, paramType);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * 获取类中指定参数类型的public方法（带缓存）
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param paramTypes 参数类型数组，空数组代表无参
     * @return Method，找不到返回null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        StringBuilder keySb = new StringBuilder(128);
        keySb.append(clazz.getName()).append("#").append(methodName);
        for (Class<?> p : paramTypes) {
            keySb.append("|").append(p.getName());
        }
        String key = keySb.toString();

        return METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * 设置对象字段值（自动处理 private / getter/setter / Map）
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        SystemMetaObject.forObject(target).setValue(fieldName, value);
    }

    /**
     * 获取对象字段值（自动处理 private / getter / Map）
     */
    public static Object getFieldValue(Object target, String fieldName) {
        return SystemMetaObject.forObject(target).getValue(fieldName);
    }
}