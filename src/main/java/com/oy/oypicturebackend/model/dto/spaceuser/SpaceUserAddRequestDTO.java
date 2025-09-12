package com.oy.oypicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间成员请求，给空间管理员使用
 */
@Data
public class SpaceUserAddRequestDTO implements Serializable {
    //空间id
    private Long spaceId;
    //用户id
    private Long userId;
    //用户在空间中的角色：viewer/editor/admin
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
