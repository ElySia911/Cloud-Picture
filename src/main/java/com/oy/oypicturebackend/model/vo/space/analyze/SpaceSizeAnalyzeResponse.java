package com.oy.oypicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分析空间图片的大小响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {
    /**
     * 图片大小范围
     */
    private String sizeRange;
    /**
     * 图片数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
