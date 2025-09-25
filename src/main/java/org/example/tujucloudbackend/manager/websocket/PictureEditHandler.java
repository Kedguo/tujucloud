package org.example.tujucloudbackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.example.tujucloudbackend.manager.websocket.disruptor.PictureEditEventProducer;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditActionEnum;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditMessageTypeEnum;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditRequestMessage;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditResponseMessage;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Yuan
 * @date :2025/9/25
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {


    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final ConcurrentHashMap<Long, Long> userOperationPictureMap = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // 保存所有图片的编辑历史，key: pictureId, value: 编辑历史
    private final ConcurrentHashMap<Long, List<String>> userOperationPictureAllHistory = new ConcurrentHashMap<>();

    /**
     * 连接建立成功
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        if (user == null) {
            log.error("用户未登录，拒绝广播");
            throw new RuntimeException("用户未登录，拒绝广播");
        }
        if (ObjUtil.isEmpty(pictureId)) {
            log.error("图片ID为空，拒绝广播");
            throw new RuntimeException("图片ID为空，拒绝广播");
        }
        //保存在会话中
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        boolean isFirstConnection = pictureSessions.get(pictureId).size() == 1;
        if (isFirstConnection) {
            // 首次连接，清理历史记录
            userOperationPictureAllHistory.remove(pictureId);
            userOperationPictureMap.remove(pictureId);
        }
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format(" %s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));

        handleHistoryEditActionMessage(session, pictureId);
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);

    }


    /**
     * 进入编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        // 没有用户正在编辑该图片，才能进入编辑
        if (!userOperationPictureMap.containsKey(pictureId)) {
            // 设置用户正在编辑该图片
            userOperationPictureMap.put(pictureId, user.getId());
            // 构造响应，发送加入编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑操作
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        Long editingUserId = userOperationPictureMap.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s操作图片%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setEditAction(actionEnum.getValue());
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
            // 记录操作历史
            userOperationPictureAllHistory.putIfAbsent(pictureId, new ArrayList<>());
            userOperationPictureAllHistory.get(pictureId).add(pictureEditRequestMessage.getEditAction());
        }
    }


    /**
     * 退出编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        Long editingUserId = userOperationPictureMap.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            userOperationPictureMap.remove(pictureId);
        }
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        String message = String.format("%s退出编辑图片", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 执行历史的操作
     *
     * @param session
     * @param pictureId
     * @throws IOException
     */
    public void handleHistoryEditActionMessage(WebSocketSession session, Long pictureId) throws IOException {
        //其他用户刚加入就会执行之前所有的操作
        List<String> operationHistory = userOperationPictureAllHistory.get(pictureId);
        if (operationHistory != null) {
            // 向该用户发送所有历史操作
            for (String historyAction : operationHistory) {
                PictureEditResponseMessage historyMessage = new PictureEditResponseMessage();
                historyMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
                historyMessage.setMessage("历史操作同步");
                historyMessage.setEditAction(historyAction);
                // 可以设置一个特殊标识表示这是历史操作
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(historyMessage)));
            }
        }
    }


    /**
     * 关闭连接
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 从 Session 属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
                //todo 可以选择性清理历史记录和编辑状态
                //userOperationPictureAllHistory.remove(pictureId);
            }
        }
        // 通知其他用户，该用户已经离开编辑
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 广播给该图片的所有用户（支持排除掉某个 Session）
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> userSessions = pictureSessions.get(pictureId);
        if (ObjUtil.isNotEmpty(userSessions)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);

            //封装成WebSocket消息
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : userSessions) {
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播给该图片的所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

}
