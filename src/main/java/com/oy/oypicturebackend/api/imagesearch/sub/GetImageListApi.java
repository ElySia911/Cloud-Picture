package com.oy.oypicturebackend.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.api.imagesearch.model.ImageSearchResult;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetImageListApi {
    /**
     * 获取具体的相似图片列表数据 （step 3）
     *
     * @param url
     * @return
     */
    public static List<ImageSearchResult> getImageList(String url) {
        try {
            //Hutool工具类发起Get请求，拿到响应，返回的是HttpResponse类型对象
            HttpResponse response = HttpUtil.createGet(url).execute();

            //获取响应状态码和响应体
            int statusCode = response.getStatus();
            String body = response.body();

            //处理响应
            if (statusCode == 200) {
                return processResponse(body);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }

    }


    /**
     * 处理接口响应内容
     *
     * @param responseBody 接口返回的JSON字符串
     * @return
     */
    private static List<ImageSearchResult> processResponse(String responseBody) {
        //Json字符串转成JsonObject对象
        JSONObject jsonObject = new JSONObject(responseBody);
        //校验顶层JSON是否包含"data"字段
        if (!jsonObject.containsKey("data")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        //提取"data"字段对应的JSONObject（因为data是一个嵌套的JSON对象）
        JSONObject data = jsonObject.getJSONObject("data");
        //校验data对象是否包含"list"字段，没有则抛异常
        if (!data.containsKey("list")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        //提取"list"字段对应的JSONArray（图片列表的数组数据）
        JSONArray list = data.getJSONArray("list");
        //使用Hutool的JSONUtil工具类，将JSON数组自动转换为ImageSearchResult类的对象列表（前提：ImageSearchResult类的字段名要和JSON数组中元素的key一一对应）
        return JSONUtil.toList(list, ImageSearchResult.class);
    }


    //测试上述方法
    public static void main(String[] args) {
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=4226792212727440227&sign=126f577d58ea42ce005c301769953981&tk=2eaf2&tpl_from=pc";
        List<ImageSearchResult> imageList = getImageList(url);
        System.out.println("搜索成功：" + imageList);
    }
}
