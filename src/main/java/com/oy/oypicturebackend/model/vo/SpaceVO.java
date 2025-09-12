package com.oy.oypicturebackend.model.vo;

import com.oy.oypicturebackend.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 返回给前端的视图包装类，关联了创建空间的用户信息
 */
@Data
public class SpaceVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私人 1-团队
     */
    private Integer spaceType;

    /**
     * 空间图片的最大总大小（空间总容量）
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量（空间最多可以存多少张图片）
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小（空间目前使用了多少容量）
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量（空间目前有多少张图片）
     */
    private Long totalCount;


    /**
     * 创建用户 id
     */
    private Long userId;


    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 创建用户信息（脱敏）
     */
    private UserVO user;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    private static final long serialVersionUID = 1L;


    //为了让实体类Space和视图类SpaceVO之间更好转换，编写如下两个方法

    /**
     * 封装类转对象 ，即VO转实体
     *
     * @param spaceVO
     * @return
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }


    /**
     * 对象转封装类，即实体转VO
     *
     * @param space
     * @return
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }


}
