package com.oy.oypicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step 1）
 */
@Slf4j
public class GetImagePageUrlAPI {

    /**
     * 获取以图搜图页面地址，通过向百度发送POST请求，获取给定图片URL的相似图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        //image：https%3A%2F%2Foy-1372001294.cos.ap-guangzhou.myqcloud.com%2F%2Fpublic%2F1948825777511940098%2F2025-08-08_uQNN0gXzHOAwNKdR.jpg
        //tn：pc
        //from：pc
        //image_source：PC_UPLOAD_URL
        //sdkParams

        //1.准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        //获取当前时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            //发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .header("acs-token", RandomUtil.randomString(1))
                    .form(formData)
                    .timeout(5000)
                    .execute();

            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            //解析响应，例如下面这个就是上传指定图片后接口返回的响应结果
            //{"status":0,"msg":"Success","data":{"url":"https://graph.baidu.com/s?card_key=\u0026entrance=GENERAL\u0026extUiData%5BisLogoShow%5D=1\u0026f=all\u0026isLogoShow=1\u0026session_id=13913994419617641622\u0026sign=12619a2e5f06a6e99f87101755623554\u0026tpl_from=pc","sign":"12619a2e5f06a6e99f87101755623554"}}
            String body = httpResponse.body();//获取响应体（JSON字符串）
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);//讲JSON字符串转为Java的Map

            //处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            //提取响应体中data.url字段，并对Url进行解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            //如果Url为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的接口地址");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        //测试上述方法
        String imageUrl = "https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/public/1948825777511940098/2025-08-12_elVAMykYVW6NL3SL.jpg";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("以图搜图成功，结果：" + searchResultUrl);
    }
}
