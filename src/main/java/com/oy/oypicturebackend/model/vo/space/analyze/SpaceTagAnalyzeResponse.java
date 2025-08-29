package com.oy.oypicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分析空间中图片标签使用情况的响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {
    /**
     * 标签名称
     */
    private String tag;
    /**
     * 使用次数
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
