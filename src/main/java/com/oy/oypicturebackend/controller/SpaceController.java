package com.oy.oypicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.DeleteRequest;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.manager.auth.SpaceUserAuthManager;
import com.oy.oypicturebackend.model.dto.space.*;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.SpaceLevelEnum;
import com.oy.oypicturebackend.model.vo.SpaceVO;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {
    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 新建私人空间
     *
     * @param spaceAddRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequestDTO spaceAddRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequestDTO, loginUser);
        return ResultUtils.success(newId);
    }


    /**
     * 删除空间，管理员或者空间作者可删
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前登录用户信息，用于后续权限校验
        User loginUser = userService.getLoginUser(request);
        //从删除请求中提取出要删除的空间id
        Long id = deleteRequest.getId();

        //判断是否存在，根据id查询数据库把这条记录查出来，用oldSpace表示，若不存在这个空间则抛出未找到异常
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        //仅本人或管理员可删除
        spaceService.checkSpaceAuth(loginUser, oldSpace);
        //操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 更新空间（管理员）
     *
     * @param spaceUpdateRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequestDTO spaceUpdateRequestDTO, HttpServletRequest request) {
        if (spaceUpdateRequestDTO == null || spaceUpdateRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //用属性拷贝方法将DTO转换成实体，属性拷贝会将类型相同，字段相同的拷贝过去
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequestDTO, space);

        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);

        //对space进行数据校验，检查核心字段是否符合要求
        spaceService.validSpace(space, false);

        long id = spaceUpdateRequestDTO.getId();
        //根据id查询数据库有没有这个空间，用oldPicture来表示
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        //操作数据库，需要传入Space实体对象
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取空间（管理员，未脱敏）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Space space = spaceService.getById(id);
        //判断查出来的space是不是空
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //返回
        return ResultUtils.success(space);

    }

    /**
     * 根据id获取空间（封装类），用户可用
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        User loginUser = userService.getLoginUser(request);
        //根据空间和登录用户拿到权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);

        //接口要返回VO类型的数据，这里进行转换
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }


    /**
     * 分页获取空间列表（仅管理员可用）
     * 接收前端的查询条件，通过分页方式从数据库获取空间数据并返回
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequestDTO spaceQueryRequestDTO) {
        //提取当前页
        long current = spaceQueryRequestDTO.getCurrent();
        //提取每页记录数
        long size = spaceQueryRequestDTO.getPageSize();

        //创建分页参数对象，指定当前页码和每页记录数
        Page<Space> pageParam = new Page<>(current, size);

        //生成查询条件
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(spaceQueryRequestDTO);

        // 调用page方法，传入分页参数和查询条件，进行查询
        Page<Space> spacePage = spaceService.page(pageParam, queryWrapper);

        return ResultUtils.success(spacePage);
    }


    /**
     * 分页获取空间列表（封装类，脱敏，用户可用）
     *
     * @param spaceQueryRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequestDTO spaceQueryRequestDTO, HttpServletRequest request) {
        long current = spaceQueryRequestDTO.getCurrent();
        long pageSize = spaceQueryRequestDTO.getPageSize();
        //限制爬虫，防止用户一次查询20个空间
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

        //构造分页参数
        Page<Space> pageParam = new Page<>(current, pageSize);

        //创建查询条件包装器
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(spaceQueryRequestDTO);

        //查询数据库
        Page<Space> spacePage = spaceService.page(pageParam, queryWrapper);

        //脱敏获取封装类
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage, request);

        return ResultUtils.success(spaceVOPage);
    }


    /**
     * 编辑（更新）空间 （用户使用）
     *
     * @param spaceEditRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequestDTO spaceEditRequestDTO, HttpServletRequest request) {
        //校验前端发过来请求是否为空 或者请求里面的id是否为空，如果id为空，数据库不知道编辑哪一个空间
        if (spaceEditRequestDTO == null || spaceEditRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //操作数据库需要使用Space实体类，new一个，然后属性拷贝，拷贝完不要忘记自动填充
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequestDTO, space);
        spaceService.fillSpaceBySpaceLevel(space);

        //编辑时间要同步更新，这里是更新数据库的editTime字段，不是updateTime字段，updateTime字段是数据库记录发生改变时候由数据库来更新
        space.setEditTime(new Date());

        //校验数据，false代表是编辑空间，不是新增空间
        spaceService.validSpace(space, false);

        //获取当前登录用户的信息
        User loginUser = userService.getLoginUser(request);

        //获取要修改的空间的id
        long id = spaceEditRequestDTO.getId();

        //根据id查询数据库有没有这种空间
        Space oldSpace = spaceService.getById(id);

        //校验，若为空，提示请求数据不存在
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        //校验身份，仅空间创作者和管理员可编辑更新
        spaceService.checkSpaceAuth(loginUser, oldSpace);

        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 获取所有的空间级别列表，便于前端展示
     * 说白了就是将定义的空间级别信息整理成前端能直接展示的格式并返回
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                //values()方法会把枚举里的所有值拿出来，变成一个数组，然后把数组转换成流（可用理解为一个便于操作的序列）
                //map()对流里的每个元素做转换，这里是对枚举对象转换成SpaceLevel对象
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}


