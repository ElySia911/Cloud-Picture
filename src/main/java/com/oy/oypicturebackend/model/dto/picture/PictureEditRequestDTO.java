package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片修改请求DTO，一般情况下给普通用户使用，可‍修改的字段范围小于更新请求
 */
@Data
public class PictureEditRequestDTO implements Serializable {

    private Long id;

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
     * 标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
