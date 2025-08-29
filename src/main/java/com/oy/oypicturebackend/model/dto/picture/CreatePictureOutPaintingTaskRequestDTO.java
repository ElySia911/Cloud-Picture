package com.oy.oypicturebackend.model.dto.picture;

import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequestDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建AI拓图任务请求类，用于接收前端传来的参数并传递给Serivice服务层
 */
@Data
public class CreatePictureOutPaintingTaskRequestDTO implements Serializable {
    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 拓图参数
     */
    private CreateOutPaintingTaskRequestDTO.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
