package com.wangchen.mapperx.autoconfigure;


import com.wangchen.mapperx.core.config.MapConfiguration;
import com.wangchen.mapperx.core.handler.MetaObjectHandler;
import com.wangchen.mapperx.core.interceptor.AutoFillInterceptor;
import com.wangchen.mapperx.core.interceptor.IdGeneratorInterceptor;
import com.wangchen.mapperx.core.spi.IdGenerator;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 自动注册插件
 *
 * @author chenwang
 * @date 2026/2/10 14:25
 **/
@Configuration
public class MyAutoConfiguration {

    @Bean
    public MapConfiguration myConfiguration() {
        return new MapConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorInterceptor idGeneratorInterceptor(@Autowired(required = false) Map<String, IdGenerator<?>> idGenerator) {
        if (idGenerator == null) {
            idGenerator = Collections.emptyMap();
        }
        return new IdGeneratorInterceptor(idGenerator);
    }

    @Bean
    @ConditionalOnBean(MetaObjectHandler.class)
    public AutoFillInterceptor autoFillInterceptor(MetaObjectHandler handler) {
        return new AutoFillInterceptor(handler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MapConfiguration configuration,
                                               @Autowired(required = false) List<Interceptor> interceptors) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        if (interceptors != null && !interceptors.isEmpty()) {
            factory.setPlugins(interceptors.toArray(new Interceptor[0]));
        }
        return factory.getObject();
    }

}
