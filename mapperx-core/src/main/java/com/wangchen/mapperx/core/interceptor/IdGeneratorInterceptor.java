package com.wangchen.mapperx.core.interceptor;

import com.wangchen.mapperx.core.annotation.PrimaryKey;
import com.wangchen.mapperx.core.idgen.SnowflakeIdGenerator;
import com.wangchen.mapperx.core.spi.IdGenerator;
import com.wangchen.mapperx.core.util.ClassUtils;
import com.wangchen.mapperx.core.util.MybatisUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdGeneratorInterceptor
 *
 * @author chenwang
 * @date 2026/2/9 15:09
 **/
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class IdGeneratorInterceptor implements Interceptor {

    private static final IdGenerator<Long> SNOWFLAKE_GENERATOR = new SnowflakeIdGenerator();
    private final Map<String, IdGenerator<?>> generatorMap = new ConcurrentHashMap<>();

    public IdGeneratorInterceptor() {
    }

    public IdGeneratorInterceptor(Map<String, IdGenerator<?>> generatorMap) {
        this.generatorMap.putAll(generatorMap);
    }

    public void registerGenerator(String name, IdGenerator<?> generator) {
        this.generatorMap.put(name, generator);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 只处理 INSERT 操作
        if (ms.getSqlCommandType() != SqlCommandType.INSERT) {
            return invocation.proceed();
        }

        if (parameter != null) {
            processParameter(parameter);
        }

        return invocation.proceed();
    }

    private void processParameter(Object param) {
        List<?> entities = MybatisUtils.extractList(param);
        if (entities == null || entities.isEmpty()) {
            // 可能是单个实体
            fillPrimaryKeyIfAbsent(param);
            return;
        }
        for (Object entity : entities) {
            if (entity != null) {
                fillPrimaryKeyIfAbsent(entity);
            }
        }
    }

    private void fillPrimaryKeyIfAbsent(Object entity) {
        if (entity == null) {
            return;
        }
        Class<?> clazz = entity.getClass();
        List<Field> primaryKeyFields = ClassUtils.getFieldsByAnnotation(clazz, PrimaryKey.class);
        if (primaryKeyFields.isEmpty()) {
            return;
        }
        // ⚠️ 按约定取第一个（你已保证唯一性）
        Field field = primaryKeyFields.get(0);
        try {
            field.setAccessible(true);
            if (field.get(entity) != null) {
                return;
            }
            Object idValue = generateIdByStrategy(clazz, field);
            if (idValue != null) {
                field.set(entity, idValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fill primary key for entity: " + clazz.getSimpleName(), e);
        }
    }

    private Object generateIdByStrategy(Class<?> clazz, Field field) {
        PrimaryKey pk = field.getAnnotation(PrimaryKey.class);
        Object idValue = null;
        switch (pk.strategy()) {
            case UUID:
                idValue = generateUUID();
                break;
            case SNOWFLAKE:
                idValue = SNOWFLAKE_GENERATOR.generate(clazz);
                break;
            case ASSIGN:
                throw new IllegalStateException("Primary key '" + field.getName() + "' requires manual assignment but is null.");
            case CUSTOM:
                idValue = generateCustomId(pk.generator(), clazz);
                break;
            case AUTO:
            case NONE:
                return null;
            default:
                break;
        }
        return idValue;
    }

    private String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private Object generateCustomId(String genName, Class<?> entityClass) {
        if (genName == null || genName.isEmpty()) {
            throw new IllegalArgumentException("Generator name required for CUSTOM strategy");
        }
        IdGenerator<?> gen = generatorMap.get(genName);
        if (gen == null) {
            throw new RuntimeException("No IdentifierGenerator found: " + genName);
        }
        return gen.generate(entityClass);
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
