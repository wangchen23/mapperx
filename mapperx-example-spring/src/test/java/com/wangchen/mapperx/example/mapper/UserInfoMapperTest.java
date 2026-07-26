package com.wangchen.mapperx.example.mapper;

import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.core.conditions.UpdateSpec;
import com.wangchen.mapperx.example.entity.UserInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserInfoMapper 完整测试类
 * 测试 BaseMapperRepository 所有默认方法
 *
 * @author chenwang
 **/
@SpringBootTest
public class UserInfoMapperTest {

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 测试 insert - 插入单个实体
     */
    @Test
    public void testInsert() {
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("测试用户_insert");
        entity.setAge(25);
        entity.setIsDelete(0);

        int result = userInfoMapper.insert(entity);
        assertTrue(result > 0);
        assertNotNull(entity.getId());
    }

    /**
     * 测试 insertSelective - 插入非null字段
     */
    @Test
    public void testInsertSelective() {
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("测试用户_insertSelective");
        entity.setAge(30);
        // dateTime 不设置，测试 selective

        int result = userInfoMapper.insertSelective(entity);
        assertTrue(result > 0);
    }

    /**
     * 测试 batchInsert - 批量插入
     */
    @Test
    public void testBatchInsert() {
        List<UserInfoDO> entities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("批量用户_" + i);
            entity.setAge(20 + i);
            entity.setIsDelete(0);
            entities.add(entity);
        }

