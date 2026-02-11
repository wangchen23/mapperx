package com.wangchen.mapperx.core.interceptor;

import com.wangchen.mapperx.core.annotation.Batch;
import com.wangchen.mapperx.core.util.ClassUtils;
import com.wangchen.mapperx.core.util.MybatisUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 批量操作智能拦截器
 * - 小批量（≤ threshold）：直接执行原生 <foreach> 方法（高性能）
 * - 大批量（> threshold）：自动拆分为单条执行（防 OOM / SQL 过长）
 *
 * @author chenwang
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchSplitInterceptor implements Interceptor {

    private static final Map<String, Boolean> IS_BATCH_METHOD = new ConcurrentHashMap<>();
    private int batchSize = 500;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];

        if (!IS_BATCH_METHOD.computeIfAbsent(ms.getId(), this::isBatchMethod)) {
            return invocation.proceed();
        }

        List<?> list = MybatisUtils.extractList(param);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        // 小批量：走原生 <foreach>（高性能）
        if (list.size() <= batchSize) {
            return invocation.proceed();
        }

        // 大批量：分片执行（每片 ≤ batchSize）
        Executor executor = (Executor) invocation.getTarget();
        int total = 0;

        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<?> subList = list.subList(i, end);
            Object subParam = rebuildParameter(param, subList);
            total += executor.update(ms, subParam);
        }

        return total;
    }

    // 判断方法是否标注了 @Batch
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

    // 重建参数：将原 param 中的 list 替换为 subList
    private Object rebuildParameter(Object originalParam, List<?> subList) {
        if (originalParam instanceof List) {
            return subList;
        }
        if (originalParam instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> originalMap = (Map<String, Object>) originalParam;
            Map<String, Object> newMap = new HashMap<>(originalMap);

            // 替换 MyBatis 默认的 collection/list 键
            if (newMap.containsKey("list")) {
                newMap.put("list", subList);
            }
            if (newMap.containsKey("collection")) {
                newMap.put("collection", subList);
            }
            return newMap;
        }
        // 理论上不会到这里，因为 extractList 能提取说明是 List/Map
        return subList;
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties props) {
        String size = props.getProperty("maxBatchSize");
        if (size != null) {
            this.batchSize = Integer.parseInt(size);
        }
    }
}