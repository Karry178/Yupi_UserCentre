package com.yupi.usercentre.common;

/**
 * 统一返回结果类
 */
public class ResultUtils {

    /**
     * 封装成功返回的状态码信息
     * @param data
     * @return BaseResponse，自定义快捷键rusc输出
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"");
    }


    /**
     * 封装错误返回的状态码信息
     * @param errorCode
     * @return BaseResponse，自定义快捷键输出
     */
    public static BaseResponse error(ErrorCode errorCode){
        // errorCode已在BaseResponse类中定义好，直接调用
        return new BaseResponse<>(errorCode);
    }


    /**
     * 封装自定义的状态码
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(int code,String message,String description){
        return new BaseResponse(code,null,message,description);
    }


    /**
     * 封装错误返回的状态码信息与详细错误信息,修改message输出
     * @param errorCode
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String message,String description){
        // errorCode已在BaseResponse类中定义好，直接调用
        return new BaseResponse(errorCode.getCode(),null,message,description);
    }


    /**
     * 封装错误返回的状态码信息与详细错误信息，不改message输出
     * @param errorCode
     * @param description
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String description){
        // errorCode已在BaseResponse类中定义好，直接调用
        return new BaseResponse(errorCode.getCode(),errorCode.getMessage(),description);
    }
}
