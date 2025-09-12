package com.oy.oypicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic门面类，管理项目中所有StpLogic账号体系
 * 添加@Component注解，目的是 让Spring自动扫描到该类，并创建Bean实例，确保DEFAULT和SPACE被初始化
 */
@Component
public class StpKit {
    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;


    /**
     * Space会话对象，管理Space表所有账号的登录，权限认证
     * StpKit.SPACE.CheckLogin(); // 检查用户是否登录
     * StpKit.SPACE.CheckPermissions();//检查用户是否有权限
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);// 空间会话对象，管理Space表所有账号的登录，权限认证


}
