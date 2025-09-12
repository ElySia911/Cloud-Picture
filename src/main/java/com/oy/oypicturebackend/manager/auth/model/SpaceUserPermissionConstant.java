package com.oy.oypicturebackend.manager.auth.model;

/**
 * 空间中的权限的常量，便于后续校验权限时使用
 * 把权限的key用常量来表示，避免写错字符串
 */
public interface SpaceUserPermissionConstant {

    /**
     * 空间用户管理权限
     */
    String SPACE_USER_MANAGE = "spaceUser:manage";

    /**
     * 图片查看权限
     */
    String PICTURE_VIEW = "picture:view";

    /**
     * 图片上传权限
     */
    String PICTURE_UPLOAD = "picture:upload";

    /**
     * 图片编辑权限
     */
    String PICTURE_EDIT = "picture:edit";

    /**
     * 图片删除权限
     */
    String PICTURE_DELETE = "picture:delete";
}
