package com.oy.oypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserAddRequestDTO;
import com.oy.oypicturebackend.model.dto.spaceuser.SpaceUserQueryRequestDTO;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.SpaceRoleEnum;
import com.oy.oypicturebackend.model.vo.SpaceUserVO;
import com.oy.oypicturebackend.model.vo.SpaceVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.SpaceUserService;
import com.oy.oypicturebackend.mapper.SpaceUserMapper;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ouziyang
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-08-30 10:52:42
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    private UserService userService;
    @Resource
    @Lazy //延迟加载
    private SpaceService spaceService;

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequestDTO
     * @return
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequestDTO spaceUserAddRequestDTO) {
        ThrowUtils.throwIf(spaceUserAddRequestDTO == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequestDTO, spaceUser);
        validSpaceUser(spaceUser, true);
        //操作数据库
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        //创建空间成员时，空间id和用户id必填，要告诉后端在哪个空间添加哪个用户
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    /**
     * 获取空间成员信息，即获取视图对象（单条）
     *
     * @param spaceUser
     * @param request
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        //对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        //关联查询用户信息
        Long userId = spaceUser.getUserId();//通过空间成员记录的userId字段拿到这个人的id
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);//通过id查询到这个成员
            UserVO userVO = userService.getUserVO(user);//脱敏
            spaceUserVO.setUser(userVO);//把 脱敏后的成员信息设置进空间成员响应类的user字段
        }
        //关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);//通过id查询到这个空间
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);//脱敏
            spaceUserVO.setSpace(spaceVO);//把 脱敏后的空间信息设置进空间成员响应类的space字段
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList
     * @return
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        //判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        //对象列表=》封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)//对列表中每个元素都执行objToVo方法，最终返回一个新的列表
                .collect(Collectors.toList());

        //从 spaceUserList列表中收集需要关联查询的用户id和空间id，用Set存起来
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)//对列表中每个元素执行getUserId方法
                .collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)//对列表中每个元素执行getSpaceId方法
                .collect(Collectors.toSet());

        //调用listByIds方法，批量查询所有用户和所有空间信息，然后通过stream流，按照userId和spaceId进行分组为Map
        // 每个 key 对应的 value 是 “只有一个元素的列表”
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));

        //给每个 SpaceUserVO 填充用户详情和空间详情
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            //填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);//取key为userId的value列表的第一个元素（列表中只会有一个元素）
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            //填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }


    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequestDTO spaceUserQueryRequestDTO) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequestDTO == null) {
            return queryWrapper;
        }
        //从对象中取值
        Long id = spaceUserQueryRequestDTO.getId();
        Long spaceId = spaceUserQueryRequestDTO.getSpaceId();//空间id
        Long userId = spaceUserQueryRequestDTO.getUserId();//成员id
        String spaceRole = spaceUserQueryRequestDTO.getSpaceRole();//成员角色
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }
}




