package com.oy.oypicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求DTO
 */
@Data
public class UserLoginRequestDTO implements Serializable {


    private static final long serialVersionUID = 6358409135056508400L;
    //账号
    private String userAccount;
    //密码
    private String userPassword;
}
