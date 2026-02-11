package com.wangchen.mapperx.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将当前方法的逻辑 “映射” 到另一个方法上
 * 当方法可能有自定义 SQL 时（如分页），建议显式设置 selfFirst = true；
 * 批量方法等强制复用场景可省略
 * 若方法 标记了  @Select("SELECT * FROM user WHERE is_vip = 1 AND name LIKE #{name}")  那么当 selfFirst = true 时就会执行指定的sql
 *
 * @author chenwang
 * @date 2025/6/18 17:16
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MapMethod {

    String value();
}
