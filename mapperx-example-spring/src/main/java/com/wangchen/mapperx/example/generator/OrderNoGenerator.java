package com.wangchen.mapperx.example.generator;

import com.wangchen.mapperx.core.spi.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * OrderNoGenerator
 *
 * @author chenwang
 * @date 2026/2/9 14:34
 **/
@Component
public class OrderNoGenerator implements IdGenerator<Long> {

    @Override
    public Long generate(Class<?> entityClass) {
        //return "ORD-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000);
        return (long) ThreadLocalRandom.current().nextInt(1000);
    }
}
