package com.manneia.swgxgxhfwylf.common;

/**
 * @author luokaixuan
 * @description 错误码通用接口
 * @created 2025/5/12 21:30
 */
public interface ErrorCode {

    /**
     * 错误码
     *
     * @return 错误码
     */
    String getCode();

    /**
     * 错误信息
     *
     * @return 错误信息
     */
    String getMessage();
}
