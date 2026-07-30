package com.wangchen.mapperx.core.util;

import com.wangchen.mapperx.core.annotation.CondOp;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * LogicDeleteUtil
 *
 * @author 13741
 * @date 2026/7/30 23:32
 **/
public class LogicDeleteUtil {

    /**
     * 根据MappedStatement获取逻辑删除附加填充参数
     * 仅 @CondOp 方法返回值，其余返回空Map
     */
    public static Map<String, Object> getFieldMap(MappedStatement ms) {
        Method mapperMethod = MybatisUtils.getMethodByMs(ms);
        if (mapperMethod == null || !mapperMethod.isAnnotationPresent(CondOp.class)) {
            return Collections.emptyMap();
        }
        Class<?> entityClass = MybatisUtils.getEntityClassByMs(ms);
        Map<String, Object> map = new HashMap<>(4);

        FieldFillUtil.getFillValue(map, entityClass);
        
        return map;
    }
}
