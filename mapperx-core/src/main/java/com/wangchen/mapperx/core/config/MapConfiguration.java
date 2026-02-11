package com.wangchen.mapperx.core.config;


import com.wangchen.mapperx.core.annotation.Batch;
import com.wangchen.mapperx.core.annotation.IdStrategy;
import com.wangchen.mapperx.core.annotation.MapMethod;
import com.wangchen.mapperx.core.annotation.PrimaryKey;
import com.wangchen.mapperx.core.annotation.SqlCommand;
import com.wangchen.mapperx.core.api.BaseMapperRepository;
import com.wangchen.mapperx.core.provider.DynamicSQLProvider;
import com.wangchen.mapperx.core.sql.DynamicSqlSource;
import com.wangchen.mapperx.core.util.ClassUtils;
import com.wangchen.mapperx.core.util.SqlFieldUtils;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 MyBatis Configuration，实现三大核心能力：
 * 1. 自动注册 BaseMapperRepository 中的通用方法（泛型驱动）
 * 2. 支持 @MapMethod 方法映射（别名代理）
 *
 * @author chenwang
 * @date 2025/6/19 12:39
 */
public class MapConfiguration extends Configuration {

    /** 缓存方法上的 @MapMethod 的 targetId 注解解析结果 */
    private final Map<String, String> annotationResults = new ConcurrentHashMap<>();
    // 新增缓存：MS ID → 对应的ResultMap（解决@MapMethod返回值不一致）
    private final Map<String, ResultMap> methodResultMapCache = new ConcurrentHashMap<>();

    /**
     * 【入口】当 MyBatis 添加 Mapper 接口时触发
     * 除了调用父类逻辑，还会尝试注册其继承的 BaseMapperRepository 泛型方法
     */
    @Override
    public <T> void addMapper(Class<T> type) {
        if (type.isInterface()) {
            System.out.println(">>> MapConfiguration.addMapper: " + type.getName());
            super.addMapper(type);
            registerSuperInterfaces(type);
        }
    }

    /**
     * 【核心】自动注册 BaseMapperRepository 的泛型方法为 MappedStatement
     * 前提：当前接口直接继承 BaseMapperRepository< T, K, Q>
     */
    private void registerSuperInterfaces(Class<?> mapperInterface) {
        Class<?>[] parents = mapperInterface.getInterfaces();
        // 只处理直接继承 BaseMapperRepository 的接口
        if (parents.length == 0 || !BaseMapperRepository.class.isAssignableFrom(parents[0])) {
            return;
        }

        // 【关键】解析泛型实参
        Type genericSuper = mapperInterface.getGenericInterfaces()[0];
        Type[] args = ((ParameterizedType) genericSuper).getActualTypeArguments();

        Class<?> entityClass = (Class<?>) args[0];
        validatePrimaryKeyAnnotation(entityClass);
        Method[] baseMethods = parents[0].getMethods();
        String mapperName = mapperInterface.getName();
        // 第一轮：注册非 MapMethod 方法
        for (Method method : baseMethods) {
            if (method.getAnnotation(MapMethod.class) != null) {
                continue;
            }
            registerMethod(mapperName, method, entityClass);
        }

        // 第二轮：注册 @MapMethod 方法（此时目标方法已存在）
        for (Method method : baseMethods) {
            if (method.getAnnotation(MapMethod.class) == null) {
                continue;
            }
            registerMethod(mapperName, method, entityClass);
        }
    }

    private void validatePrimaryKeyAnnotation(Class<?> entityClass) {
        List<Field> fields = ClassUtils.getFieldsByAnnotation(entityClass, PrimaryKey.class);
        if (fields.isEmpty()) {
            throw new IllegalStateException("Entity [" + entityClass.getSimpleName() + "] must have at least one field annotated with @PrimaryKey.");
        }
    }

