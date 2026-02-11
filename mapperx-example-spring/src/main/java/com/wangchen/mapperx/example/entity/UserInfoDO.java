package com.wangchen.mapperx.example.entity;


import com.wangchen.mapperx.core.annotation.Column;
import com.wangchen.mapperx.core.annotation.IdStrategy;
import com.wangchen.mapperx.core.annotation.LogicDelete;
import com.wangchen.mapperx.core.annotation.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserInfoDO
 *
 * @author chenwang
 * @date 2026/1/16 19:43
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDO {

    @PrimaryKey(strategy = IdStrategy.CUSTOM,generator = "orderNoGenerator")
    private Long id;

    @Column(value = "name")
    private String userName;

    private Integer age;
    @LogicDelete
    private Integer isDelete;
}
