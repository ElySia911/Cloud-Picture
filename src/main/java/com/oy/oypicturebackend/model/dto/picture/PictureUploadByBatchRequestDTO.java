package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequestDTO {
    /**
     * 抓取的关键词
     */
    private String searchText;

    /**
     * 抓取数量，默认10条
     */
    private Integer count = 10;

    /**
     * 批量导入图片时，指定的名称前缀，例如：汽车，则最终图片名称为汽车1 汽车2 汽车3...
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
