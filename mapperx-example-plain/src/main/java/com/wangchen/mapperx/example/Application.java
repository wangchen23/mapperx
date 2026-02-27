package com.wangchen.mapperx.example;

import com.github.pagehelper.PageInterceptor;
import com.wangchen.mapperx.core.config.MapConfiguration;
import com.wangchen.mapperx.core.interceptor.AutoFillInterceptor;
import com.wangchen.mapperx.core.interceptor.BatchSplitInterceptor;
import com.wangchen.mapperx.core.interceptor.IdGeneratorInterceptor;
import com.wangchen.mapperx.example.entity.UserInfoDO;
import com.wangchen.mapperx.example.generator.OrderNoGenerator;
import com.wangchen.mapperx.example.handler.BusinessLogicDrivenHandler;
import com.wangchen.mapperx.example.mapper.UserInfoMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Application（适配组合模式的 MapConfiguration）
 *
 * @author chenwang
 **/
public class Application {

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.log.com.wangchen.mapperx", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.ibatis", "DEBUG");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss");

        SqlSessionFactory sqlSessionFactory = createSqlSessionFactory();

        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            UserInfoMapper userMapper = session.getMapper(UserInfoMapper.class);

            List<UserInfoDO> list = new ArrayList<>();
            list.add(new UserInfoDO(null, "张三", null,1));
            list.add(new UserInfoDO(null, "李", 13,1));
            list.add(new UserInfoDO(null, "王五", null,1));

            UserInfoDO userInfoDO = new UserInfoDO();
            userInfoDO.setId(53L);
            userInfoDO.setAge(90);
            //list.add(userInfoDO1);
            //UserInfoDO userInfoDO2 = new UserInfoDO();
            //userInfoDO2.setId(null);
            //userInfoDO2.setAge(90);
            //list.add(userInfoDO2);
            //UserInfoDO userInfoDO3 = new UserInfoDO();
            //userInfoDO3.setId(null);
            //userInfoDO3.setAge(90);
            //list.add(userInfoDO3);
            

            int i = userMapper.insertSelective(userInfoDO);
            //int i = userMapper.batchInsertSelective(list);
            //int i = userMapper.batchUpdate(list);

            session.commit();
            //list.forEach(u -> System.out.println(u.getId()));
            System.out.println(userInfoDO.getId());
        }
    }

    public static SqlSessionFactory createSqlSessionFactory() {
        // 1. 数据源
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/sharding_db_0");
        config.setUsername("root");
        config.setPassword("root");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        DataSource dataSource = new HikariDataSource(config);

        // 2. MyBatis 环境
        Environment environment = new Environment("dev", new JdbcTransactionFactory(), dataSource);

        // 3. 创建原生 Configuration
        Configuration nativeConfig = new Configuration();
        nativeConfig.setEnvironment(environment);
        nativeConfig.setMapUnderscoreToCamelCase(true);

        // 4. 【关键】先注册 Mapper 到 MyBatis 原生 Configuration
        nativeConfig.addMapper(UserInfoMapper.class);
        // 如果有其他 Mapper，继续加：
        // nativeConfig.addMapper(OrderMapper.class);

        // 5. 【关键】创建 MapConfiguration 并增强
        MapConfiguration mapConfig = new MapConfiguration(nativeConfig);
        mapConfig.enhance(); // 👈 这一步会为 UserInfoMapper 注入 Base 方法

        // 6. 注册插件（必须在 enhance 之后？其实顺序无关，但建议在 enhance 前或后均可）
        nativeConfig.addInterceptor(new PageInterceptor());
        nativeConfig.addInterceptor(new AutoFillInterceptor(new BusinessLogicDrivenHandler()));

        BatchSplitInterceptor batchInterceptor = new BatchSplitInterceptor();
        //batchInterceptor.setProperties(new Properties() {
        //    {setProperty("splitThreshold", "2");}
        //    {setProperty("batchChunkSize", "1");}
        //});
        nativeConfig.addInterceptor(batchInterceptor);

        IdGeneratorInterceptor idGenInterceptor = new IdGeneratorInterceptor();
        idGenInterceptor.registerGenerator("order", new OrderNoGenerator());
        nativeConfig.addInterceptor(idGenInterceptor);

        // 7. 构建工厂
        return new SqlSessionFactoryBuilder().build(nativeConfig);
    }
}