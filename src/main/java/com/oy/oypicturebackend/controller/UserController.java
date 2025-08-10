package com.oy.oypicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.DeleteRequest;
import com.oy.oypicturebackend.common.OssUtil;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.model.dto.user.*;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.LoginUserVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.UserService;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    @Autowired
    private OssUtil ossUtil;

    /**
     * 用户注册
     *
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        ThrowUtils.throwIf(userRegisterRequestDTO == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegisterRequestDTO.getUserAccount();
        String userPassword = userRegisterRequestDTO.getUserPassword();
        String checkPassword = userRegisterRequestDTO.getCheckPassword();
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);

        return ResultUtils.success(result);
    }


    /**
     * 用户登录
     *
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequestDTO userLoginRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequestDTO == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequestDTO.getUserAccount();
        String userPassword = userLoginRequestDTO.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 获取当前登录用户
     *
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);//脱敏
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 用户注销
     *
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     *
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequestDTO userAddRequestDTO) {
        ThrowUtils.throwIf(userAddRequestDTO == null, ErrorCode.PARAMS_ERROR);//判断是否为空，若是空就返回参数错误
        User user = new User();
        BeanUtils.copyProperties(userAddRequestDTO, user);//属性拷贝
        //默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);//加密
        user.setUserPassword(encryptPassword);
        //插入数据库，调用save方法将封装好的user对象插进去
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户（仅管理员）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类，脱敏
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> response = getUserById(id);//调用上面那个方法
        User user = response.getData();//从统一响应结果类中获取User实体
        return ResultUtils.success(userService.getUserVO(user));//调用getUserVO获取脱敏的用户信息，然后返回封装的UserVO对象
    }


    /**
     * 删除用户 需要管理员权限才能删用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {//判断删除请求是否为空 或者 要删除的记录的id是否小于等于
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户 需要管理员权限
     *
     * @param userUpdateRequestDTO
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequestDTO userUpdateRequestDTO) {
        if (userUpdateRequestDTO == null || userUpdateRequestDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequestDTO, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 分页查询获取用户列表，脱敏（仅管理员）
     * 返回值类型是一个 分页的类型，这个分页对象是mybatis-plus提供的包装类，分页里面存储的是脱敏的用户信息UserVO
     *
     * @param userQueryRequestDTO
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequestDTO userQueryRequestDTO) {
        ThrowUtils.throwIf(userQueryRequestDTO == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequestDTO.getCurrent();//当前页码
        long pageSize = userQueryRequestDTO.getPageSize();//每页记录数
        //分页查询User原始数据
        Page<User> userPage = userService.page(//使用mybatis-plus提供的分页查询接口page
                new Page<>(current, pageSize),//创建分页对象
                userService.getQueryWrapper(userQueryRequestDTO));//getQueryWrapper方法返回一个封装好的查询条件
        //脱敏
        //构造一个Page<UserVO>对象，传入总记录数，页码，每页记录数
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        /*userPage.getRecords() 获取当前页的所有 User 实体数据，返回一个列表，但这些数据是未脱敏的
         * 然后使用getUserListVO方法，将未脱敏的数据传进去进行脱敏，这个方法接收的参数类型是未脱敏的用户信息列表*/
        List<UserVO> userListVO = userService.getUserListVO(userPage.getRecords());
        //将已经脱敏的用户信息列表设置到分页结果中，setRecords方法返回的是Page类型
        userVOPage.setRecords(userListVO);
        return ResultUtils.success(userVOPage);

    }

    /**
     * 当前登录用户更新自己个人信息
     *
     * @param userUpdateMySelfRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMySelf(@RequestBody UserUpdateMySelfRequestDTO userUpdateMySelfRequestDTO, HttpServletRequest request) {
        //判断
        ThrowUtils.throwIf(userUpdateMySelfRequestDTO == null, ErrorCode.PARAMS_ERROR);

        boolean result = userService.updateMySelf(userUpdateMySelfRequestDTO, request);
        return ResultUtils.success(result);
    }

    //上传头像
    @PostMapping("/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {

        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "头像为空");
        String url = ossUtil.uploadFile(file);
        return ResultUtils.success(url);
    }

}

