package com.oy.oypicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限类
 * 定义系统有哪些操作权限，比如 查看图片 上传图片 修改图片 删除图片 管理成员
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}
