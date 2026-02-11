package com.wangchen.mapperx.example.config;

import com.github.pagehelper.PageInterceptor;
import com.wangchen.mapperx.core.interceptor.BatchSplitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MybatisConfig
 *
 * @author chenwang
 * @date 2026/2/11 14:00
 **/
@Configuration
public class MybatisConfig {


    @Bean
    public BatchSplitInterceptor batchSplitInterceptor() {
        BatchSplitInterceptor interceptor = new BatchSplitInterceptor();
        Properties props = new Properties();
        props.setProperty("maxBatchSize", "2");
        interceptor.setProperties(props);
        return interceptor;
    }

    @Bean
    public PageInterceptor pageInterceptor() {
        return new PageInterceptor();
    }
}