    private void registerMethod(String mapperName, Method method, Class<?> entityClass) {
        String msId = mapperName + "." + method.getName();
        checkMethod(mapperName, method);
        if (mappedStatements.containsKey(msId)) {
            return;
        }
        DynamicSqlSource sqlSource = buildSqlSource(method);
        SqlCommand sqlCommand = method.getAnnotation(SqlCommand.class);
        // 通过 MapMethod 映射的会为空，但是在后面的 addMappedStatement 中会被替换掉，所以不影响
        SqlCommandType cmd = Optional.ofNullable(sqlCommand).map(SqlCommand::value).orElse(null);
        Class<?> param = method.getParameterCount() > 0 ? method.getParameterTypes()[0] : Object.class;
        ParameterMap paramMap = new ParameterMap.Builder(this, msId + "-Inline", param, Collections.emptyList()).build();

        // 构建并注册 ResultMap（id = msId）
        ResultMap rm = buildResultMap(msId, method, entityClass);
        super.addResultMap(rm);
        methodResultMapCache.put(msId, rm);

        // ========== 自动配置 AUTO 主键回写 ==========
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        String autoKeyProp = null;
        if (cmd == SqlCommandType.INSERT) {
            autoKeyProp = getAutoKeyProperty(entityClass);
            if (autoKeyProp != null) {
                //keyGenerator = new AutoGeneratedKeyGenerator(autoKeyProp);
                keyGenerator = Jdbc3KeyGenerator.INSTANCE;
            }
        }

        MappedStatement.Builder msBuilder = new MappedStatement.Builder(this, msId, sqlSource, cmd).parameterMap(paramMap).resultMaps(Collections.singletonList(rm)).keyGenerator(keyGenerator);

        if (autoKeyProp != null) {
            msBuilder.keyProperty(autoKeyProp);
        }

        MappedStatement ms = msBuilder.build();
        sqlSource.setMappedStatement(ms);
        this.addMappedStatement(ms);
    }

    private void checkMethod(String mapperName, Method method) {
        boolean isBatch = method.isAnnotationPresent(Batch.class);
        MapMethod mapMethodAnno = method.getAnnotation(MapMethod.class);

        // 规则1: 有 @Batch ⇒ 必须有有效 @MapMethod
        if (isBatch) {
            if (mapMethodAnno == null || mapMethodAnno.value().isEmpty()) {
                throw new IllegalStateException("Method [" + method.getName() + "] in [" + mapperName + "] has @Batch but missing or empty @MapMethod annotation.");
            }
        }

        // 规则2: 有 @MapMethod ⇒ 对应的 MappedStatement 必须已注册
        if (mapMethodAnno != null && !mapMethodAnno.value().isEmpty()) {
            String targetMethodName = mapMethodAnno.value();
            String targetMsId = mapperName + "." + targetMethodName;

            if (!mappedStatements.containsKey(targetMsId)) {
                throw new IllegalStateException("Method [" + method.getName() + "] in [" + mapperName + "] references non-existent MappedStatement via @MapMethod: '" + targetMsId + "'. " + "Ensure the target method is registered before this one.");
            }
        }
    }

