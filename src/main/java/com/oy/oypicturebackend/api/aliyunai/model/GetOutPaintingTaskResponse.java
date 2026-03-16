package com.oy.oypicturebackend.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询扩图任务结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOutPaintingTaskResponse implements Serializable {
    /**
     * 请求唯一标识
     */
    private String requestId;


    private Output output;

    /**
     * 输出的任务信息
     */
    @Data
    public static class Output {
        /**
         * 任务ID
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

        /**
         * 提交时间
         * YYYY-MM-DD HH:mm:ss
         */
        private String submitTime;

        /**
         * 开始时间
         */
        private String scheduledTime;

        /**
         * 完成时间
         */
        private String endTime;

        /**
         * 输出图像的Url
         */
        private String outputImageUrl;

        /**
         * 请求失败的错误码，请求成功时不会返回此参数
         */
        private String code;

        /**
         * 请求失败的错误信息，请求成功时不会返回此参数
         */
        private String message;

        private TaskMetrics taskMetrics;
    }

    /**
     * 任务统计信息
     */
    @Data
    public static class TaskMetrics {
        /**
         * 总任务数量
         */
        private Integer total;

        /**
         * 任务状态为成功的任务数。
         */
        private Integer succeeded;

        /**
         * 任务状态为失败的任务数。
         */
        private Integer failed;
    }


}
