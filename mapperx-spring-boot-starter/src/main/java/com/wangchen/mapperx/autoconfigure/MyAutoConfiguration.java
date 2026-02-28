package com.wangchen.mapperx.autoconfigure;

import com.wangchen.mapperx.core.config.MapConfiguration;
import com.wangchen.mapperx.core.handler.MetaObjectHandler;
import com.wangchen.mapperx.core.interceptor.AutoFillInterceptor;
import com.wangchen.mapperx.core.interceptor.BatchSplitInterceptor;
import com.wangchen.mapperx.core.interceptor.IdGeneratorInterceptor;
import com.wangchen.mapperx.core.spi.IdGenerator;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

/**
 * MyBatis 插件自动配置类
 * 核心特性：
 * 1. 兼容 MyBatis 自动创建的 SqlSessionFactory（优先使用）
 * 2. 兜底创建 SqlSessionFactory（仅当用户未配置/MyBatis 自动配置未触发时生效）
 * 3. 保证 MapConfiguration 及增强逻辑正常启动
 *
 * @author haochen78
 */
@org.springframework.context.annotation.Configuration
@ConditionalOnClass({SqlSessionFactory.class, DataSource.class})
public class MyAutoConfiguration {


    /**
     * ID生成器拦截器
     * 特性：无自定义 IdGenerator 时也能正常创建（空Map兜底）
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorInterceptor idGeneratorInterceptor(@Autowired(required = false) Map<String, IdGenerator<?>> idGeneratorMap) {
        Map<String, IdGenerator<?>> idGenerators = idGeneratorMap == null ? Collections.emptyMap() : idGeneratorMap;
        return new IdGeneratorInterceptor(idGenerators);
    }

    /**
     * 自动填充拦截器
     * 特性：仅当存在 MetaObjectHandler 时创建（避免无处理器时加载）
     */
    @Bean
    @ConditionalOnBean(MetaObjectHandler.class)
    @ConditionalOnMissingBean
    public AutoFillInterceptor autoFillInterceptor(MetaObjectHandler handler) {
        return new AutoFillInterceptor(handler);
    }

    /**
     * 批量拆分拦截器
     * 特性：无条件创建（核心功能）
     */
    @Bean
    @ConditionalOnMissingBean
    public BatchSplitInterceptor batchSplitInterceptor() {
        return new BatchSplitInterceptor();
    }


    /**
     * 注册 MapConfiguration（核心ORM配置）
     * 依赖：必须存在 SqlSessionFactory（无论是MyBatis自动创建还是兜底创建）
     */
    @Bean
    @ConditionalOnMissingBean
    public MapConfiguration mapConfiguration(SqlSessionFactory sqlSessionFactory) {
        Configuration mybatisConfig = sqlSessionFactory.getConfiguration();
        return new MapConfiguration(mybatisConfig);
    }

    /**
     * 执行 MapConfiguration 增强逻辑
     * 时机：Spring 容器启动完成后（所有Bean初始化、Mapper注册完成）
     */
    @Bean
    @ConditionalOnBean(MapConfiguration.class)
    public ApplicationRunner mapConfigurationEnhanceRunner(MapConfiguration mapConfiguration) {
        return args -> mapConfiguration.enhance();
    }
}