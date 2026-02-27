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
 * MyBatis 批量操作智能拦截器（线程安全、无日志依赖版）
 * - 小批量（≤ threshold）：走原生 <foreach> 批量（高性能）
 * - 大批量（> threshold）：创建独立 BatchExecutor 单条循环执行（防 OOM / SQL 过长）
 *
 * @author chenwang
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchSplitInterceptor implements Interceptor {

    private static final Map<String, Boolean> IS_BATCH_METHOD = new ConcurrentHashMap<>();
    private static final Map<String, String> BATCH_TO_SINGLE_MS_ID = new ConcurrentHashMap<>();

    private int splitThreshold = 1000;
    private int batchChunkSize = 300;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];

        // 非 @Batch 方法直接放行
        if (!IS_BATCH_METHOD.computeIfAbsent(ms.getId(), this::isBatchMethod)) {
            return invocation.proceed();
        }

        List<?> list = MybatisUtils.extractList(param);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        // 小批量：走原生 <foreach>（由 MyBatis XML 处理）
        if (list.size() <= splitThreshold) {
            return invocation.proceed();
        }

        // === 大批量：创建独立 BatchExecutor 执行 ===
        Executor originalExecutor = (Executor) invocation.getTarget();
        Configuration configuration = ms.getConfiguration();
        Transaction transaction = originalExecutor.getTransaction();

        // 获取对应的单条操作 MappedStatement
        String singleMsId = BATCH_TO_SINGLE_MS_ID.computeIfAbsent(ms.getId(), this::deriveSingleMsId);
        MappedStatement singleMs = configuration.getMappedStatement(singleMsId, false);

        // 创建独立的 BatchExecutor（复用当前事务，线程安全）
        Executor batchExecutor = configuration.newExecutor(transaction, ExecutorType.BATCH);

        int total = 0;
        int count = 0;
        try {
            for (Object item : list) {
                batchExecutor.update(singleMs, item);
                total++;
                count++;
                if (count >= batchChunkSize) {
                    batchExecutor.flushStatements();
                    count = 0;
                }
            }
            // 执行所有 pending 的 batch 语句（但不提交）
            batchExecutor.flushStatements();
            return total;
        } catch (Exception e) {
            throw new PersistenceException("Batch method [" + ms.getId() + "] failed after processing " + total + " items", e);
        }
    }

    /**
     * 根据 @MapMethod 注解获取单条方法 ID（强约定：有 @Batch 必有 @MapMethod）
     */
    private String deriveSingleMsId(String batchMsId) {
        int lastDot = batchMsId.lastIndexOf('.');
        String className = batchMsId.substring(0, lastDot);
        String methodName = batchMsId.substring(lastDot + 1);

        try {
            Class<?> mapperClass = Class.forName(className);
            Method batchMethod = ClassUtils.getPublicMethod(mapperClass, methodName);
            MapMethod mapMethodAnn = batchMethod.getAnnotation(MapMethod.class);
            return className + "." + mapMethodAnn.value();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Mapper class not found: " + className, e);
        }
    }

    /**
     * 判断是否为 @Batch 方法
     */
    private boolean isBatchMethod(String msId) {
        try {
            int dot = msId.lastIndexOf('.');
            String className = msId.substring(0, dot);
            String methodName = msId.substring(dot + 1);

            Class<?> mapperClass = Class.forName(className);
            Method method = ClassUtils.getPublicMethod(mapperClass, methodName);
            return method != null && method.isAnnotationPresent(Batch.class);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties props) {
        if (props == null) {
            return;
        }

        try {
            String v = props.getProperty("splitThreshold");
            if (v != null) {
                int n = Integer.parseInt(v.trim());
                if (n > 0) {
                    splitThreshold = n;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            String v = props.getProperty("batchChunkSize");
            if (v != null) {
                int n = Integer.parseInt(v.trim());
                if (n > 0) {
                    batchChunkSize = n;
                }
            }
        } catch (Exception ignored) {
        }
    }
}