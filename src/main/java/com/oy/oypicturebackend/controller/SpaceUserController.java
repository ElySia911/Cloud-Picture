package com.oy.oypicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.DeleteRequest;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserAddRequestDTO;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserEditRequestDTO;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserQueryRequestDTO;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.SpaceUserVO;
import com.oy.oypicturebackend.service.SpaceUserService;
import com.oy.oypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequestDTO spaceUserAddRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequestDTO == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequestDTO);
        return ResultUtils.success(id);
    }

    /**
     * 从空间删除成员
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        //根据删除请求中的id，判断空间成员表是否存在这条记录
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库，移除这条记录
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);

    }

    /**
     * 查询某个成员在某个空间中的信息
     *
     * @param spaceUserQueryRequestDTO
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequestDTO spaceUserQueryRequestDTO) {
        //参数校验，确保spaceId和userId必填，因为这个接口是查询某个空间的某个成员
        ThrowUtils.throwIf(spaceUserQueryRequestDTO == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequestDTO.getSpaceId();//哪一个空间
        Long userId = spaceUserQueryRequestDTO.getUserId();//空间中的哪一个人
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);

        //构造查询条件之后，查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequestDTO));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     *
     * @param spaceUserQueryRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequestDTO spaceUserQueryRequestDTO, HttpServletRequest request) {
        //这里只判断请求是否为空，而不对请求中的字段进行判断是否为空，是因为这个接口是查询空间中的成员信息，并不是只查一个人的信息
        ThrowUtils.throwIf(spaceUserQueryRequestDTO == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequestDTO));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 编辑空间成员的信息
     *
     * @param spaceUserEditRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequestDTO spaceUserEditRequestDTO, HttpServletRequest request) {
        if (spaceUserEditRequestDTO == null || spaceUserEditRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将DTO转成实体类
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequestDTO, spaceUser);
        //数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        //判断表中是否存在这条记录
        long id = spaceUserEditRequestDTO.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        //根据request查出当前登录的用户
        User loginUser = userService.getLoginUser(request);
        //构建查询请求
        SpaceUserQueryRequestDTO spaceUserQueryRequestDTO = new SpaceUserQueryRequestDTO();
        spaceUserQueryRequestDTO.setUserId(loginUser.getId());
        //构造查询条件 sql：select * from space_user where userId= ?
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequestDTO);
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);

        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);

    }

}
