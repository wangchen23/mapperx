package com.wangchen.mapperx.core.conditions;

import com.wangchen.mapperx.core.util.LambdaUtils;
import com.wangchen.mapperx.core.util.MybatisUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 条件构造器（支持 @Column 注解 + Lambda 表达式）
 *
 * @param <T> 实体类型
 * @author chenwang
 */
public class ConditionWrapper<T> {

    private final List<Condition> conditions = new ArrayList<>();
    private final List<OrderItem> orders = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();

    // ========================
    // 单值条件（自动跳过 null）
    // ========================

    public <R> ConditionWrapper<T> eq(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::eq);
    }

    public ConditionWrapper<T> eq(String column, Object value) {
        return eq(column, value, true);
    }

    public ConditionWrapper<T> eq(String column, Object value, boolean ignoreNull) {
        MybatisUtils.validateColumnName(column);
        if (!ignoreNull || value != null) {
            conditions.add(Condition.eq(column, value));
        }
        return this;
    }

    public <R> ConditionWrapper<T> ne(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::ne);
    }

    public <R> ConditionWrapper<T> gt(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::gt);
    }

    public <R> ConditionWrapper<T> ge(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::ge);
    }

    public <R> ConditionWrapper<T> lt(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::lt);
    }

    public <R> ConditionWrapper<T> le(SFunction<T, R> field, Object value) {
        return addIfNotNull(field, value, Condition::le);
    }

    // ========================
    // LIKE（跳过 null 或空字符串）
    // ========================

    public <R> ConditionWrapper<T> like(SFunction<T, R> field, String value) {
        return like(field, value, true);
    }

    public <R> ConditionWrapper<T> like(SFunction<T, R> field, String value, boolean ignoreEmpty) {
        if (!ignoreEmpty || (value != null && !value.trim().isEmpty())) {
            String col = LambdaUtils.getColumnName(field);
            conditions.add(Condition.like(col, value));
        }
        return this;
    }

    public ConditionWrapper<T> like(String column, String value) {
        return like(column, value, true);
    }

    public ConditionWrapper<T> like(String column, String value, boolean ignoreEmpty) {
        MybatisUtils.validateColumnName(column);
        if (!ignoreEmpty || (value != null && !value.trim().isEmpty())) {
            conditions.add(Condition.like(column, value));
        }
        return this;
    }

    // ========================
    // IS NULL / NOT NULL
    // ========================

    public <R> ConditionWrapper<T> isNull(SFunction<T, R> field) {
        String col = LambdaUtils.getColumnName(field);
        conditions.add(Condition.isNull(col));
        return this;
    }

    public ConditionWrapper<T> isNull(String column) {
        MybatisUtils.validateColumnName(column);
        conditions.add(Condition.isNull(column));
        return this;
    }

    public <R> ConditionWrapper<T> isNotNull(SFunction<T, R> field) {
        String col = LambdaUtils.getColumnName(field);
        conditions.add(Condition.isNotNull(col));
        return this;
    }

    public ConditionWrapper<T> isNotNull(String column) {
        MybatisUtils.validateColumnName(column);
        conditions.add(Condition.isNotNull(column));
        return this;
    }

    // ========================
    // IN / NOT IN
    // ========================

    public <R> ConditionWrapper<T> in(SFunction<T, R> field, Collection<?> values) {
        return in(field, values, true);
    }

    public <R> ConditionWrapper<T> in(SFunction<T, R> field, Collection<?> values, boolean ignoreEmpty) {
        if (!ignoreEmpty || (values != null && !values.isEmpty())) {
            String col = LambdaUtils.getColumnName(field);
            conditions.add(Condition.in(col, values));
        }
        return this;
    }

    public ConditionWrapper<T> notIn(String column, Collection<?> values) {
        return notIn(column, values, true);
    }

    public ConditionWrapper<T> notIn(String column, Collection<?> values, boolean ignoreEmpty) {
        MybatisUtils.validateColumnName(column);
        if (!ignoreEmpty || (values != null && !values.isEmpty())) {
            conditions.add(Condition.notIn(column, values));
        }
        return this;
    }

    // ========================
    // 逻辑分组
    // ========================

    public ConditionWrapper<T> and(Condition... subConditions) {
        Condition group = Condition.and(subConditions);
        if (group != null) {
            conditions.add(group);
        }
        return this;
    }

    public ConditionWrapper<T> or(Condition... subConditions) {
        Condition group = Condition.or(subConditions);
        if (group != null) {
            conditions.add(group);
        }
        return this;
    }

    // ========================
    // 排序（ORDER BY）
    // ========================

    public static class OrderItem {
        private final String column;
        private final boolean ascending;

        public OrderItem(String column, boolean ascending) {
            this.column = column;
            this.ascending = ascending;
        }

        public String getColumn() {
            return column;
        }

        public boolean isAscending() {
            return ascending;
        }
    }

    public <R> ConditionWrapper<T> orderByAsc(SFunction<T, R> field) {
        String col = LambdaUtils.getColumnName(field);
        orders.add(new OrderItem(col, true));
        return this;
    }

    public ConditionWrapper<T> orderByAsc(String column) {
        MybatisUtils.validateColumnName(column);
        orders.add(new OrderItem(column, true));
        return this;
    }

    public <R> ConditionWrapper<T> orderByDesc(SFunction<T, R> field) {
        String col = LambdaUtils.getColumnName(field);
        orders.add(new OrderItem(col, false));
        return this;
    }

    public ConditionWrapper<T> orderByDesc(String column) {
        MybatisUtils.validateColumnName(column);
        orders.add(new OrderItem(column, false));
        return this;
    }

    public <R> ConditionWrapper<T> orderBy(SFunction<T, R> field, boolean asc) {
        String col = LambdaUtils.getColumnName(field);
        orders.add(new OrderItem(col, asc));
        return this;
    }

    public ConditionWrapper<T> orderBy(String column, boolean asc) {
        MybatisUtils.validateColumnName(column);
        orders.add(new OrderItem(column, asc));
        return this;
    }

    // ========================
    // 排序（ORDER BY）
    // ========================

    public <R> ConditionWrapper<T> groupBy(SFunction<T, R> field) {
        String col = LambdaUtils.getColumnName(field);
        groups.add(col);
        return this;
    }

    public ConditionWrapper<T> groupBy(String column) {
        MybatisUtils.validateColumnName(column);
        groups.add(column);
        return this;
    }

    @SafeVarargs
    public final <R> ConditionWrapper<T> groupBy(SFunction<T, R>... fields) {
        for (SFunction<T, R> field : fields) {
            String col = LambdaUtils.getColumnName(field);
            groups.add(col);
        }
        return this;
    }

    public ConditionWrapper<T> groupBy(String... columns) {
        for (String column : columns) {
            MybatisUtils.validateColumnName(column);
        }
        Collections.addAll(groups, columns);
        return this;
    }

    // ========================
    // 通用辅助方法
    // ========================

    private <R> ConditionWrapper<T> addIfNotNull(SFunction<T, R> field, Object value, BiFunction<String, Object, Condition> builder) {
        if (value != null) {
            String col = LambdaUtils.getColumnName(field);
            conditions.add(builder.apply(col, value));
        }
        return this;
    }

    // ========================
    // Getter（返回不可变副本）
    // ========================

    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public List<OrderItem> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }
}