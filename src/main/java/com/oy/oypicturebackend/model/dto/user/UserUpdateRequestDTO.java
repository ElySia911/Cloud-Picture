package com.oy.oypicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求DTO
 */
@Data
public class UserUpdateRequestDTO implements Serializable {
    private Long id;
    private String userName;
    private String userAvatar;//头像
    private String userProfile;//简介
    private String userRole;//角色：user/admin
}
