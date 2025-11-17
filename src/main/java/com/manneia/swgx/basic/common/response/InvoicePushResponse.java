package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 发票推送接口响应
 *
 * @author lk
 */
@Getter
@Setter
public class InvoicePushResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应代码
     */
    private String code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 错误信息（如果有）
     */
    private String errorMsg;
}

