package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片批量编辑请求DTO
 */
@Data
public class PictureEditByBatchRequestDTO implements Serializable {

    /**
     * 图片id列表
     * 要更新哪些图片，就传id进来，用列表收集起来
     */
    private List<Long> pictureIdList;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 分类
     */
    private String category;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 命令规则
     */
    private String nameRule;

    private static final long serialVersionUID = 1;
}
