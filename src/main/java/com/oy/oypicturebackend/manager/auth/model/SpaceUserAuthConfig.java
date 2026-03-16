package com.oy.oypicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限配置加载器
 * 读取json配置文件，把里面写的权限和角色转换成Java对象
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    //权限列表，权限类，对应JSON文件中的第一个元素
    private List<SpaceUserPermission> permissions;

    //角色列表，角色类，对应JSON文件中第二个元素
    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}
