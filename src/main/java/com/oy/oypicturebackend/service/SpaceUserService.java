package com.oy.oypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.model.dto.space.SpaceAddRequestDTO;
import com.oy.oypicturebackend.model.dto.space.SpaceQueryRequestDTO;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserAddRequestDTO;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserQueryRequestDTO;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.SpaceUserVO;
import com.oy.oypicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ouziyang
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-08-30 10:52:42
 */
public interface SpaceUserService extends IService<SpaceUser> {


    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequestDTO
     * @return
     */
    long addSpaceUser(SpaceUserAddRequestDTO spaceUserAddRequestDTO);


    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);


    /**
     * 获取空间成员包装类，即获取视图对象（单条）
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);


    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);


    /**
     * 获取查询对象，根据查询条件DTO构建QueryWrapper
     * QueryWrapper是MP的查询条件构造器，用于拼接SQL条件
     * 作用：将前端查询参数转换为数据库查询条件
     *
     * @param spaceUserQueryRequestDTO
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequestDTO spaceUserQueryRequestDTO);

}
