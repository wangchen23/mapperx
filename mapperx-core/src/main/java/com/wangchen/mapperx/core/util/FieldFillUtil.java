package com.wangchen.mapperx.core.util;

import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.FillType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段自动填充工具类
 * 处理 fillType 和 fillMethod 逻辑
 *
 * @author chenwang
 */
public class FieldFillUtil {

    /**
     * 根据操作类型填充所有需要填充的字段
     *
     * @param target         目标对象
     * @param operationType  操作类型（INSERT/UPDATE）
     */
    public static void fillFields(Object target, FillType operationType) {
        List<Field> fields = getFillFields(target, operationType);
        for (Field field : fields) {
            Object value = invokeFillMethod(target, field, operationType);
            if (value != null) {
                fillFieldValue(target, field, value);
            }
        }
    }

    /**
     * 获取对象中所有需要填充的字段（指定操作类型）
     */
    private static List<Field> getFillFields(Object target, FillType operationType) {
        List<Field> result = new ArrayList<>();
        for (Field field : ClassUtils.getFieldsByAnnotation(target.getClass(), Column.class)) {
            if (shouldFill(target, field.getName(), operationType)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * 判断字段在指定操作下是否需要填充
     */
    private static boolean shouldFill(Object target, String fieldName, FillType operationType) {
        Field field = ClassUtils.getFieldMap(target.getClass()).get(fieldName);
        if (field == null || !field.isAnnotationPresent(Column.class)) {
            return false;
        }

        Column annotation = field.getAnnotation(Column.class);
        FillType fillType = annotation.fillType();
        return fillType == FillType.INSERT_UPDATE || fillType == operationType;
    }

    /**
     * 填充字段值到目标对象
     */
    private static void fillFieldValue(Object target, Field field, Object value) {
        ClassUtils.setFieldValue(target, field.getName(), value);
    }

    /**
     * 执行字段的填充方法
     */
    private static Object invokeFillMethod(Object target, Field field, FillType operationType) {
        Column annotation = field.getAnnotation(Column.class);
        String fillMethod = annotation.fillMethod();

        if (fillMethod.isEmpty()) {
            return null;
        }

        try {
            if (fillMethod.contains(".")) {
                // 全路径方法调用，格式：com.xxx.Service.methodName
                Method method = ClassUtils.getGlobalMethod(fillMethod, operationType.getClass());
                if (method != null) {
                    return method.invoke(null, operationType);
                }
            } else {
                // 在 target 上查找方法
                Method method = ClassUtils.getMethod(target.getClass(), fillMethod);
                if (method != null && method.getParameterCount() == 1) {
                    return method.invoke(target, operationType);
                }
            }
        } catch (Exception e) {
            // 填充方法执行失败，忽略
        }
        return null;
    }


}