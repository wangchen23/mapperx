package com.wangchen.mapperx.core.provider;

import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
import com.wangchen.mapperx.core.util.MybatisUtils;
import com.wangchen.mapperx.core.util.SqlFieldUtils;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.Map;

/**
 * 动态 SQL 提供者
 * 核心功能：根据实体类注解自动生成单条/批量插入的纯文本 SQL，支持主键过滤、字段名映射
 *
 * @author chenwang
 * @date 2025/6/18 17:12
 **/
public class DynamicSQLProvider {

    public String getById(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(String.join(",", MybatisUtils.getColumnsByMs(ms)));
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, false));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    public String existsById(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("COUNT(1)");
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, true));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    public String selectOne(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(String.join(",", MybatisUtils.getColumnsByMs(ms)));
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, true));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        sql.append(" LIMIT 1");
        return sql.toString();
    }

    public String lockById(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(String.join(",", MybatisUtils.getColumnsByMs(ms)));
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, true));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        sql.append(" FOR UPDATE");
        return sql.toString();
    }

    public String insert(Object entity, MappedStatement ms) {
        return insert(entity, false, ms);
    }

    public String insertSelective(Object entity, MappedStatement ms) {
        return insert(entity, true, ms);
    }

    public String update(Object entity, MappedStatement ms) {
        return update(entity, false, ms);
    }

    public String updateSelective(Object entity, MappedStatement ms) {
        return update(entity, true, ms);
    }

    public String updateByCondition(Object entity, MappedStatement ms) {
        return updateByCondition(entity, false, ms);
    }

    public String updateByConditionSelective(Object entity, MappedStatement ms) {
        return updateByCondition(entity, true, ms);
    }

    public String updateByConditionWithFields(Object entity, MappedStatement ms) {
        @SuppressWarnings("unchecked") Map<String, Object> paramMap = (Map<String, Object>) entity;
        UpdateSpec<?> updateSpec = (UpdateSpec<?>) paramMap.get("updateSpec");
        ConditionWrapper<?> condition = (ConditionWrapper<?>) paramMap.get("condition");
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClauseForUpdateSpec(updateSpec));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereClause(condition, "condition"));
        return sql.toString();
    }

    public String logicDelete(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildLogicColumns(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, true));
        return sql.toString();
    }

    public String delete(Object entity, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(ms, entity, true));
        return sql.toString();
    }


    private String insert(Object entity, boolean selective, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(SqlFieldUtils.buildInsertColumns(entity, selective));
        sql.append("VALUES ");
        sql.append(SqlFieldUtils.buildInsertValues(entity, selective));
        return sql.toString();
    }

    private String update(Object entity, boolean selective, MappedStatement ms) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClause(entity, selective, null));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereClause(entity));
        return sql.toString();
    }

    private String updateByCondition(Object entity, boolean selective, MappedStatement ms) {
        @SuppressWarnings("unchecked") Map<String, Object> paramMap = (Map<String, Object>) entity;
        Object targetEntity = paramMap.get("entity");
        Object conditionObj = paramMap.get("condition");
        ConditionWrapper<?> condition = (ConditionWrapper<?>) conditionObj;
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClause(targetEntity, selective, "entity"));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereClause(condition, "condition"));
        return sql.toString();
    }

}
