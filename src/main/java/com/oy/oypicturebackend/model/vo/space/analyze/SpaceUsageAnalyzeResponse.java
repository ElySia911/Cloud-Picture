package com.oy.oypicturebackend.model.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 分析空间资源使用情况的响应（返回给前端的字段）
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    /**
     * 空间容量已使用大小
     */
    private Long usedSize;

    /**
     * 空间容量总大小
     */
    private Long maxSize;

    /**
     * 空间容量使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 空间当前图片数量
     */
    private Long userCount;

    /**
     * 空间最大图片数量
     */
    private Long maxCount;

    /**
     * 空间图片数量使用比例
     */
    private Double countUsageRatio;

    private static final long serialVersionUID = 1L;

}
