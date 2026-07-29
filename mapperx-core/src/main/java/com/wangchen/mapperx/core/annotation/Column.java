package com.wangchen.mapperx.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段映射
 *
 * @author chenwang
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    // 是否忽略标注的字段
    boolean ignore() default false;

    // 字段映射
    String value() default "";

    // 填充类型
    FillType fillType() default FillType.NEVER;

    // 填充取值方法：参数为操作类型（INSERT/UPDATE）
    // 支持全路径格式：com.xxx.UserService.fillCreateTime（方法需为静态方法，参数为 FillType）
    // 或直接写方法名（在实体类上查找实例方法）
    String fillMethod() default "";

    // 过滤类型
    FilterType filterType() default FilterType.NEVER;

    // 过滤取值方法：参数为操作类型（SELECT/UPDATE/DELETE）
    // 支持全路径格式：com.xxx.UserService.filterStatus（方法需为静态方法，参数为 FilterType）
    // 或直接写方法名（在实体类上查找实例方法）
    String filterMethod() default "";
}