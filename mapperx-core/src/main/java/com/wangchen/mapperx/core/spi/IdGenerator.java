package com.wangchen.mapperx.core.spi;

import java.io.Serializable;

/**
 * <p>用户可通过实现此接口，并注册为 Spring Bean 或手动注入，
 * * 以支持自定义主键生成逻辑（如雪花 ID、业务编码等）。</p>
 *
 * @author chenwang
 * @date 2026/2/9 14:32
 **/
@FunctionalInterface
public interface IdGenerator<T extends Serializable> {
    T generate(Class<?> entityClass);
}
