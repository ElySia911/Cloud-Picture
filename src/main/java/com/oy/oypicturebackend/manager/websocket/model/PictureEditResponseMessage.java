package com.oy.oypicturebackend.manager.websocket.model;

import com.oy.oypicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片编辑响应消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO userVO;

    /**
     * 图片的状态
     */
    private PictureEditState state;

    private List<String> actions;

}
