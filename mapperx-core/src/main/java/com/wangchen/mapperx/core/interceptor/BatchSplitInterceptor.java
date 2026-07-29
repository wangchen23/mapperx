package com.wangchen.mapperx.core.interceptor;

import com.wangchen.mapperx.core.annotation.Batch;
import com.wangchen.mapperx.core.annotation.MapMethod;
import com.wangchen.mapperx.core.util.ClassUtils;
import com.wangchen.mapperx.core.util.MybatisUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.transaction.Transaction;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量操作拦截器
 * 通过 @Batch 注解将集合参数转换为 JDBC 批量执行
 *
 * @author chenwang
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchSplitInterceptor implements Interceptor {

    private static final Map<String, Boolean> BATCH_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> SINGLE_MS_ID_CACHE = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];

        if (!isBatchMethod(ms.getId())) {
            return invocation.proceed();
        }

        List<?> list = MybatisUtils.extractList(param);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        return executeBatch(invocation, ms, list);
    }

    private int executeBatch(Invocation invocation, MappedStatement ms, List<?> list) {
        Executor originalExecutor = (Executor) invocation.getTarget();
        Configuration configuration = ms.getConfiguration();
        Transaction transaction = originalExecutor.getTransaction();

        String singleMsId = getSingleMsId(ms.getId());
        MappedStatement singleMs = configuration.getMappedStatement(singleMsId, false);
        if (singleMs == null) {
            throw new IllegalStateException("Cannot find mappedStatement specified by @MapMethod, id: " + singleMsId);
        }

        Executor batchExecutor = configuration.newExecutor(transaction, ExecutorType.BATCH);

        try {
            for (Object item : list) {
                batchExecutor.update(singleMs, item);
            }
            batchExecutor.flushStatements();
            return list.size();
        } catch (Exception e) {
            throw new PersistenceException("Batch method [" + ms.getId() + "] execute failed.", e);
        } finally {
            batchExecutor.close(false);
        }
    }

    private boolean isBatchMethod(String msId) {
        return BATCH_METHOD_CACHE.computeIfAbsent(msId, id -> {
            Method method = getMapperMethod(id);
            return method != null && method.isAnnotationPresent(Batch.class);
        });
    }

    private String getSingleMsId(String batchMsId) {
        return SINGLE_MS_ID_CACHE.computeIfAbsent(batchMsId, id -> {
            Method batchMethod = getMapperMethod(id);
            if (batchMethod == null) {
                throw new IllegalStateException("Batch method not found: " + id);
            }
            MapMethod mapMethod = batchMethod.getAnnotation(MapMethod.class);
            if (mapMethod == null) {
                throw new IllegalStateException("@MapMethod annotation missing on batch method: " + id);
            }
            int lastDot = id.lastIndexOf('.');
            return id.substring(0, lastDot + 1) + mapMethod.value();
        });
    }

    private Method getMapperMethod(String msId) {
        int lastDot = msId.lastIndexOf('.');
        String className = msId.substring(0, lastDot);
        String methodName = msId.substring(lastDot + 1);
        try {
            Class<?> mapperClass = Class.forName(className);
            return ClassUtils.getMethod(mapperClass, methodName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Object plugin(Object target) {
        return target instanceof Executor ? Plugin.wrap(target, this) : target;
    }

    @Override
    public void setProperties(Properties props) {
        // no-op
    }
}