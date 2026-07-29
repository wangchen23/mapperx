package com.wangchen.mapperx.core.util;

import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.FillType;
import com.wangchen.mapperx.core.conditions.UpdateSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段自动填充工具类
 * 处理 fillType 和 fillMethod 逻辑
 *
 * @author chenwang
 */
public class FieldFillUtil {
    /**
     * 构造器缓存，字段、方法元数据缓存统一复用ClassUtils
     */
    private static final ConcurrentHashMap<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 单实体 insert / update 填充
     *
     * @param target        实体实例
     * @param operationType 操作类型
     */
    public static void fillFields(Object target, FillType operationType) {
        List<Field> fields = getFillFields(target, operationType);
        for (Field field : fields) {
            Object value = invokeFillMethod(target, field, operationType);
            if (value != null) {
                ClassUtils.setFieldValue(target, field.getName(), value);
            }
        }
    }

    /**
     * 批量条件更新 updateByCondition 填充
     *
     * @param updateSpec    更新集合
     * @param entityClass   实体Class
     * @param operationType 操作类型
     */
    public static void fillUpdateSpec(UpdateSpec<?> updateSpec, Class<?> entityClass, FillType operationType) {
        Map<String, Object> updateMap = updateSpec.getUpdates();
        List<Field> fieldList = ClassUtils.getFieldsByAnnotation(entityClass, Column.class);
        // 仅创建一次临时实例
        Object tempInstance = createTempInstance(entityClass);

        for (Field field : fieldList) {
            Column columnAnn = field.getAnnotation(Column.class);
            FillType fillRule = columnAnn.fillType();

            // 判断是否执行填充
            if (fillRule != FillType.INSERT_UPDATE && fillRule != operationType) {
                continue;
            }

            String propertyName = field.getName();
            // 重点：如果updateSpec已经存在该属性，跳过填充
            if (updateMap.containsKey(propertyName)) {
                continue;
            }

            Object fillValue;
            if (tempInstance != null) {
                fillValue = invokeFillMethod(tempInstance, field, operationType);
            } else {
                // 无无参构造器，仅尝试全局静态方法
                fillValue = tryInvokeStaticMethod(field, operationType);
            }

            if (fillValue != null) {
                // 注意！如果updateSpec的key是数据库列名，替换为 SqlFieldUtils.getColumnName(field)
                updateSpec.set(propertyName, fillValue);
            }
        }
    }

    private static List<Field> getFillFields(Object target, FillType operationType) {
        Class<?> clazz = target.getClass();
        List<Field> allColumnFields = ClassUtils.getFieldsByAnnotation(clazz, Column.class);
        List<Field> result = new ArrayList<>();
        for (Field field : allColumnFields) {
            if (shouldFill(target, field.getName(), operationType)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * 创建实体空实例（批量更新使用）
     */
    private static Object createTempInstance(Class<?> entityClass) {
        try {
            Constructor<?> constructor = CONSTRUCTOR_CACHE.computeIfAbsent(entityClass, k -> {
                try {
                    Constructor<?> c = k.getDeclaredConstructor();
                    c.setAccessible(true);
                    return c;
                } catch (Exception e) {
                    return null;
                }
            });
            return constructor == null ? null : constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断该字段是否需要填充
     */
    private static boolean shouldFill(Object target, String fieldName, FillType operationType) {
        Field field = ClassUtils.getFieldMap(target.getClass()).get(fieldName);
        if (field == null || !field.isAnnotationPresent(Column.class)) {
            return false;
        }
        // 已有值不再填充
        Object currentVal = ClassUtils.getFieldValue(target, fieldName);
        if (currentVal != null) {
            return false;
        }
        Column ann = field.getAnnotation(Column.class);
        FillType fillRule = ann.fillType();
        return fillRule == FillType.INSERT_UPDATE || fillRule == operationType;
    }

    /**
     * 执行填充方法：支持【实体内部实例方法】、【全局静态方法】
     *
     * @param instance      实体实例（不能为null）
     * @param field         目标字段
     * @param operationType 操作类型
     * @return 填充值
     */
    private static Object invokeFillMethod(Object instance, Field field, FillType operationType) {
        Column ann = field.getAnnotation(Column.class);
        String fillMethodName = ann.fillMethod();
        if (fillMethodName.isEmpty()) {
            return null;
        }

        try {
            // 包含包路径 → 全局静态方法
            if (fillMethodName.contains(".")) {
                Method method = ClassUtils.getGlobalMethod(fillMethodName, FillType.class);
                return method == null ? null : method.invoke(null, operationType);
            }
            // 实体内部实例方法
            Method method = ClassUtils.getMethod(instance.getClass(), fillMethodName, FillType.class);
            return method == null ? null : method.invoke(instance, operationType);
        } catch (Exception e) {
            // log.warn("自动填充执行失败,field={},method={}", field.getName(), fillMethodName, e);
            return null;
        }
    }

    /**
     * 兜底：仅尝试全局静态方法（无实体实例时调用）
     */
    private static Object tryInvokeStaticMethod(Field field, FillType operationType) {
        Column ann = field.getAnnotation(Column.class);
        String fillMethodName = ann.fillMethod();
        if (!fillMethodName.contains(".")) {
            return null;
        }
        try {
            Method method = ClassUtils.getGlobalMethod(fillMethodName, FillType.class);
            return method == null ? null : method.invoke(null, operationType);
        } catch (Exception e) {
            return null;
        }
    }
}