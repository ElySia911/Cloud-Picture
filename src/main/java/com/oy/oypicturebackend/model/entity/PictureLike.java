package com.oy.oypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 图片点赞记录表
 *
 * @TableName picture_like
 */
@TableName(value = "picture_like")
@Data
public class PictureLike implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 图片名称（冗余字段）
     */
    private String pictureName;

    /**
     * 点赞次数（默认1）
     */
    private Integer likeCount;

    /**
     * 是否删除（0-未删，1-已删）
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}