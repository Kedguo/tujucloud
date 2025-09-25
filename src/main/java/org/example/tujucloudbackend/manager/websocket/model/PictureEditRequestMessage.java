package org.example.tujucloudbackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : Yuan
 * @date :2025/9/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    private static final long serialVersionUID = 7661248005243497526L;

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作（放大、缩小）
     */
    private String editAction;

}
