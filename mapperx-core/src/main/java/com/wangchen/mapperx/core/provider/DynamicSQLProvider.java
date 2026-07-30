package com.wangchen.mapperx.core.provider;

import com.wangchen.mapperx.core.annotation.FillType;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
import com.wangchen.mapperx.core.util.FieldFillUtil;
import com.wangchen.mapperx.core.util.MybatisUtils;
import com.wangchen.mapperx.core.util.SqlFieldUtils;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.Map;

/**
 * 动态 SQL 提供者
 * 核心功能：根据实体类注解自动生成单条/批量插入的纯文本 SQL，支持主键过滤、字段名映射
 *
 * @author chenwang
 **/
public class DynamicSQLProvider {

    public String getById(Object entity, MappedStatement ms) {
        return buildSelectByIdSql(entity, ms, false, false);
    }

    public String existsById(Object entity, MappedStatement ms) {
        return buildSelectByIdSql(entity, ms, true, false);
    }

    public String selectOne(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildSelectSql(condition, ms, true);
    }

    public String lockById(Object entity, MappedStatement ms) {
        return buildSelectByIdSql(entity, ms, false, true);
    }

    public String list(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildSelectSql(condition, ms, false);
    }

    public String existsByCondition(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildCountSql(condition, ms, true);
    }

    public String count(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildCountSql(condition, ms, false);
    }

    public String logicDeleteByCondition(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildUpdateByConditionSql(condition, ms, true);
    }

    public String deleteByCondition(Object conditionObj, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        ConditionWrapper<Object> condition = (ConditionWrapper<Object>) conditionObj;
        return buildUpdateByConditionSql(condition, ms, false);
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
        @SuppressWarnings("unchecked")
        Map<String, Object> paramMap = (Map<String, Object>) entity;
        UpdateSpec<?> updateSpec = (UpdateSpec<?>) paramMap.get("updateSpec");
        ConditionWrapper<?> condition = (ConditionWrapper<?>) paramMap.get("condition");

        // 自动填充：获取实体类，处理需要自动填充的字段
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        FieldFillUtil.fillUpdateSpec(updateSpec, entityClass, FillType.UPDATE);

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClauseForUpdateSpec(updateSpec));
        sql.append(" WHERE 1=1");
        // todo 默认过滤 无
        sql.append(SqlFieldUtils.buildWhereClause(condition, "condition"));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    public String logicDelete(Object entity, MappedStatement ms) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(MybatisUtils.getLogicColumn(ms));
        sql.append(", ");
        sql.append(FieldFillUtil.buildFillSql(entityClass, FillType.UPDATE));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(entityClass, entity));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    public String delete(Object entity, MappedStatement ms) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(entityClass, entity));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }


    private String insert(Object entity, boolean selective, MappedStatement ms) {
        FieldFillUtil.fillFields(entity, FillType.INSERT);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(SqlFieldUtils.buildInsertColumns(entity, selective));
        sql.append("VALUES ");
        sql.append(SqlFieldUtils.buildInsertValues(entity, selective));
        return sql.toString();
    }

    private String update(Object entity, boolean selective, MappedStatement ms) {
        FieldFillUtil.fillFields(entity, FillType.UPDATE);

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClause(entity, selective, null));
        sql.append(" WHERE ");
        // todo 默认过滤 有
        sql.append(SqlFieldUtils.buildWhereId(entity));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    private String updateByCondition(Object entity, boolean selective, MappedStatement ms) {
        @SuppressWarnings("unchecked")
        Map<String, Object> paramMap = (Map<String, Object>) entity;
        Object targetEntity = paramMap.get("entity");
        Object conditionObj = paramMap.get("condition");
        ConditionWrapper<?> condition = (ConditionWrapper<?>) conditionObj;
        FieldFillUtil.fillFields(targetEntity, FillType.UPDATE);

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" SET ");
        sql.append(SqlFieldUtils.buildSetClause(targetEntity, selective, "entity"));
        sql.append(" WHERE 1=1");
        // todo 默认过滤 有
        sql.append(SqlFieldUtils.buildWhereClause(condition, "condition"));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

    private String buildSelectSql(ConditionWrapper<?> condition, MappedStatement ms, boolean withLimit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(String.join(",", MybatisUtils.getColumnsByMs(ms)));
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE 1=1");
        // todo 默认过滤 无
        sql.append(SqlFieldUtils.buildWhereClause(condition, null));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        sql.append(MybatisUtils.buildGroupByClause(condition));
        sql.append(MybatisUtils.buildOrderByClause(condition));
        if (withLimit) {
            sql.append(" LIMIT 1");
        }
        return sql.toString();
    }

    private String buildSelectByIdSql(Object entity, MappedStatement ms, boolean count, boolean forUpdate) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(count ? "COUNT(1)" : String.join(",", MybatisUtils.getColumnsByMs(ms)));
        sql.append(" FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE ");
        sql.append(SqlFieldUtils.buildWhereId(entityClass, entity));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        if (forUpdate) {
            sql.append(" FOR UPDATE");
        }
        return sql.toString();
    }

    private String buildCountSql(ConditionWrapper<?> condition, MappedStatement ms, boolean withLimit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(1) FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        sql.append(" WHERE 1=1");
        // todo 默认过滤 无
        sql.append(SqlFieldUtils.buildWhereClause(condition, null));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        if (withLimit) {
            sql.append(" LIMIT 1");
        }
        return sql.toString();
    }

    private String buildUpdateByConditionSql(ConditionWrapper<?> condition, MappedStatement ms, boolean logicDelete) {
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);

        StringBuilder sql = new StringBuilder();
        sql.append(logicDelete ? "UPDATE " : "DELETE FROM ");
        sql.append(MybatisUtils.getTableNameByMs(ms));
        if (logicDelete) {
            sql.append(" SET ");
            sql.append(MybatisUtils.getLogicColumn(ms));
            sql.append(", ");
            sql.append(FieldFillUtil.buildFillSql(entityClass, FillType.UPDATE));
        }
        sql.append(" WHERE 1=1");
        // todo 默认过滤 无
        sql.append(SqlFieldUtils.buildWhereClause(condition, null));
        sql.append(MybatisUtils.getDeleteFilterSql(ms));
        return sql.toString();
    }

}
