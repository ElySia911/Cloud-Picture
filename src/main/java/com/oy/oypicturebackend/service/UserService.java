package com.oy.oypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oy.oypicturebackend.model.dto.user.UserQueryRequestDTO;
import com.oy.oypicturebackend.model.dto.user.UserUpdateMySelfRequestDTO;
import com.oy.oypicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.vo.LoginUserVO;
import com.oy.oypicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author ouziyang
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-07-25 16:34:58
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request
     * @return 脱敏后的信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录的用户，用于service间传输
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息，即从User（未脱敏）转成LoginUserVO（脱敏）
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获得脱敏后的信息，即从User（未脱敏）转成UserVO（脱敏），这个service是提供给用户根据id获取其他单个用户信息的
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表，即从User列表（未脱敏）转成UserVO列表（脱敏），这个service是提供给管理员根据id获取多个用户信息
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserListVO(List<User> userList);

    /**
     * 获取查询条件，把对java象转换成mybatis-plus需要的QueryWrapper
     * 根据前端传入的查询条件，动态构造一个QueryWrapper<User>用于查询用户表 user
     * 返回值是 QueryWrapper<User>：MyBatis-Plus提供的查询构造器，封装了SQL的WHERE和ORDER BY等条件
     *
     * @param userQueryRequestDTO
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequestDTO userQueryRequestDTO);


    /**
     * 密码加密
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 当前登录用户更新自己个人信息
     *
     * @param userUpdateMySelfRequestDTO
     * @param request
     * @return
     */
    boolean updateMySelf(UserUpdateMySelfRequestDTO userUpdateMySelfRequestDTO, HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
