package com.manneia.swgx.basic.exception;


import com.manneia.swgx.basic.common.ErrorCode;

/**
 * @author luokaixuan
 * @description 错误码
 * @created 2025/5/12 21:34
 */
public enum RepoErrorCode implements ErrorCode {

    /**
     * 未知错误
     */
    UNKNOWN_ERROR("UNKNOWN_ERROR", "未知错误"),

    /**
     * 数据库查询失败
     */
    SELECT_FAILED("SELECT_FAILED", "数据库查询失败"),

    /**
     * 数据库插入失败
     */
    INSERT_FAILED("INSERT_FAILED", "数据库插入失败"),

    /**
     * 数据库更新失败
     */
    UPDATE_FAILED("UPDATE_FAILED", "数据库更新失败"),

    /**
     * 数据库删除失败
     */
    DELETE_FAILED("DELETE_FAILED", "数据库删除失败");

    private final String code;

    private final String message;

    RepoErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
