package org.example.tujucloudbackend.manager.websocket.disruptor;

import lombok.Data;
import org.example.tujucloudbackend.manager.websocket.model.PictureEditRequestMessage;
import org.example.tujucloudbackend.model.entity.User;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {

    /**
    * 消息
    */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
    * 当前用户的 session
    */
    private WebSocketSession session;

    /**
    * 当前用户
    */
    private User user;

    /**
    * 图片 id
    */
    private Long pictureId;

}
