package com.wangchen.mapperx.core.handler;

import com.wangchen.mapperx.core.annotation.FieldFill;
import com.wangchen.mapperx.core.util.ClassUtils;
import org.apache.ibatis.reflection.MetaObject;

/**
 * 元对象处理器接口
 * 提供完全自定义的自动填充能力
 *
 * @author chenwang
 * @date 2026/2/6 16:30
 **/
public interface MetaObjectHandler {

    /**
     * 插入时填充
     * 业务方完全控制填充逻辑
     */
    void insertFill(MetaObject metaObject);

    /**
     * 更新时填充
     * 业务方完全控制填充逻辑
     */
    void updateFill(MetaObject metaObject);

    /**
     * 判断字段是否应在 INSERT 时填充
     */
    default boolean needInsertFill(MetaObject metaObject, String fieldName) {
        return ClassUtils.shouldFill(metaObject.getOriginalObject(), fieldName, FieldFill.INSERT);
    }

    /**
     * 判断字段是否应在 UPDATE 时填充
     */
    default boolean needUpdateFill(MetaObject metaObject, String fieldName) {
        return ClassUtils.shouldFill(metaObject.getOriginalObject(), fieldName, FieldFill.UPDATE);
    }
}
