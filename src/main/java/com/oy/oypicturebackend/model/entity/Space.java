package com.oy.oypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 空间
 *
 * @TableName space
 */
@TableName(value = "space")
@Data
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 空间类型：0-私有 1-团队
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
     * 是否删除
     */
    @TableLogic//逻辑删除的注解
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}