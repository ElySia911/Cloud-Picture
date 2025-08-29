package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 按照颜色搜索图片请求
 */
@Data
public class SearchPictureByColorRequestDTO implements Serializable {
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
