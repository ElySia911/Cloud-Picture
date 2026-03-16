package com.oy.oypicturebackend.manager.auth;

import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import lombok.Data;

/**
 * 这个类不做任何的权限判断，它的职责是整合并承载权限判断所需的业务上下文数据，就是把请求涉及的资源信息整理好，交给权限系统使用
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
     * 空间用户 ID（记录的id）
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
