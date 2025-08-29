package com.oy.oypicturebackend.model.dto.space;

import lombok.Data;

/**
 * 空间更新请求（管理员），可以修改空间级别和限额
 */
@Data
public class SpaceUpdateRequestDTO {
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
     * 空间图片的最大总大小（空间总容量）
     */
    private Long maxSize;
    /**
     * 空间图片的最大数量（空间最多可以存多少张图片）
     */
    private Long maxCount;

    private static final long serialVersionUID = 1L;
}
