package org.example.tujucloudbackend.manager.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * @author : Yuan
 * @date :2025/9/24
 */
@Getter
public enum PictureEditActionEnum {

    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("旋转操作", "ROTATE_LEFT"),
    ROTATE_RIGHT("旋转操作", "ROTATE_RIGHT");

    private String text;
    private String value;

    PictureEditActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static PictureEditActionEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (PictureEditActionEnum valueEnum : PictureEditActionEnum.values()) {
            if (valueEnum.value.equals(value)) {
                return valueEnum;
            }
        }
        return null;
    }

}
