package com.wangchen.mapperx.core.interceptor;

import com.wangchen.mapperx.core.handler.MetaObjectHandler;
import com.wangchen.mapperx.core.util.MybatisUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.List;

/**
 * AutoFillInterceptor
 *
 * @author chenwang
 * @date 2026/2/6 15:34
 **/
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class AutoFillInterceptor implements Interceptor {

    // 直接持有用户实现的处理器实例
    private final MetaObjectHandler metaObjectHandler;

    public AutoFillInterceptor(MetaObjectHandler metaObjectHandler) {
        this.metaObjectHandler = metaObjectHandler;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];
        SqlCommandType cmd = ms.getSqlCommandType();
        if (cmd == SqlCommandType.INSERT || cmd == SqlCommandType.UPDATE) {
            processParameter(param, cmd);
        }
        return invocation.proceed();
    }

    /**
     * 递归处理参数（支持单个实体、List、Map包装的实体）
     */
    private void processParameter(Object param, SqlCommandType cmd) {
        if (param == null) {
            return;
        }
        List<?> entities = MybatisUtils.extractList(param);
        if (entities != null) {
            for (Object entity : entities) {
                performAutoFill(entity, cmd);
            }
            return;
        }
        performAutoFill(param, cmd);
    }

    private void performAutoFill(Object entity, SqlCommandType cmd) {
        if (entity == null) {
            return;
        }
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        if (cmd == SqlCommandType.INSERT) {
            metaObjectHandler.insertFill(metaObject);
        } else if (cmd == SqlCommandType.UPDATE) {
            metaObjectHandler.updateFill(metaObject);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
