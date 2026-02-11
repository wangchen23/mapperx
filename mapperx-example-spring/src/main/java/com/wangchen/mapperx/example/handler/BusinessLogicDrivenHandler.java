package com.wangchen.mapperx.example.handler;

import com.wangchen.mapperx.core.handler.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * BusinessLogicDrivenHandler
 *
 * @author chenwang
 * @date 2026/2/6 16:49
 **/
@Component
public class BusinessLogicDrivenHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (!needInsertFill(metaObject, "userName")) {
            return;
        }
        if (metaObject.hasSetter("userName") && metaObject.getValue("userName") == null) {
            metaObject.setValue("userName", "system");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (!needUpdateFill(metaObject, "userName")) {
            return;
        }
        if (metaObject.hasSetter("userName") && metaObject.getValue("userName") == null) {
            metaObject.setValue("userName", "system");
        }
    }
}
