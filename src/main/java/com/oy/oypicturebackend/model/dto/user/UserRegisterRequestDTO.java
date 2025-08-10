package com.oy.oypicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求DTO，接收前端传过来的数据
 */
@Data
public class UserRegisterRequestDTO implements Serializable {
    //给每一个类定义一个唯一的序列化id，即使类的结构发生改变（如新增或删除字段），只要这个id不变，就能兼容旧数据，可以理解为 保证对象序列化和反序列化的一致性
    private static final long serialVersionUID = -733936723456754208L;
    //账号
    private String userAccount;
    //密码
    private String userPassword;
    //确认密码
    private String checkPassword;


}
