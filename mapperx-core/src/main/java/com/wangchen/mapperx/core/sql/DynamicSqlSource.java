package com.wangchen.mapperx.core.sql;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 1、用于在运行时通过指定的 Provider 方法动态生成 SQL 语句
 * 2、往指定的 Provider 的方法上插入参数 MappedStatement
 * 3、解析 Provider 生成的带标签的 SQL
 *
 * @author chenwang
 * @date 2025/6/19 12:39
 */
public class DynamicSqlSource implements SqlSource {

    private final Object providerInstance;
    private final Method providerMethod;
    private MappedStatement mappedStatement;

    private static final XMLLanguageDriver XML_LANGUAGE_DRIVER = new XMLLanguageDriver();

    public DynamicSqlSource(Object providerInstance, Method method) {
        this.providerInstance = providerInstance;
        this.providerMethod = method;
    }

    public void setMappedStatement(MappedStatement ms) {
        this.mappedStatement = ms;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        String sql;
        try {
            sql = (String) providerMethod.invoke(providerInstance, parameterObject, mappedStatement);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        SqlSource sqlSource = XML_LANGUAGE_DRIVER.createSqlSource(mappedStatement.getConfiguration(),
                "<script>" + sql + "</script>", Object.class);
        return sqlSource.getBoundSql(parameterObject);
    }
}
