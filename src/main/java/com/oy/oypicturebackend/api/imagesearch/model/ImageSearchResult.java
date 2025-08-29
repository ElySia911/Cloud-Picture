package com.oy.oypicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * 图片搜素结果类，用于接收API的返回结果
 */
@Data
public class ImageSearchResult {
    /**
     * 缩略图地址
     */
    private String thumbUrl;
    /**
     * 来源地址
     */
    private String fromUrl;
}
