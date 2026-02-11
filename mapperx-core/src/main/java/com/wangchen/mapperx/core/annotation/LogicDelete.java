package com.wangchen.mapperx.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记逻辑删除字段
 *
 * @author chenwang
 * @date 2025/6/18 22:57
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicDelete {

    String deleted() default "1";

    String normal() default "0";
}
