package com.wangchen.mapperx.core.api;


import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;

import java.util.List;

/**
 * 通用数据访问接口（规范增强版）
 * <p>
 * 泛型说明：
 * - T: 实体 DO（Data Object）
 * - K: 主键类型（如 Long, String）
 * <p>
 * 设计原则：
 * - 所有批量操作要求列表非 null 且非空（实现类应做校验）
 * - Selective 方法仅操作非 null 字段，避免覆盖
 * - 逻辑删除 ≠ 物理删除，语义分离
 *
 * @author chenwang
 * @date 2025/6/18 16:49
 */
public interface BaseRepository<T, K> {

    // ==================== 查询 ====================

    /**
     * 根据主键查询单条记录
     */
    T getById(K id);

    /**
     * 判断主键对应的记录是否存在
     */
    boolean existsById(K id);

    /**
     * 判断是否满足查询条件的记录存在
     */
    boolean existsByCondition(ConditionWrapper<T> condition);

    /**
     * 根据条件查询单条记录（多条报错，没有为 null）
     */
    T selectOne(ConditionWrapper<T> condition);

    /**
     * 根据条件查询多条记录
     */
    List<T> list(ConditionWrapper<T> condition);

    /**
     * 根据主键列表批量查询
     *
     * @param idList 主键列表，不可为 null 或空
     */
    List<T> listByIds(List<K> idList);

    /**
     * 根据条件统计记录总数
     */
    long count(ConditionWrapper<T> condition);

    /**
     * 根据主键查询并加行锁（SELECT ... FOR UPDATE）
     * <p>
     * ⚠️ 使用前提：
     * 1. 必须在 @Transactional 事务方法中调用
     * 2. 仅用于防止并发修改的关键场景（如库存、状态机）
     * 3. 避免在高并发热点数据上使用
     */
    T lockById(K id);

    // ==================== 插入 ====================

    /**
     * 插入实体（所有字段，含 null）
     */
    int insert(T entity);

    /**
     * 批量插入实体（所有字段）
     *
     * @param entityList 实体列表，不可为 null 或空
     */
    int batchInsert(List<T> entityList);

    /**
     * 插入实体（仅非 null 字段，推荐）
     */
    int insertSelective(T entity);

    /**
     * 批量插入实体（仅非 null 字段，推荐）
     *
     * @param entityList 实体列表，不可为 null 或空
     */
    int batchInsertSelective(List<T> entityList);


    // ==================== 更新 ====================

    /**
     * ⚠️全字段更新（字段含 null，可能清空数据，慎用）
     */
    int update(T entity);

    /**
     * ⚠️批量全字段更新（字段含 null，可能清空数据，慎用）
     *
     * @param entityList 实体列表，不可为 null 或空
     */
    int batchUpdate(List<T> entityList);

    /**
     * 安全更新（仅非 null 字段，推荐）
     */
    int updateSelective(T entity);

    /**
     * 批量安全更新（仅非 null 字段，推荐）
     *
     * @param entityList 实体列表，不可为 null 或空
     */
    int batchUpdateSelective(List<T> entityList);

    /**
     * ⚠️ 根据条件全字段更新（极度危险！可能清空全表字段）
     *
     * @param entity    待更新字段（含 null）
     * @param condition 更新条件（必须有效，避免全表更新）
     */
    int updateByCondition(T entity, ConditionWrapper<T> condition);

    /**
     * 根据条件安全更新（仅非 null 字段）
     *
     * @param entity    待更新字段（非 null 生效）
     * @param condition 更新条件（必须有效，避免全表更新）
     */
    int updateByConditionSelective(T entity, ConditionWrapper<T> condition);

    /**
     * 根据条件更新指定字段 （可更新指定字段为 null）
     *
     * @param updateSpec 待更新的指定字段
     * @param condition  更新条件（必须有效，避免全表更新）
     */
    int updateByConditionWithFields(UpdateSpec<T> updateSpec, ConditionWrapper<T> condition);


    // ==================== 删除 ====================

    /**
     * 逻辑删除（如 is_deleted = 1）
     */
    int logicDelete(K id);

    /**
     * 批量逻辑删除
     *
     * @param idList 主键列表，不可为 null 或空
     */
    int batchLogicDelete(List<K> idList);

    /**
     * 根据条件逻辑删除
     *
     * @param condition 删除条件（必须有效，避免全表逻辑删除）
     */
    int logicDeleteByCondition(ConditionWrapper<T> condition);

    /**
     * ⚠️ 物理删除（真删！不可恢复！）
     */
    int delete(K id);

    /**
     * ⚠️ 批量物理删除
     *
     * @param idList 主键列表，不可为 null 或空
     */
    int batchDelete(List<K> idList);

    /**
     * ⚠️ 根据条件物理删除（极度危险！）
     *
     * @param condition 删除条件
     */
    int deleteByCondition(ConditionWrapper<T> condition);
}