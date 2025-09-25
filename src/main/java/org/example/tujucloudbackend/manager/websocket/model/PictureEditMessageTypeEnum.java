package org.example.tujucloudbackend.manager.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * @author : Yuan
 * @date :2025/9/24
 */
@Getter
public enum PictureEditMessageTypeEnum {

    // 原有枚举值...
    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private String text;
    private String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (PictureEditMessageTypeEnum valueEnum : PictureEditMessageTypeEnum.values()) {
            if (valueEnum.value.equals(value)) {
                return valueEnum;
            }
        }
        return null;
    }

}
