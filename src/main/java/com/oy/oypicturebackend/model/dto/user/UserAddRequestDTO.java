package com.oy.oypicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求DTO，管理员用的
 */
@Data
public class UserAddRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userName;//用户昵称

    private String userAccount;//账号

    private String userAvatar;//用户头像

    private String userProfile;//用户简介

    private String userRole;//用户角色 ：user admin


}
