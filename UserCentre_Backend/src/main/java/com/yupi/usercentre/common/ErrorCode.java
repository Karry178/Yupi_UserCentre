package com.yupi.usercentre.common;

/**
 * 错误码
 */
public enum ErrorCode {


    SUCCESS(0, "ok", ""),
    // 枚举一些特定的错误码
    PARAM_ERROR(40000, "参数错误", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NO_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无访问权限", ""),
    FORBIDDEN(40301, "禁止操作", ""),
    SYSTEM_ERROR(50000, "系统内部异常", "");


    private final int code;
    /**
     * 状态码信息
     */
    private final String message;
    // 对错误码的详细描述
    private final String description;


    /**
     * ErrorCode的构造方法，并且枚举值不支持set，只能get
     * @param code
     * @param message
     * @param description
     */
    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
