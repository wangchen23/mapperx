package com.wangchen.mapperx.example.mapper;

import com.wangchen.mapperx.core.api.BaseMapperRepository;
import com.wangchen.mapperx.example.entity.UserInfoDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserInfoMapper
 *
 * @author chenwang
 **/
@Mapper
public interface UserInfoMapper extends BaseMapperRepository<UserInfoDO, Long> {
}
