package com.oy.oypicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.oy.oypicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.oy.oypicturebackend.manager.websocket.model.*;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    //WebSocket是多用户并发连接，用线程安全的ConcurrentHashMap可以避免出现多用户同时抢占图片的编辑权

    // 每张图片的编辑状态，key：pictureId  value：当前正在编辑的用户Id 。相当于 图片-编辑者的映射表。
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    //保存所有连接到具体某张图片的会话，key: pictureId, value: 用户会话集合。相当于 图片-观察者连接的映射表
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    //----------------------------------------------------------------------------------------------


    //建立连接成功之后要做的事情
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        //保存会话到集合中
        User user = (User) session.getAttributes().get("user");//getAttributes是获取WebSocket连接的“属性集合”
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());//如果pictureId对应的集合不存在，则创建一个空的集合
        pictureSessions.get(pictureId).add(session);
        //构造响应，发送某某某加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户%s加入了编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
        //将响应广播 给所有连接到该图片的会话
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    //收到客户端消息之后，根据消息类别执行不同的处理
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        //获取消息内容，将消息解析为PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);//getPayload是获取客户端发来的原始文本内容

        //获取当前会话的属性
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        //根据消息类型执行不同的处理（生产消息到Disruptor环形队列中）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    //处理加入编辑状态
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        //没有用户正在编辑该图片，才能进入编辑
        ////putIfAbsent方法，如果pictureId对应的value不存在，则将user.getId()赋值给pictureId对应的value，并返回null；如果pictureId对应的value已经存在，则返回pictureId对应的value
        Long editingUserId = pictureEditingUsers.putIfAbsent(pictureId, user.getId());
        if (editingUserId == null) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        } else {
            //如果已经有用户在编辑了，那么就将该用户广播给请求进行编辑的用户
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            //获取当前正在编辑的用户
            User editingUser = userService.getById(editingUserId);
            String message = String.format("用户%s正在编辑中", editingUser.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setUserVO(userService.getUserVO(editingUser));
            //单独广播给当前请求编辑的session
            ObjectMapper objectMapper = new ObjectMapper();//将对象转换为json字符串
            String str = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(str);//将JSON字符串包装成WebSocket的文本消息对象
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    //处理执行编辑的操作
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        //正在编辑的用户Id
        Long editingUserId = pictureEditingUsers.get(pictureId);
        //当前的编辑操作，例如：左旋 右旋 放大 缩小
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);

        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        //如果当前用户是正在编辑的用户，则执行编辑操作
        if (editingUserId != null && editingUserId.equals(user.getId())) {


            //构造响应，发送某某某执行了某某某操作的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
            //广播给除了当前客户端之外的其他会话，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    //处理退出编辑状态
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            //构造响应，发出退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }


    //前端连接关闭之后要做的事情
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");//断开连接的用户
        Long pictureId = (Long) attributes.get("pictureId");//该用户之前操作的图片Id
        //移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        //删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);//获取该图片下的所有会话集合
        if (sessionSet != null) {
            sessionSet.remove(session);//移除当前会话
            //如果该图片的集合下没有会话了，则从Map集合中移除这个图片id的key
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);

            }
        }

        //响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }


    //---------------------------------------------------------------------------------------------

    /**
     * 广播给该图片的所有会话（支持排除某个Session）
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession             被排除的会话
     * @throws Exception
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        //拿到当前连接到该图片的所有会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);

        if (CollUtil.isNotEmpty(sessionSet)) {

            ObjectMapper objectMapper = new ObjectMapper();//创建Jackson的JSON处理对象，负责Java对象和JSON的互相转换，即一个转换器
            SimpleModule module = new SimpleModule();//用于拓展转换器的序列化功能
            //给Long类型和long基本类型注册序列化器：将他们转成字符串，解决前端JS丢失精度的问题
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);//把自定义的序列化器进行注册

            //使用转换器将Java对象序列化成JSON字符串，因为WebSocket传输的是JSON字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            //将JSON字符串转换成WebSocket协议能识别的格式
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessionSet) {
                //排除掉自己的会话
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    //检查这个会话是否处于连接状态
                    session.sendMessage(textMessage);
                }
            }

        }
    }

    //全部广播
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
