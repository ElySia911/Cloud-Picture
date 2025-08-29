package com.oy.oypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.model.dto.space.SpaceAddRequestDTO;
import com.oy.oypicturebackend.model.dto.space.SpaceQueryRequestDTO;
import com.oy.oypicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ouziyang
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-08-14 16:41:21
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequestDTO
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequestDTO spaceAddRequestDTO, User loginUser);


    /**
     * 校验空间，add是布尔值，区分新增空间和修改空间两种场景，比如新增时必须填名称，修改时可以不填
     * 作用：检查数据是否符合业务规则
     *
     * @param space
     * @param add   CRUD中的“Create/Update”前置校验，新增（Create）或修改（Update）空间时，必须先校验数据合法性（比如名称不为空、容量符合级别限制），否则直接报错。没有这个校验，CRUD 操作可能写入非法数据
     */
    void validSpace(Space space, boolean add);


    /**
     * 获取空间包装类，即获取视图对象 SpaceVO是给前端返回的数据格式，脱敏
     *
     * @param space
     * @param request
     * @return CRUD 中“Read”的结果转换，查询（Read）数据库得到 Space 实体后，不能直接返回给前端（可能包含敏感字段，或格式不符合前端需求），这个方法负责把实体转成前端需要的 SpaceVO（视图对象），是 “查询” 流程的必要步骤
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 获取空间包装类（分页），将分页查询到的Space实体列表，批量转换为SpaceVO分页对象
     *
     * @param spacePage
     * @param request
     * @return CRUD 中“Read”的结果转换，查询（Read）数据库得到 Space 实体后，不能直接返回给前端（可能包含敏感字段，或格式不符合前端需求），这个方法负责把实体转成前端需要的 SpaceVO（视图对象），是 “查询” 流程的必要步骤
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);


    /**
     * 获取查询对象，根据查询条件DTO构建QueryWrapper
     * QueryWrapper是MP的查询条件构造器，用于拼接SQL条件
     * 作用：将前端查询参数转换为数据库查询条件
     *
     * @param spaceQueryRequestDTO
     * @return CRUD 中“Read”的条件构建，前端查询（Read）空间时，会传入各种筛选条件（如按用户 ID、空间名称查询），这个方法把这些条件转换成数据库能理解的查询语句（通过 QueryWrapper），是 “查询” 的前置条件。
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequestDTO spaceQueryRequestDTO);

    /**
     * 根据用户选择的空间级别，为这个空间自动填充最大容量和最大的图片数量
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

}
