/*
package com.oy.oypicturebackend.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageColorUtil {


    public static String getImageMainColor(String imageUrl) {

        String requestUrl = imageUrl + "?imageAve";

        try {
            HttpResponse response = HttpRequest.get(requestUrl).timeout(5000).execute();
            if (response.isOk()) {
                return response.body();
            } else {
                return "请求失败，状态码：" + response.getStatus() + ",URL:" + requestUrl;
            }
        } catch (Exception e) {
            return "请求异常，URL：" + requestUrl + "，异常信息：" + e.getMessage();
        }
    }


    public static Map<String, String> batchGetImageMainColor(List<String> imageUrls) {
        Map<String, String> resultMap = new HashMap<>();

        if (imageUrls == null || imageUrls.isEmpty()) {
            return resultMap;
        }


        for (String url : imageUrls) {
            String colorJson = getImageMainColor(url);
            resultMap.put(url, colorJson);
        }
        return resultMap;
    }


    public static void main(String[] args) {
        List<String> imageUrls = new ArrayList<>();

        imageUrls.add("https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2026-01-23_q0mgfirp7TRt3wKv.webp");
        imageUrls.add("https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2026-01-23_5BcV9MI1XXgCjRfu.webp");
        imageUrls.add("https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2026-01-23_4eErh4waVVboM8RH.webp");
        imageUrls.add("https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2026-01-23_Fl2piHeBP4pY2dAE.webp");


        Map<String, String> colorResultMap = batchGetImageMainColor(imageUrls);


        for (Map.Entry<String, String> entry : colorResultMap.entrySet()) {
            System.out.println("原始URL：" + entry.getKey());
            System.out.println("主色调JSON：" + entry.getValue());
            System.out.println("----------------------------------------");
        }

    }
}
*/
