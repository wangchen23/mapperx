package com.wangchen.mapperx.core.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Condition
 *
 * @author chenwang
 * @date 2026/1/30 11:29
 **/
public class Condition {

    // —————— 简单条件 ——————
    public static Condition eq(String column, Object value) {
        return new Simple("=", column, value);
    }

    public static Condition ne(String column, Object value) {
        return new Simple("<>", column, value);
    }

    public static Condition gt(String column, Object value) {
        return new Simple(">", column, value);
    }

    public static Condition ge(String column, Object value) {
        return new Simple(">=", column, value);
    }

    public static Condition lt(String column, Object value) {
        return new Simple("<", column, value);
    }

    public static Condition le(String column, Object value) {
        return new Simple("<=", column, value);
    }

    public static Condition like(String column, String pattern) {
        return new Simple("LIKE", column, "%" + pattern + "%");
    }

    public static Condition isNull(String column) {
        return new Null(column, true);
    }

    public static Condition isNotNull(String column) {
        return new Null(column, false);
    }

    public static Condition in(String column, Iterable<?> values) {
        return new In(column, values, false);
    }

    public static Condition notIn(String column, Iterable<?> values) {
        return new In(column, values, true);
    }

    // —————— 逻辑分组 ——————
    public static Condition and(Condition... conditions) {
        return group(Arrays.asList(conditions), "AND");
    }

    public static Condition or(Condition... conditions) {
        return group(Arrays.asList(conditions), "OR");
    }

    private static Condition group(List<Condition> list, String logic) {
        List<Condition> filtered = new ArrayList<>();
        for (Condition c : list) {
            if (c != null) {
                filtered.add(c);
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        return new Group(logic, Collections.unmodifiableList(filtered));
    }

    // —————— 内部实现类 ——————
    public static class Simple extends Condition {
        public final String operator, column;
        public final Object value;

        Simple(String op, String col, Object val) {
            this.operator = op;
            this.column = col;
            this.value = val;
        }
    }

    public static class Null extends Condition {
        public final String column;
        public final boolean isNull;

        Null(String col, boolean isNull) {
            this.column = col;
            this.isNull = isNull;
        }
    }

    public static class In extends Condition {
        public final String column;
        public final Iterable<?> values;
        public final boolean not;

        In(String col, Iterable<?> vals, boolean not) {
            this.column = col;
            this.values = vals;
            this.not = not;
        }
    }

    public static class Group extends Condition {
        public final String logic; // "AND" 或 "OR"
        public final List<Condition> children;

        Group(String logic, List<Condition> children) {
            this.logic = logic;
            this.children = children;
        }
    }
}