    /**
     * 【动态 SQL】根据方法名匹配对应的 SQL 生成策略（Provider 方法）
     * 当前仅支持 insert，可按需扩展
     */
    private DynamicSqlSource buildSqlSource(Method method) {
        String providerMethod = method.getName();
        // 从 DynamicSQLProvider 中查找匹配的方法; 若通过 MapMethod 映射的会为空，但是在后面的 addMappedStatement 中会被替换掉，所以不影响
        Method pm = Arrays.stream(DynamicSQLProvider.class.getMethods()).filter(m -> m.getName().equals(providerMethod)).findFirst().orElse(null);

        try {
            Object provider = DynamicSQLProvider.class.getDeclaredConstructor().newInstance();
            return new DynamicSqlSource(provider, pm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 构建 ResultMap：支持实体、List<实体>
     */
    private ResultMap buildResultMap(String msId, Method method, Class<?> entity) {
        Class<?> returnType = method.getReturnType();
        boolean isSimpleType = returnType.isPrimitive();
        Class<?> resultType = isSimpleType ? returnType : entity;
        List<ResultMapping> mappings = new ArrayList<>();
        if (isSimpleType) {
            return new ResultMap.Builder(this, msId, resultType, mappings).build();
        }
        for (Map.Entry<String, Field> entry : ClassUtils.getFieldMap(entity).entrySet()) {
            Field field = entry.getValue();
            String columnName = SqlFieldUtils.getColumnName(field);
            ResultMapping mapping = new ResultMapping.Builder(this, field.getName(), columnName, field.getType()).build();
            mappings.add(mapping);
        }
        return new ResultMap.Builder(this, msId, resultType, mappings).build();
    }

    /**
     * 获取实体类中 @PrimaryKey(strategy = IdStrategy.AUTO) 的字段名
     * 前提：每个实体最多一个主键（你已保证）
     */
    private String getAutoKeyProperty(Class<?> entityClass) {
        List<Field> primaryKeyFields = ClassUtils.getFieldsByAnnotation(entityClass, PrimaryKey.class);
        if (primaryKeyFields.isEmpty()) {
            return null;
        }

        // 按约定取第一个（你已确保唯一性）
        Field field = primaryKeyFields.get(0);
        PrimaryKey pk = field.getAnnotation(PrimaryKey.class);
        if (pk.strategy() == IdStrategy.AUTO) {
            return field.getName();
        }
        return null;
    }

    // ====== 以下为 @MapMethod 字段映射支持 ======

    /**
     * 【拦截点】所有 MappedStatement 注册都会经过此方法
     * 在此处处理 @MapMethod（方法别名）
     */
    @Override
    public void addMappedStatement(MappedStatement ms) {
        String id = ms.getId();
        if (mappedStatements.containsKey(id)) {
            return;
        }

        // 解析方法上的注解
        String targetId = annotationResults.computeIfAbsent(id, this::parseAnnotations);
        MappedStatement finalMs = ms;

        // 【@MapMethod】将当前方法映射到另一个已存在的 MappedStatement
        if (targetId != null) {
            MappedStatement target = getMappedStatement(targetId, false);
            ResultMap resultMap = methodResultMapCache.get(id);
            if (target != null) {
                finalMs = copyMappedStatement(target, id, target.getSqlSource(), Collections.singletonList(resultMap));
            }
        }
        super.addMappedStatement(finalMs);
    }

    /**
     * 解析方法上的 @MapMethod 注解
     */
    private String parseAnnotations(String id) {
        String r = null;
        try {
            int dot = id.lastIndexOf('.');
            String className = id.substring(0, dot);
            String methodName = id.substring(dot + 1);
            Class<?> clazz = Resources.classForName(className);
            Method method = ClassUtils.getPublicMethod(clazz, methodName);
            if (method == null) {
                return r;
            }

            MapMethod mm = method.getAnnotation(MapMethod.class);

            if (mm != null && !mm.value().isEmpty()) {
                r = clazz.getName() + "." + mm.value();
            }
        } catch (Exception e) {
            throw new RuntimeException("Parse annotation failed: " + id, e);
        }
        return r;
    }

    /**
     * 复制一个 MappedStatement，仅修改 ID 和 SqlSource
     */
    private MappedStatement copyMappedStatement(MappedStatement source, String newId, SqlSource sqlSource, List<ResultMap> newResultMaps) {
        MappedStatement.Builder b = new MappedStatement.Builder(source.getConfiguration(), newId, sqlSource, source.getSqlCommandType());
        b.resource(source.getResource()).fetchSize(source.getFetchSize()).statementType(source.getStatementType()).resultSetType(source.getResultSetType()).timeout(source.getTimeout()).parameterMap(source.getParameterMap()).resultMaps(newResultMaps).cache(source.getCache()).flushCacheRequired(source.isFlushCacheRequired()).useCache(source.isUseCache()).keyGenerator(source.getKeyGenerator());

        if (source.getKeyProperties() != null && source.getKeyProperties().length > 0) {
            b.keyProperty(String.join(",", source.getKeyProperties()));
        }
        b.databaseId(source.getDatabaseId()).lang(source.getLang());
        return b.build();
    }

    /**
     * 安全添加 KeyGenerator，避免 XML 与注解冲突
     */
    @Override
    public void addKeyGenerator(String id, KeyGenerator kg) {
        if (!keyGenerators.containsKey(id)) {
            keyGenerators.put(id, kg);
        }
    }

}