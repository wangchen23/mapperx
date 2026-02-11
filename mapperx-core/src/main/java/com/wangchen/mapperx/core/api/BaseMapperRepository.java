package com.wangchen.mapperx.core.api;

import com.wangchen.mapperx.core.annotation.Batch;
import com.wangchen.mapperx.core.annotation.MapMethod;
import com.wangchen.mapperx.core.annotation.SqlCommand;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.List;

/**
 * 增强版通用 MyBatis Mapper 接口
 *
 * @author chenwang
 * @date 2025/6/18
 */
public interface BaseMapperRepository<T, K> extends BaseRepository<T, K> {
    // ==================== 查询 ====================

    @Override
    @SqlCommand(SqlCommandType.SELECT)
    T getById(K id);

    @Override
    @SqlCommand(SqlCommandType.SELECT)
    boolean existsById(K id);

    @Override
    @MapMethod("existsById")
    boolean existsByCondition(ConditionWrapper<T> condition);

    @Override
    @SqlCommand(SqlCommandType.SELECT)
    T selectOne(ConditionWrapper<T> condition);

    @Override
    @MapMethod("getById")
    List<T> list(ConditionWrapper<T> condition);

    @Override
    @MapMethod("getById")
    List<T> listByIds(List<K> idList);

    @Override
    @MapMethod("existsById")
    long count(ConditionWrapper<T> condition);

    @Override
    @SqlCommand(SqlCommandType.SELECT)
    T lockById(K id);


    // ==================== 插入 ====================

    @Override
    @SqlCommand(SqlCommandType.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(T entity);

    @Override
    @Batch
    @MapMethod(value = "insert")
    int batchInsert(List<T> entityList);


    @Override
    @SqlCommand(SqlCommandType.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSelective(T entity);

    @Override
    @Batch
    @MapMethod(value = "insertSelective")
    int batchInsertSelective(List<T> entityList);


    // ==================== 更新 ====================

    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int update(T entity);

    @Override
    @Batch
    @MapMethod(value = "update")
    int batchUpdate(List<T> entityList);

    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int updateSelective(T entity);

    @Override
    @Batch
    @MapMethod(value = "updateSelective")
    int batchUpdateSelective(List<T> entityList);

    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int updateByCondition(@Param("entity") T entity, @Param("condition") ConditionWrapper<T> condition);


    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int updateByConditionSelective(@Param("entity") T entity, @Param("condition") ConditionWrapper<T> condition);

    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int updateByConditionWithFields(@Param("updateSpec") UpdateSpec<T> updateSpec, @Param("condition") ConditionWrapper<T> condition);

    // ==================== 删除 ====================

    @Override
    @SqlCommand(SqlCommandType.UPDATE)
    int logicDelete(K id);

    @Override
    @Batch
    @MapMethod(value = "logicDelete")
    int batchLogicDelete(List<K> idList);

    @Override
    @MapMethod(value = "logicDelete")
    int logicDeleteByCondition(ConditionWrapper<T> condition);

    @Override
    @SqlCommand(SqlCommandType.DELETE)
    int delete(K id);

    @Override
    @Batch
    @MapMethod(value = "delete")
    int batchDelete(List<K> idList);

    @Override
    @MapMethod(value = "delete")
    int deleteByCondition(ConditionWrapper<T> condition);
}