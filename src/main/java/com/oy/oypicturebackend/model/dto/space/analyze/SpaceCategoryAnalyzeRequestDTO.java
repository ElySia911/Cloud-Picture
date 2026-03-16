package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对空间中的图片的分类进行分析的请求
 */
@EqualsAndHashCode(callSuper = true)//callSuper=true时，生成的equals方法和hashCode方法同时包含父类的字段
@Data
public class SpaceCategoryAnalyzeRequestDTO extends SpaceAnalyzeRequestDTO {

}
