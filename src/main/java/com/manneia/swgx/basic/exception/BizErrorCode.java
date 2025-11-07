package com.manneia.swgx.basic.exception;


import com.manneia.swgx.basic.common.ErrorCode;

/**
 * @author luokaixuan
 * @description com.manneia.basic.exception
 * @created 2025/5/12 21:31
 */
public enum BizErrorCode implements ErrorCode {

    DISCOUNT_GOODS_LIST_HANDLER_ERROR("discount_goods_list_handler_error", "折扣行商品处理失败"),

    GOODS_PRICE_NOT_NULL("GOODS_PRICE_NOT_NULL", "商品单价未维护"),

    /**
     * HTTP 客户端错误
     */
    HTTP_CLIENT_ERROR("HTTP_CLIENT_ERROR", "HTTP 客户端错误"),

    /**
     * HTTP 服务端错误
     */
    HTTP_SERVER_ERROR("HTTP_SERVER_ERROR", "HTTP 服务端错误"),

    /**
     * 不允许重复发送通知
     */
    SEND_NOTICE_DUPLICATED("SEND_NOTICE_DUPLICATED", "不允许重复发送通知"),

    /**
     * 通知保存失败
     */
    NOTICE_SAVE_FAILED("NOTICE_SAVE_FAILED", "通知保存失败"),

    /**
     * 状态机转换失败
     */
    STATE_MACHINE_TRANSITION_FAILED("STATE_MACHINE_TRANSITION_FAILED", "状态机转换失败"),

    /**
     * 重复请求
     */
    DUPLICATED("DUPLICATED", "重复请求"),

    /**
     * 远程调用返回结果为空
     */
    REMOTE_CALL_RESPONSE_IS_NULL("REMOTE_CALL_RESPONSE_IS_NULL", "远程调用返回结果为空"),

    /**
     * 远程调用返回结果失败
     */
    REMOTE_CALL_RESPONSE_IS_FAILED("REMOTE_CALL_RESPONSE_IS_FAILED", "远程调用返回结果失败");

    private final String code;

    private final String message;

    BizErrorCode(String code, String message) {
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
