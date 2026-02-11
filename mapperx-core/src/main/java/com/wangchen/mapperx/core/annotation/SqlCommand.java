package com.wangchen.mapperx.core.annotation;

import org.apache.ibatis.mapping.SqlCommandType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * sql 类型
 *
 * @author chenwang
 * @date 2026/2/6 10:11
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlCommand {
    SqlCommandType value();
}
