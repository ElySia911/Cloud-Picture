package com.oy.oypicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserRole;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.SpaceRoleEnum;
import com.oy.oypicturebackend.model.enums.SpaceTypeEnum;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.service.SpaceUserService;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间成员权限管理器
 * 加载配置文件到对象，并提供根据角色获取权限列表的方法
 */
@Component
public class SpaceUserAuthManager {
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;


    //用来接收配置文件转成对象后的数据
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        //使用Hutool工具类的方法加载配置文件
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        //将json字符串转换为对象，这个对象是 SpaceUserAuthConfig类型
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取这个角色的权限列表
     *
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        //找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles()//getRoles()方法返回的是SpaceUserRole类型的List
                .stream()//将列表转换成流的方式
                .filter(r -> spaceUserRole.equals(r.getKey()))//判断当前角色有没有和列表中的其中一个匹配
                .findFirst()//找到第一个符合条件的元素
                .orElse(null);//如果没有找到，则返回null
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();//返回这个角色所掌握的权限列表

    }


    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        String userRole = loginUser.getUserRole();
        //管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        //公共图库
        if (space == null) {
            //判断当前用户是不是管理员
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            //todo 这里需要完善，用户不是管理员，但是图片的作者，要返回对应的权限
            //return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);//查看权限
            return new ArrayList<>();

        }
        //不是公共图库，就判断是私人空间还是团队空间
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        //根据空间的类别获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                //私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                //团队空间，查询SpaceUser并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())//where spaceId = space.getId() and userId = loginUser.getId()
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }
}
