package org.example.tujucloudbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author : Yuan
 * @date :2025/9/18
 * 空间类型枚举
 */
@Getter
public enum SpaceTypeEnum {

    PRIVATE(0, "私有空间"),
    TEAM(1, "团队空间");

    private final Integer value;
    private final String text;

    SpaceTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }

        for (SpaceTypeEnum valueEnum : SpaceTypeEnum.values()) {
            if (valueEnum.value.equals(value)) {
                return valueEnum;
            }
        }
        return null;
    }
}
