package com.oy.oypicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequestDTO;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.oy.oypicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component//交给Spring管理，自动new一个
public class AliYunAiApi {

    //读取阿里云ai配置
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    //创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    //查询任务地址
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建拓图任务，返回类型是扩图任务响应类
     *
     * @param createOutPaintingTaskRequestDTO
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequestDTO createOutPaintingTaskRequestDTO) {
        if (createOutPaintingTaskRequestDTO == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        //发送请求，需要携带
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                //必须开启异步处理设置为enable
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                //使用Hutool工具类转换为json
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequestDTO));

        //处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            //异常
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败");
            }
            //正常就使用工具类转换为创建扩图任务响应
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            //继续判断，若返回的数据有状态码，就肯定是请求失败，这是官方规定的
            if (createOutPaintingTaskResponse.getCode() != null) {
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败" + errorMessage);
            }
            return createOutPaintingTaskResponse;
        }
    }


    /**
     * 根据任务Id查询创建的任务结果，返回类型是扩图任务结果类
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTaskResponse(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务Id为空");
        }
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        //发送查询结果的请求，需要携带Authorization和任务Id
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }


}
