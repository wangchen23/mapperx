package com.wangchen.mapperx.core.annotation;

/**
 * 字段填充类型
 * 决定字段在什么操作时自动填充值
 *
 * @author chenwang
 **/
public enum FillType {

    NEVER,         // 不填充
    INSERT,        // 新增时
    UPDATE,        // 修改时
    INSERT_UPDATE  // 新增和修改时
}