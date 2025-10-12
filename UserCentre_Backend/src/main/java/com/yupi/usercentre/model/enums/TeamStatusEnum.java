package com.yupi.usercentre.model.enums;

/**
 * 队伍状态枚举类
 */
public enum TeamStatusEnum {

    // 2.创建枚举类，定义枚举值
    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    // 1.写枚举的构造函数
    private int value;

    private String text;

    // 2.写枚举类方法
    public static TeamStatusEnum getEnumByValue(Integer value){
        // 如果枚举值为null，则返回null
        if (value == null) {
            return null;
        }
        // 遍历枚举类，找到对应的枚举值
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values){
            if (teamStatusEnum.getValue() == value){
                return teamStatusEnum;
            }
        }
        return null;
    }

    TeamStatusEnum(int value,String text){
        this.value = value;
        this.text = text;
    }

    // 3.写getter和setter方法
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
