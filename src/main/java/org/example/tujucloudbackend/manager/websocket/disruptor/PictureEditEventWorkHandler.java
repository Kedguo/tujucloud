package org.example.tujucloudbackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import org.example.tujucloudbackend.manager.websocket.PictureEditHandler;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditMessageTypeEnum;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditRequestMessage;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditResponseMessage;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * @author : Yuan
 * @date :2025/9/25
 */
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);

        // 调用对应的消息处理方法
        if (enumByValue != null) {
            switch (enumByValue) {
                case ENTER_EDIT:
                    pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                    break;
                case EDIT_ACTION:
                    pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                    break;
                case EXIT_EDIT:
                    pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                    break;
                default:
                    PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                    pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                    pictureEditResponseMessage.setMessage("消息类型错误");
                    pictureEditResponseMessage.setUser(userService.getUserVO(user));
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
            }
        }

    }
}
