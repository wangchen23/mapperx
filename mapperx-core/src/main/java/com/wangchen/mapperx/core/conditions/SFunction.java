package com.wangchen.mapperx.core.conditions;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化的函数式接口，用于 Lambda 字段引用。
 * 必须继承 Serializable，否则无法通过反射提取字段名。
 *
 * @author chenwang
 **/
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
