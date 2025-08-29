package com.oy.oypicturebackend.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建 扩图任务响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutPaintingTaskResponse implements Serializable {

    private Output output;


    /**
     * 任务输出信息
     */
    @Data
    public static class Output {
        /**
         * 任务id
         */
        private String taskId;

        /**
         * 任务状态
         * PENDING：任务排队中
         * RUNNING：任务处理中
         * SUCCEEDED：任务执行成功
         * FAILED：任务执行失败
         * CANCELED：任务取消成功
         * UNKNOWN：任务不存在或状态未知
         */
        private String taskStatus;
    }

    /**
     * 请求失败的错误码，请求成功时不会返回此参数
     */
    private String code;

    /**
     * 请求失败的详细信息，请求成功不会返回此参数
     */
    private String message;

    /**
     * 请求唯一标识
     */
    private String requestId;

}
