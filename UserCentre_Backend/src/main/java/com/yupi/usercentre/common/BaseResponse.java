package com.yupi.usercentre.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 通用返回类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;

    private T data;

    private String message;
    private String description;

    // 全部传递
    public BaseResponse(int code,T data,String message,String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    // 不传任何信息
    public BaseResponse(int code, T data) {
        this(code,data,"","");
    }

    /**
     * 只传错误信息，无详细描述
     * @param code
     * @param data
     * @param message
     */
    public BaseResponse(int code, T data, String message){
        this(code,data,message,"");
    }


    /**
     * 全部错误信息
     * @param errorCode
     */
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage(),errorCode.getDescription());
    }
}
