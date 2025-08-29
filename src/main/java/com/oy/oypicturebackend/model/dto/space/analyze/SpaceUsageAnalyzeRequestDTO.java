package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分析空间资源 使用 情况的请求DTO
 */
@EqualsAndHashCode(callSuper = true)//callSuper=true时，生成的equals方法和hashCode方法同时包含父类的字段
@Data
public class SpaceUsageAnalyzeRequestDTO extends SpaceAnalyzeRequestDTO {

}
