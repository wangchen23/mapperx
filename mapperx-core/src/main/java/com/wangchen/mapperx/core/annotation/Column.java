package com.wangchen.mapperx.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段映射
 *
 * @author chenwang
 * @date 2025/6/18 15:32
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    // 是否忽略标注的字段
    boolean ignore() default false;

    // 字段映射
    String value() default "";

    // 自动填充时机
    FieldFill fill() default FieldFill.NEVER;
}
