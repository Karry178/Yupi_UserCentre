package com.yupi.usercentre.exception;

import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局异常捕获拦截器
@Slf4j
@RestControllerAdvice  // (AOP功能)切面功能(可以在执行代码前后做额外处理)，拦截所有Controller异常
public class GlobalExceptionHandler {

    // 此方法只去捕获BusinessException中的异常
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e){
        // 打日志
        log.error("businessException" + e.getMessage(), e);
        return ResultUtils.error(e.getCode(),e.getMessage(),e.getDescription());
    }

    // 此方法用于捕获所有的RuntimeException异常
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e){
        // 打日志
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,e.getMessage(),"");
    }
}
