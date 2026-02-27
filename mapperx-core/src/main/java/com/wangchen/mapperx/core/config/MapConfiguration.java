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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 【优雅实现】MyBatis 泛型 Mapper 增强器（组合模式）
 * 特性：
 * 1. 用户自定义方法（XML/注解）优先，自动跳过冲突的 Base 方法
 * 2. @MapMethod 仅注册别名，无冗余 SQL
 * 3. 不继承 Configuration，兼容未来版本
 *
 * @author chenwang
 */
public class MapConfiguration {

    private final Configuration configuration;

    public MapConfiguration(Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    }

    /**
     * 【推荐入口】在所有 Mapper 注册完成后调用（如 Spring 的 ApplicationRunner）
     */
    public void enhance() {
        // 获取所有已注册的 Mapper 接口
        Collection<Class<?>> mappers = configuration.getMapperRegistry().getMappers();
        for (Class<?> mapper : mappers) {
            if (isBaseMapper(mapper)) {
                registerBaseMethods(mapper);
            }
        }
    }

    private boolean isBaseMapper(Class<?> mapperInterface) {
        return Arrays.stream(mapperInterface.getInterfaces())
                .anyMatch(BaseMapperRepository.class::isAssignableFrom);
    }

    private void registerBaseMethods(Class<?> mapperInterface) {
        // 查找继承的 BaseMapperRepository 接口及泛型参数
        Class<?> baseParent = null;
        Type genericType = null;
        Class<?>[] interfaces = mapperInterface.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (BaseMapperRepository.class.isAssignableFrom(interfaces[i])) {
                baseParent = interfaces[i];
                genericType = mapperInterface.getGenericInterfaces()[i];
                break;
            }
        }
        if (baseParent == null || !(genericType instanceof ParameterizedType)) {
            return;
        }

