package com.wangchen.mapperx.example;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Application
 *
 * @author chenwang
 * @date 2026/2/10 15:04
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
            list.add(new UserInfoDO(52L, "张三", null,1));
            list.add(new UserInfoDO(53L, "李", 13,1));
            list.add(new UserInfoDO(54L, "王五", null,1));

            UserInfoDO userInfoDO = new UserInfoDO();
            //userInfoDO.setUserName("nnn");
            userInfoDO.setAge(90);

            //userMapper.batchInsert(list);
            //userMapper.batchUpdate(list);
            //userMapper.batchUpdateSelective(list);
            ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
            wrapper.in(UserInfoDO::getId, Arrays.asList(58L, 509L, 60L));
            //int update =   userMapper.updateByConditionSelective(userInfoDO, wrapper);
            //int update = userMapper.updateByCondition(userInfoDO, wrapper);
            UpdateSpec<UserInfoDO> updateSpec = new UpdateSpec<>();
            updateSpec.set(UserInfoDO::getUserName, null);
            //int update = userMapper.updateByConditionWithFields(updateSpec, wrapper);
            //userMapper.deleteByCondition(wrapper);
            //userMapper.logicDeleteByCondition(wrapper);
            //userMapper.insert(userInfoDO);
            int i = userMapper.insertSelective(userInfoDO);
            //userMapper.updateSelective(userInfoDO);
            //int update = userMapper.updateSelective(userInfoDO);

            //UserInfoDO infoDO = userMapper.getById(60L);

            //boolean  b = userMapper.existsById(50L);
            //boolean b1 = userMapper.existsByCondition(wrapper);
            //UserInfoDO userInfoDO1 = userMapper.selectOne(wrapper);
            //List<UserInfoDO> list1 = userMapper.list(wrapper);
            //long count = userMapper.count(wrapper);
            //UserInfoDO userInfoDO1 = userMapper.lockById(60L);
            //QueryWrapper<UserInfoDO> wrapper1 = new QueryWrapper<>();
            //PageResult<UserInfoDO> result = userMapper.pageQuery(wrapper1);
            PageHelper.startPage(2, 1);
            //List<UserInfoDO> list1 = userMapper.list(null);
            //PageInfo<UserInfoDO> pageInfo = new PageInfo<>(list1);
            System.out.println("插入成功");

            // 分页（前提是 PageQueryInterceptor 已注册）
            // PageResult<UserDO> page = userMapper.page(param);
            session.commit();
        }
    }

    public static SqlSessionFactory createSqlSessionFactory() {
        // 1. 创建数据源（以 HikariCP 为例）
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/sharding_db_0");
        config.setUsername("root");
        config.setPassword("root");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        DataSource dataSource = new HikariDataSource(config);

        // 2. 创建 MyBatis Environment
        JdbcTransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("dev", transactionFactory, dataSource);

        // 3. 创建 Configuration
        Configuration configuration = new MapConfiguration();
        configuration.setEnvironment(environment);
        // 可选：开启驼峰映射
        configuration.setMapUnderscoreToCamelCase(true);

        // 4. 【关键】注册你的具体 Mapper 接口
        configuration.addMapper(UserInfoMapper.class);
        // 如果有多个，继续 addMapper(...)

        // 5. 【可选】注册自定义插件（如分页、批量等）
        // 分页插件
        configuration.addInterceptor(new PageInterceptor());
        // 自动填充插件
        configuration.addInterceptor(new AutoFillInterceptor(new BusinessLogicDrivenHandler()));
        // 自动分批次插件
        BatchSplitInterceptor interceptor = new BatchSplitInterceptor();
        Properties props = new Properties();
        props.setProperty("maxBatchSize", "2");
        interceptor.setProperties(props);
        configuration.addInterceptor(interceptor);
        // id 生成器
        IdGeneratorInterceptor idGeneratorInterceptor = new IdGeneratorInterceptor();
        idGeneratorInterceptor.registerGenerator("order", new OrderNoGenerator());
        configuration.addInterceptor(idGeneratorInterceptor);
        // 6. 构建 SqlSessionFactory
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
