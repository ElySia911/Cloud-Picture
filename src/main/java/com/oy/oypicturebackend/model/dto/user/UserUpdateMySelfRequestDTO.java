package com.oy.oypicturebackend.model.dto.user;

import lombok.Data;

@Data
public class UserUpdateMySelfRequestDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户头像（URL）
     */
    private String userAvatar;
}
