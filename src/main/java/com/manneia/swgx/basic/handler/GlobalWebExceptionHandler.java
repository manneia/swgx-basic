package com.manneia.swgx.basic.handler;

import com.google.common.collect.Maps;
import com.manneia.swgx.basic.common.ErrorCode;
import com.manneia.swgx.basic.exception.BizException;
import com.manneia.swgx.basic.exception.SystemException;
import com.manneia.swgx.basic.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

import static com.manneia.swgx.basic.common.response.ResponseCode.SYSTEM_ERROR;


/**
 * @author luokaixuan
 * @description 异常拦截器
 * @created 2025/5/12 21:23
 */
@ControllerAdvice
@Slf4j
public class GlobalWebExceptionHandler {

    /**
     * 自定义方法参数校验异常处理器
     *
     * @param ex 异常
     *
     * @return 返回异常map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException occurred.", ex);
        Map<String, String> errors = Maps.newHashMapWithExpectedSize(1);
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    /**
     * 自定义业务异常处理器
     *
     * @param bizException 业务异常
     *
     * @return 返回错误结果
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Result<?> exceptionHandler(BizException bizException) {
        log.error("bizException occurred.", bizException);
        return getExceptionResult(bizException.getErrorCode(), bizException.getMessage());
    }

    /**
     * 自定义系统异常处理器
     *
     * @param systemException 系统异常
     *
     * @return 返回异常结果
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Result<?> systemExceptionHandler(SystemException systemException) {
        log.error("systemException occurred.", systemException);
        return getExceptionResult(systemException.getErrorCode(), systemException.getMessage());
    }

    /**
     * 自定义系统异常处理器
     *
     * @param throwable 异常
     *
     * @return 返回异常结果
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Result<?> throwableHandler(Throwable throwable) {
        log.error("throwable occurred.", throwable);
        Result<?> result = new Result<>();
        result.setCode(SYSTEM_ERROR.name());
        result.setMessage("哎呀，当前网络比较拥挤，请您稍后再试~");
        result.setSuccess(false);
        return result;
    }

    private Result<?> getExceptionResult(ErrorCode errorCode, String message) {
        Result<?> result = new Result<>();
        result.setCode(errorCode.getCode());
        if (message == null) {
            result.setMessage(errorCode.getMessage());
        } else {
            result.setMessage(message);
        }
        result.setSuccess(false);
        return result;
    }
}
