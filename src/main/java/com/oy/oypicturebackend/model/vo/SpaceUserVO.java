package com.oy.oypicturebackend.model.vo;

import com.oy.oypicturebackend.model.entity.SpaceUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间成员响应类
 */
@Data
public class SpaceUserVO implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 空间角色：view/editor/admin
     */
    private String spaceRole;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 用户信息
     */
    private UserVO user;
    /**
     * 空间信息
     */
    private SpaceVO space;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param spaceUserVo
     * @return
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVo) {
        if (spaceUserVo == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVo, spaceUser);
        return spaceUser;
    }

    /**
     * 对象转封装类
     *
     * @param spaceUser
     * @return
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVo = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        return spaceUserVo;
    }
}
