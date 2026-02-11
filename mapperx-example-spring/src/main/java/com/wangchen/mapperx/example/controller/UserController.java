package com.wangchen.mapperx.example.controller;

import com.github.pagehelper.PageHelper;
import com.wangchen.mapperx.core.conditions.ConditionWrapper;
import com.wangchen.mapperx.example.entity.UserInfoDO;
import com.wangchen.mapperx.example.mapper.UserInfoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * UserController
 *
 * @author chenwang
 * @date 2026/2/10 17:10
 **/
@RestController
public class UserController {

    @Resource
    private UserInfoMapper userInfoMapper;

    @GetMapping("/user")
    public String test() {
        UserInfoDO userInfoDO = new UserInfoDO();
        userInfoDO.setUserName("张三");
        userInfoDO.setAge(90);
        int inserted = userInfoMapper.insertSelective(userInfoDO);
        return inserted + "+";
    }
    
    @GetMapping("/get")
    public Object get(@RequestParam Integer pageNum,@RequestParam Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        ConditionWrapper<UserInfoDO> wrapper = new ConditionWrapper<>();
        wrapper.eq(UserInfoDO::getAge,90);
        List<UserInfoDO> list = userInfoMapper.list(wrapper);
        return list;
    }
}
