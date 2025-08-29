package com.oy.oypicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分析空间中图片分类使用情况的响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {
    /**
     * 图片分类的名称
     */
    private String category;

    /**
     * 每个分类下的图片数量
     */
    private Long count;

    /**
     * 每个分类下的图片的总大小
     */
    private Long totalSize;

    private static final long serialVersionUID = 1L;
}
