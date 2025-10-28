package com.manneia.swgxgxhfwylf.exception;

import com.manneia.swgxgxhfwylf.common.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author luokaixuan
 * @description 系统异常
 * @created 2025/5/12 21:33
 */
@Setter
@Getter
public class SystemException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SystemException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SystemException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SystemException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public SystemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                           ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

}
