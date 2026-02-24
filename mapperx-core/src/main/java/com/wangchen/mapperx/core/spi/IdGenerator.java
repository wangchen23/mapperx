package com.wangchen.mapperx.core.spi;

import java.io.Serializable;

/**
 * <p>用户可通过实现此接口，并注册为 Spring Bean 或手动注入，
 * * 以支持自定义主键生成逻辑（如雪花 ID、业务编码等）。</p>
 *
 * @author chenwang
 **/
@FunctionalInterface
public interface IdGenerator<T extends Serializable> {
    T generate(Class<?> entityClass);
}
