package com.manneia.swgx.basic.exception;


import com.manneia.swgx.basic.common.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author luokaixuan
 * @description 业务异常
 * @created 2025/5/12 21:32
 */
@Setter
@Getter
public class BizException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BizException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BizException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                        ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

}
