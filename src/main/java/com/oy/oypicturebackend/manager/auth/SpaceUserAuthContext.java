package com.oy.oypicturebackend.manager.auth;

import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import lombok.Data;

/**
 * SpaceUserAuthContext（空间用户授权上下文）
 * 表示用户在特定空间内的授权上下文，包括关联的图片、空间和用户信息。
 */
@Data
public class SpaceUserAuthContext {

    /**
     * 临时参数，不同请求对应的 id 可能不同
     */
    private Long id;

    /**
     * 图片 ID
     */
    private Long pictureId;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 空间用户 ID
     */
    private Long spaceUserId;

    /**
     * 图片信息对象
     */
    private Picture picture;

    /**
     * 空间信息对象
     */
    private Space space;

    /**
     * 空间用户信息对象
     */
    private SpaceUser spaceUser;
}
