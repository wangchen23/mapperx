package com.wangchen.mapperx.core.util;

import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.FilterType;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 字段过滤工具类
 * 处理 filterType 和 filterMethod 逻辑
 *
 * @author chenwang
 */
public class FieldFilterUtil {

    private FieldFilterUtil() {
    }

    /**
     * 判断过滤类型是否匹配操作类型
     */
    private static boolean matchesFilterType(FilterType filterType, FilterType operationType) {
        if (filterType == FilterType.SELECT_UPDATE_DELETE) {
            return true;
        }

        switch (filterType) {
            case SELECT:
                return operationType == FilterType.SELECT;
            case UPDATE:
                return operationType == FilterType.UPDATE;
            case DELETE:
                return operationType == FilterType.DELETE;
            case SELECT_UPDATE:
                return operationType == FilterType.SELECT || operationType == FilterType.UPDATE;
            case SELECT_DELETE:
                return operationType == FilterType.SELECT || operationType == FilterType.DELETE;
            case UPDATE_DELETE:
                return operationType == FilterType.UPDATE || operationType == FilterType.DELETE;
            default:
                return false;
        }
    }

    /**
     * 根据实体类的 filterType 配置生成 WHERE SQL（不包含 WHERE 关键字）
     *
     * @param ms         MappedStatement
     * @param filterType 过滤类型
     * @return WHERE 条件 SQL
     */
    public static String buildWhereSql(MappedStatement ms, FilterType filterType) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        StringBuilder sb = new StringBuilder();
        List<Field> fieldList = ClassUtils.getFieldsByAnnotation(entityClass, Column.class);

        for (Field field : fieldList) {
            Column annotation = field.getAnnotation(Column.class);
            FilterType fieldFilterType = annotation.filterType();
            if (!matchesFilterType(fieldFilterType, filterType)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(SqlFieldUtils.getColumnName(field)).append(" = #{").append(field.getName()).append("}");
        }
        return sb.toString();
    }
}