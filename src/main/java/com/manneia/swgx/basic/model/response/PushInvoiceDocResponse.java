package com.manneia.swgx.basic.model.response;

import lombok.Data;

/**
 * @author lkx
 * @description
 * @created 2025-10-29 16:31:17
 */
@Data
public class PushInvoiceDocResponse {

    /**
     * 单据编号
     */
    private String docNo;

    /**
     * 开票链接生成是否成功
     */
    private boolean success;

    /**
     * 开票链接
     */
    private String issueUrl;

    /**
     * 错误信息
     */
    private String errorMessage;
}
