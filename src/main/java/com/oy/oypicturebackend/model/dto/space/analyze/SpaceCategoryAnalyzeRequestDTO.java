package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对空间图片的 分类 进行分析的请求DTO
 */
@EqualsAndHashCode(callSuper = true)//callSuper=true时，生成的equals方法和hashCode方法同时包含父类的字段
@Data
public class SpaceCategoryAnalyzeRequestDTO extends SpaceAnalyzeRequestDTO {

}
