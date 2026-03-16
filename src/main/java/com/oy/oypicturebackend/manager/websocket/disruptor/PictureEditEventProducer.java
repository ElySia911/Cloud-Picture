package com.oy.oypicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.oy.oypicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.oy.oypicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 图片编辑事件 生产者
 */
@Slf4j
@Component
public class PictureEditEventProducer {
    @Resource
    Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    //往队列里发布事件
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession webSocketSession, User user, Long pictureId) {
        //获取Disruptor的缓冲区
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        //获取下一个可以放事件的槽位序号（原子操作）
        long next = ringBuffer.next();
        //根据序号获取预创建的空事件对象
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        //给空事件填充业务数据
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);//将前端发来的'图片编辑请求消息'填充给'图片编辑事件'
        pictureEditEvent.setSession(webSocketSession);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        ringBuffer.publish(next);//发布事件
    }

    //停机，作用是在服务关闭时，确保队列里的所有任务都处理完再关闭，避免任务丢失
    @PreDestroy //在服务关闭时调用
    public void close() {
        pictureEditEventDisruptor.shutdown();
    }

}