        // 修复：严谨校验泛型参数类型
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length == 0 || !(actualTypeArguments[0] instanceof Class)) {
            throw new IllegalStateException("BaseMapperRepository generic parameter must be a Class type for mapper: " + mapperInterface.getName());
        }
        Class<?> entityClass = (Class<?>) actualTypeArguments[0];

        validatePrimaryKeyAnnotation(entityClass);

        String mapperName = mapperInterface.getName();
        Method[] methods = baseParent.getMethods();

        // 先注册普通方法（非 @MapMethod）
        for (Method method : methods) {
            if (method.getAnnotation(MapMethod.class) == null) {
                registerNormalMethod(mapperName, method, entityClass);
            }
        }

        // 再注册 @MapMethod 别名（此时目标方法一定存在）
        for (Method method : methods) {
            MapMethod anno = method.getAnnotation(MapMethod.class);
            if (anno != null && !anno.value().isEmpty()) {
                registerMapMethodAlias(mapperName, method, anno, entityClass);
            }
        }
    }

    // ==================== 普通方法注册 ====================
    private void registerNormalMethod(String mapperName, Method method, Class<?> entityClass) {
        String msId = mapperName + "." + method.getName();
        if (configuration.hasStatement(msId, false)) {
            return; // 用户已定义，跳过
        }

        validateBatchAnnotation(method);

        SqlCommand sqlCommand = method.getAnnotation(SqlCommand.class);
        SqlCommandType cmd = Optional.ofNullable(sqlCommand).map(SqlCommand::value).orElse(null);

        DynamicSqlSource sqlSource = buildSqlSource(method);
        Class<?> paramType = method.getParameterCount() > 0 ? method.getParameterTypes()[0] : Object.class;
        ParameterMap paramMap = new ParameterMap.Builder(configuration, msId + "-Inline", paramType, Collections.emptyList()).build();

        ResultMap resultMap = buildResultMap(msId, method, entityClass);
        // 修复：先检查 ResultMap 是否已存在，避免重复注册
        if (!configuration.hasResultMap(resultMap.getId())) {
            configuration.addResultMap(resultMap);
        }

        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        String autoKeyProp = null;
        if (cmd == SqlCommandType.INSERT) {
            autoKeyProp = getAutoKeyProperty(entityClass);
            if (autoKeyProp != null) {
                keyGenerator = Jdbc3KeyGenerator.INSTANCE;
            }
        }

        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, cmd)
                .parameterMap(paramMap)
                .resultMaps(Collections.singletonList(resultMap))
                .keyGenerator(keyGenerator)
                .keyProperty(autoKeyProp)
                .build();

        sqlSource.setMappedStatement(ms);
        configuration.addMappedStatement(ms);
    }

    // ==================== @MapMethod 别名注册 ====================
    private void registerMapMethodAlias(String mapperName, Method method, MapMethod mapMethodAnno, Class<?> entityClass) {
        String msId = mapperName + "." + method.getName();
        if (configuration.hasStatement(msId, false)) {
            return;
        }

        validateBatchAnnotation(method);

        String targetMsId = mapperName + "." + mapMethodAnno.value();
        if (!configuration.hasStatement(targetMsId, false)) {
            throw new IllegalStateException("Target method not found for @MapMethod: " + targetMsId);
        }

        ResultMap resultMap = buildResultMap(msId, method, entityClass);
        // 修复：先检查 ResultMap 是否已存在，避免重复注册
        if (!configuration.hasResultMap(resultMap.getId())) {
            configuration.addResultMap(resultMap);
        }

        MappedStatement targetMs = configuration.getMappedStatement(targetMsId, false);
        MappedStatement aliasMs = copyMappedStatement(targetMs, msId, targetMs.getSqlSource(), Collections.singletonList(resultMap));

        configuration.addMappedStatement(aliasMs);
    }

    // ==================== 工具方法 ====================
    private void validatePrimaryKeyAnnotation(Class<?> entityClass) {
        List<Field> fields = ClassUtils.getFieldsByAnnotation(entityClass, PrimaryKey.class);
        if (fields.isEmpty()) {
            throw new IllegalStateException("Entity [" + entityClass.getSimpleName() + "] must have exactly one @PrimaryKey field");
        }
        // 修复：校验主键字段唯一性
        if (fields.size() > 1) {
            throw new IllegalStateException("Entity [" + entityClass.getSimpleName() + "] cannot have more than one @PrimaryKey field");
        }
    }

    private void validateBatchAnnotation(Method method) {
        // 修复：@Batch 注解校验逻辑，仅当存在 @Batch 时检查 @MapMethod 是否非空
        if (method.isAnnotationPresent(Batch.class)) {
            MapMethod mm = method.getAnnotation(MapMethod.class);
            if (mm == null || mm.value().isEmpty()) {
                throw new IllegalStateException("Method [" + method.getName() + "] with @Batch must have non-empty @MapMethod");
            }
        }
    }

    private DynamicSqlSource buildSqlSource(Method method) {
        String methodName = method.getName();
        Method providerMethod = Arrays.stream(DynamicSQLProvider.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No SQL provider for method: " + methodName));

        try {
            DynamicSQLProvider provider = DynamicSQLProvider.class.getDeclaredConstructor().newInstance();
            return new DynamicSqlSource(provider, providerMethod);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SQL source for method: " + methodName, e);
        }
    }

    private ResultMap buildResultMap(String msId, Method method, Class<?> entity) {
        Class<?> returnType = method.getReturnType();
        // 使用 MyBatis 内置工具判断简单类型，替代自定义 SqlFieldUtils
        boolean isSimple = isMyBatisSimpleType(returnType);
        Class<?> resultType = isSimple ? returnType : entity;

        List<ResultMapping> mappings = new ArrayList<>();
        if (!isSimple) {
            for (Map.Entry<String, Field> entry : ClassUtils.getFieldMap(entity).entrySet()) {
                Field field = entry.getValue();
                String column = SqlFieldUtils.getColumnName(field);
                mappings.add(new ResultMapping.Builder(configuration, field.getName(), column, field.getType()).build());
            }
        }
        // ResultMap ID 增加唯一性标识，避免冲突
        String resultMapId = msId + "-ResultMap";
        return new ResultMap.Builder(configuration, resultMapId, resultType, mappings).build();
    }

    /**
     * 适配 MyBatis 的简单类型判断（推荐版本）
     * 1. 优先用 isPrimitive() 判断基本类型（遵循官方语义）
     * 2. 补充 MyBatis 核心简单类型（包装类、String、常用日期/数值）
     * 3. 可根据业务灵活增删，无版本/反射依赖
     */
    private boolean isMyBatisSimpleType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        // 1. 基本类型（严格遵循 isPrimitive() 官方规则：8个基本类型+void）
        if (clazz.isPrimitive()) {
            return true;
        }

        // 2. 核心包装类型（对应8个基本类型，必加）
        if (clazz.equals(Boolean.class) || clazz.equals(Byte.class)
                || clazz.equals(Character.class) || clazz.equals(Short.class)
                || clazz.equals(Integer.class) || clazz.equals(Long.class)
                || clazz.equals(Float.class) || clazz.equals(Double.class)) {
            return true;
        }

        // 3. 常用简单类型（根据业务场景增删，以下是通用必选）
        return clazz.equals(String.class)
                || clazz.equals(Date.class)
                || clazz.equals(java.sql.Date.class)
                || clazz.equals(BigDecimal.class)
                || clazz.isEnum();
    }

    private String getAutoKeyProperty(Class<?> entityClass) {
        List<Field> fields = ClassUtils.getFieldsByAnnotation(entityClass, PrimaryKey.class);
        if (!fields.isEmpty()) {
            Field field = fields.get(0);
            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            // 修复：增加空指针防护
            if (primaryKey != null && primaryKey.strategy() == IdStrategy.AUTO) {
                return field.getName();
            }
        }
        return null;
    }

    private MappedStatement copyMappedStatement(MappedStatement source, String newId, SqlSource sqlSource, List<ResultMap> resultMaps) {
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, newId, sqlSource, source.getSqlCommandType())
                .resource(source.getResource())
                .fetchSize(source.getFetchSize())
                .statementType(source.getStatementType())
                .resultSetType(source.getResultSetType())
                .timeout(source.getTimeout())
                .parameterMap(source.getParameterMap())
                .resultMaps(resultMaps)
                .cache(source.getCache())
                .flushCacheRequired(source.isFlushCacheRequired())
                .useCache(source.isUseCache())
                .keyGenerator(source.getKeyGenerator())
                .databaseId(source.getDatabaseId())
                .lang(source.getLang());

        // 修复：规范设置 keyProperty，避免空字符串
        if (source.getKeyProperties() != null && source.getKeyProperties().length > 0) {
            String keyProperty = String.join(",", source.getKeyProperties());
            if (!keyProperty.isEmpty()) {
                builder.keyProperty(keyProperty);
            }
        }
        return builder.build();
    }
}