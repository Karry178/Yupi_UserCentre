package com.yupi.usercentre.exception;

import com.yupi.usercentre.common.ErrorCode;

// 自定义业务异常类
public class BusinessException extends RuntimeException{

    // RuntimeException函数未封装code和description参数，所以需要自定义封装
    private int code;

    // 作用：final 让字段在对象构造完成后不可再被重新赋值（只读）。必须在声明处或每个构造器里完成一次性赋值。
    private final String description;

    /**
     * 把message构造给父级函数，因为父级函数RuntimeException有message参数，直接super调用即可
     * @param message
     * @param code
     * @param description
     */
    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }


    /**
     * 第一段：参数类型已是父类需要的类型（String）→ 直接传。
     * 第二段：参数类型是业务对象（ErrorCode）→ 需要先从对象中提取父类需要的字段（message）再传。
     * @param errorCode
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // errorCode已封装message参数，直接调用
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }


    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage()); // errorCode已封装message参数，直接调用
        this.code = errorCode.getCode();
        this.description = description;
    }


    // Getter
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
