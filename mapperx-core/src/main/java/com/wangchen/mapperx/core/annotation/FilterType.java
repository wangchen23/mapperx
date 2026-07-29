package com.wangchen.mapperx.core.annotation;

/**
 * 字段过滤类型
 * 决定字段在什么操作时作为 WHERE 条件过滤
 *
 * @author chenwang
 **/
public enum FilterType {

    NEVER,                   // 不作为过滤条件
    SELECT,                  // 查询时
    UPDATE,                  // 修改时
    DELETE,                  // 删除时
    SELECT_UPDATE,           // 查询和修改时
    SELECT_DELETE,           // 查询和删除时
    UPDATE_DELETE,           // 修改和删除时
    SELECT_UPDATE_DELETE     // 查询、修改和删除时
}