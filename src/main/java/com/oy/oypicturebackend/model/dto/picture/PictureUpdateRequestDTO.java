package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片更新请求DTO （管理员）
 */
@Data
public class PictureUpdateRequestDTO implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 图片名
     */
    private String name;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 分类
     */
    private String category;
    /**
     * 标签 List类型，方便前端传js数组
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;

}
