package com.oy.oypicturebackend.manager.websocket.disruptor;

import com.oy.oypicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.oy.oypicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑的事件 充当上下文容器，可以当作是被生产者和消费者使用的
 */
@Data
public class PictureEditEvent {
    //消息
    private PictureEditRequestMessage pictureEditRequestMessage;
    //当前用户的session
    private WebSocketSession session;
    //当前用户
    private User user;
    //图片Id
    private Long pictureId;

}
