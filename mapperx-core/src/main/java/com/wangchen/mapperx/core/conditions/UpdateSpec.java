package com.wangchen.mapperx.core.conditions;

import com.wangchen.mapperx.core.util.LambdaUtils;
import com.wangchen.mapperx.core.util.MybatisUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 更新规范（支持 @Column 注解 + Lambda 表达式）
 *
 * @param <T> 实体类型
 * @author chenwang
 */
public class UpdateSpec<T> {

    private final Map<String, Object> updates = new LinkedHashMap<>();

    // ========================
    // 设置字段值（显式控制，null 会设为数据库 NULL）
    // ========================

    public <R> UpdateSpec<T> set(SFunction<T, R> field, R value) {
        String column = LambdaUtils.getColumnName(field);
        updates.put(column, value);
        return this;
    }

    public UpdateSpec<T> set(String column, Object value) {
        MybatisUtils.validateColumnName(column);
        updates.put(column, value);
        return this;
    }

    // ========================
    // 清空字段（语义更清晰，等价于 set(field, null)）
    // ========================

    public <R> UpdateSpec<T> clear(SFunction<T, R> field) {
        return set(field, null);
    }

    public UpdateSpec<T> clear(String column) {
        return set(column, null);
    }

    // ========================
    // 条件设置（跳过 null 值，类似 ConditionWrapper 的 ignoreNull）
    // ========================

    public <R> UpdateSpec<T> setIfNotNull(SFunction<T, R> field, R value) {
        if (value != null) {
            return set(field, value);
        }
        return this;
    }

    public UpdateSpec<T> setIfNotNull(String column, Object value) {
        if (value != null) {
            return set(column, value);
        }
        return this;
    }

    // ========================
    // 批量设置（适用于动态场景）
    // ========================

    public UpdateSpec<T> setAll(Map<String, Object> columnValues) {
        if (columnValues != null) {
            updates.putAll(columnValues);
        }
        return this;
    }

    // ========================
    // Getter（返回不可变视图）
    // ========================

    /**
     * 获取要更新的字段映射（列名 -> 值）
     * 注意：值可能为 null，表示要设为数据库 NULL
     */
    public Map<String, Object> getUpdates() {
        return Collections.unmodifiableMap(updates);
    }
}