        int result = userInfoMapper.batchInsert(entities);
        assertEquals(5, result);
    }

    /**
     * 测试 getById - 根据主键查询
     */
    @Test
    public void testGetById() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("getById测试");
        entity.setAge(28);
        userInfoMapper.insertSelective(entity);

        // 测试查询
        UserInfoDO result = userInfoMapper.getById(entity.getId());
        assertNotNull(result);
        assertEquals("getById测试", result.getUserName());
    }

    /**
     * 测试 existsById - 判断主键是否存在
     */
    @Test
    public void testExistsById() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("existsById测试");
        entity.setAge(28);
        userInfoMapper.insertSelective(entity);

        // 测试存在
        assertTrue(userInfoMapper.existsById(entity.getId()));

        // 测试不存在
        assertFalse(userInfoMapper.existsById(999999L));
    }

    /**
     * 测试 selectOne - 条件查询单条
     */
    @Test
    public void testSelectOne() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("selectOne测试");
        entity.setAge(35);
        userInfoMapper.insertSelective(entity);

        // 条件查询
        ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
        wrapper.eq(UserInfoDO::getUserName, "selectOne测试");

        UserInfoDO result = userInfoMapper.selectOne(wrapper);
        assertNotNull(result);
        assertEquals(35, result.getAge());
    }

    /**
     * 测试 list - 条件查询多条
     */
    @Test
    public void testList() {
        // 先插入几条数据
        //for (int i = 0; i < 3; i++) {
        //    UserInfoDO entity = new UserInfoDO();
        //    entity.setUserName("list测试_" + i);
        //    entity.setAge(40 + i);
        //    userInfoMapper.insertSelective(entity);
        //}

        // 条件查询
        ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
        wrapper.like(UserInfoDO::getUserName, "list测试_");
        wrapper.orderByDesc(UserInfoDO::getDateTime);
        List<UserInfoDO> result = userInfoMapper.list(wrapper);
        assertNotNull(result);
        assertTrue(result.size() >= 3);
    }

    /**
     * 测试 listByIds - 根据主键列表批量查询
     */
    @Test
    public void testListByIds() {
        // 先插入几条数据
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("listByIds测试_" + i);
            entity.setAge(45 + i);
            userInfoMapper.insertSelective(entity);
            ids.add(entity.getId());
        }

        // 批量查询
        List<UserInfoDO> result = userInfoMapper.listByIds(ids);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    /**
     * 测试 count - 统计数量
     */
    @Test
    public void testCount() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("count测试");
        entity.setAge(50);
        userInfoMapper.insertSelective(entity);

        // 统计
        ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
        wrapper.eq(UserInfoDO::getUserName, "count测试");

        long count = userInfoMapper.count(wrapper);
        assertTrue(count > 0);
    }

    /**
     * 测试 existsByCondition - 条件判断是否存在
     */
    @Test
    public void testExistsByCondition() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("existsByCondition测试");
        entity.setAge(55);
        userInfoMapper.insertSelective(entity);

        // 条件判断
        ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
        wrapper.eq(UserInfoDO::getUserName, "existsByCondition测试");

        assertTrue(userInfoMapper.existsByCondition(wrapper));
    }

    /**
     * 测试 update - 全字段更新
     */
    @Test
    public void testUpdate() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("update测试");
        entity.setAge(60);
        entity.setDateTime(LocalDateTime.now());
        userInfoMapper.insertSelective(entity);

        // 更新
        UserInfoDO updateEntity = new UserInfoDO();
        updateEntity.setId(entity.getId());
        updateEntity.setUserName("update测试_已更新");
        updateEntity.setAge(61);
        updateEntity.setDateTime(LocalDateTime.now());
        updateEntity.setIsDelete(0);

        int result = userInfoMapper.update(updateEntity);
        assertTrue(result > 0);

        // 验证
        UserInfoDO updated = userInfoMapper.getById(entity.getId());
        assertEquals("update测试_已更新", updated.getUserName());
        assertEquals(61, updated.getAge());
    }

    /**
     * 测试 updateSelective - 非null字段更新
     */
    @Test
    public void testUpdateSelective() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("updateSelective测试");
        entity.setAge(65);
        entity.setDateTime(LocalDateTime.now());
        userInfoMapper.insertSelective(entity);

        // 更新（只更新age）
        UserInfoDO updateEntity = new UserInfoDO();
        updateEntity.setId(entity.getId());
        updateEntity.setAge(66);
        // userName 不设置，测试 selective

        int result = userInfoMapper.updateSelective(updateEntity);
        assertTrue(result > 0);

        // 验证
        UserInfoDO updated = userInfoMapper.getById(entity.getId());
        assertEquals("updateSelective测试", updated.getUserName()); // 保持不变
        assertEquals(66, updated.getAge());
    }

    /**
     * 测试 batchUpdate - 批量更新
     */
    @Test
    public void testBatchUpdate() {
        // 先插入几条数据
        List<UserInfoDO> entities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("batchUpdate测试_" + i);
            entity.setAge(70 + i);
            entity.setDateTime(LocalDateTime.now());
            userInfoMapper.insertSelective(entity);
            entities.add(entity);
        }

        // 批量更新年龄
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).setAge(80 + i);
        }

        int result = userInfoMapper.batchUpdate(entities);
        assertEquals(3, result);
    }

    /**
     * 测试 updateByCondition - 根据条件更新
     */
    @Test
    public void testUpdateByCondition() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("updateByCondition测试");
        entity.setAge(85);
        entity.setDateTime(LocalDateTime.now());
        userInfoMapper.insertSelective(entity);

        // 条件更新
        ConditionWrapper<UserInfoDO> condition = new ConditionWrapper<>();
        condition.eq(UserInfoDO::getUserName, "updateByCondition测试");

        UserInfoDO updateEntity = new UserInfoDO();
        updateEntity.setAge(86);

        int result = userInfoMapper.updateByCondition(updateEntity, condition);
        assertTrue(result > 0);
    }

    /**
     * 测试 updateByConditionSelective - 根据条件更新非null字段
     */
    @Test
    public void testUpdateByConditionSelective() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("updateByConditionSelective测试");
        entity.setAge(90);
        entity.setDateTime(LocalDateTime.now());
        userInfoMapper.insertSelective(entity);

        // 条件更新
        ConditionWrapper<UserInfoDO> condition = new ConditionWrapper<>();
        condition.eq(UserInfoDO::getUserName, "updateByConditionSelective测试");

        UserInfoDO updateEntity = new UserInfoDO();
        updateEntity.setAge(91);

        int result = userInfoMapper.updateByConditionSelective(updateEntity, condition);
        assertTrue(result > 0);
    }

    /**
     * 测试 updateByConditionWithFields - 更新指定字段
     */
    @Test
    public void testUpdateByConditionWithFields() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("updateByConditionWithFields测试");
        entity.setAge(95);
        entity.setDateTime(LocalDateTime.now());
        userInfoMapper.insertSelective(entity);

        // 指定字段更新
        UpdateSpec<UserInfoDO> updateSpec = new UpdateSpec<>();
        updateSpec.set(UserInfoDO::getAge, 96);

        ConditionWrapper<UserInfoDO> condition = new ConditionWrapper<>();
        condition.eq(UserInfoDO::getUserName, "updateByConditionWithFields测试");

        int result = userInfoMapper.updateByConditionWithFields(updateSpec, condition);
        assertTrue(result > 0);
    }

    /**
     * 测试 lockById - 查询并加锁
     */
    @Test
    public void testLockById() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("lockById测试");
        entity.setAge(100);
        userInfoMapper.insertSelective(entity);

        // 查询加锁
        UserInfoDO result = userInfoMapper.lockById(entity.getId());
        assertNotNull(result);
        assertEquals("lockById测试", result.getUserName());
    }

    /**
     * 测试 logicDelete - 逻辑删除
     */
    @Test
    public void testLogicDelete() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("logicDelete测试");
        entity.setAge(105);
        userInfoMapper.insertSelective(entity);

        // 逻辑删除
        int result = userInfoMapper.logicDelete(entity.getId());
        assertTrue(result > 0);

        // 验证被逻辑删除（is_delete = 1）
        UserInfoDO deleted = userInfoMapper.getById(entity.getId());
        assertNull(deleted); // 逻辑删除后查不到
    }

    /**
     * 测试 batchLogicDelete - 批量逻辑删除
     */
    @Test
    public void testBatchLogicDelete() {
        // 先插入几条数据
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("batchLogicDelete测试_" + i);
            entity.setAge(110 + i);
            userInfoMapper.insertSelective(entity);
            ids.add(entity.getId());
        }

        // 批量逻辑删除
        int result = userInfoMapper.batchLogicDelete(ids);
        assertEquals(3, result);
    }

    /**
     * 测试 logicDeleteByCondition - 根据条件逻辑删除
     */
    @Test
    public void testLogicDeleteByCondition() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("logicDeleteByCondition测试");
        entity.setAge(115);
        userInfoMapper.insertSelective(entity);

        // 条件删除
        ConditionWrapper<UserInfoDO> condition = new ConditionWrapper<>();
        condition.eq(UserInfoDO::getUserName, "logicDeleteByCondition测试");

        int result = userInfoMapper.logicDeleteByCondition(condition);
        assertTrue(result > 0);
    }

    /**
     * 测试 delete - 物理删除
     */
    @Test
    public void testDelete() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("delete测试");
        entity.setAge(120);
        userInfoMapper.insertSelective(entity);

        // 物理删除
        int result = userInfoMapper.delete(entity.getId());
        assertTrue(result > 0);

        // 验证被删除
        UserInfoDO deleted = userInfoMapper.getById(entity.getId());
        assertNull(deleted);
    }

    /**
     * 测试 batchDelete - 批量物理删除
     */
    @Test
    public void testBatchDelete() {
        // 先插入几条数据
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("batchDelete测试_" + i);
            entity.setAge(125 + i);
            userInfoMapper.insertSelective(entity);
            ids.add(entity.getId());
        }

        // 批量物理删除
        int result = userInfoMapper.batchDelete(ids);
        assertEquals(3, result);
    }

    /**
     * 测试 deleteByCondition - 根据条件物理删除
     */
    @Test
    public void testDeleteByCondition() {
        // 先插入一条数据
        UserInfoDO entity = new UserInfoDO();
        entity.setUserName("deleteByCondition测试");
        entity.setAge(130);
        userInfoMapper.insertSelective(entity);

        // 条件删除
        ConditionWrapper<UserInfoDO> condition = new ConditionWrapper<>();
        condition.eq(UserInfoDO::getUserName, "deleteByCondition测试");

        int result = userInfoMapper.deleteByCondition(condition);
        assertTrue(result > 0);
    }

    /**
     * 测试 batchInsertSelective - 批量插入非null字段
     */
    @Test
    public void testBatchInsertSelective() {
        List<UserInfoDO> entities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("批量插入Selective_" + i);
            entity.setAge(135 + i);
            // dateTime 不设置，测试 selective
            entities.add(entity);
        }

        int result = userInfoMapper.batchInsertSelective(entities);
        assertEquals(3, result);
    }

    /**
     * 测试 batchUpdateSelective - 批量更新非null字段
     */
    @Test
    public void testBatchUpdateSelective() {
        // 先插入几条数据
        List<UserInfoDO> entities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfoDO entity = new UserInfoDO();
            entity.setUserName("batchUpdateSelective测试_" + i);
            entity.setAge(140 + i);
            entity.setDateTime(LocalDateTime.now());
            userInfoMapper.insertSelective(entity);
            entities.add(entity);
        }

        // 批量更新年龄（只更新age，userName不设置）
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).setAge(150 + i);
            entities.get(i).setUserName(null); // 设为null验证 selective
        }

        int result = userInfoMapper.batchUpdateSelective(entities);
        assertEquals(3, result);
    }
}