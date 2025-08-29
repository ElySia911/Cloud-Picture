package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间排名分析请求DTO（管理员）
 */
@Data
public class SpaceRankAnalyzeRequestDTO implements Serializable {
    /**
     * 排名前N的空间，默认为10
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
