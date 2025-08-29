package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间用户上传行为分析请求DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpaceUserAnalyzeRequestDTO extends SpaceAnalyzeRequestDTO{
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 时间维度：day/week/month
     */
    private String  timeDimension;

}
