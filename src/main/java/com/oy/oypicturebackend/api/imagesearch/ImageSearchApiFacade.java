package com.oy.oypicturebackend.api.imagesearch;

import com.oy.oypicturebackend.api.imagesearch.model.ImageSearchResult;
import com.oy.oypicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.oy.oypicturebackend.api.imagesearch.sub.GetImageListApi;
import com.oy.oypicturebackend.api.imagesearch.sub.GetImagePageUrlAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {
    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlAPI.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2025-10-28_EvUKqQEh0oOqAO2U.webp";
        List<ImageSearchResult> imageSearchResults = searchImage(imageUrl);
        System.out.println("结果列表" + imageSearchResults);
    }
}
