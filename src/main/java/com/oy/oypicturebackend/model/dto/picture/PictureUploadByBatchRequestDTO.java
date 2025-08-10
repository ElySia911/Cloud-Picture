package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequestDTO {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量，默认10条
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
