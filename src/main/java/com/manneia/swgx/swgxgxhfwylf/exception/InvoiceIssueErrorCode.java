package com.manneia.swgx.basic.exception;

import com.manneia.baiwangbasic.common.ErrorCode;

/**
 * @author luokaixuan
 * @description com.manneia.baiwangbasic.exception
 * @created 2025/5/13 15:47
 */
@SuppressWarnings("unused")
public enum InvoiceIssueErrorCode implements ErrorCode {

    INVOICE_ISSUE_ERROR("INVOICE_ISSUE_ERROR", "发票开具失败"),
    INVOICE_ISSUE_INVOICE_TOTAL_PRICE_ERROR("", "发票价税合计与商品明细价税合计之和不相同"),
    INVOICE_ISSUE_DETAIL_INCLUDING_TAX_TOTAL_PRICE_ERROR("FLOW_LIST_DETAIL_TOTAL_PRICE_ERROR", "商品明细行含税金额与数量乘积与合计金额不一致"),
    INVOICE_ISSUE_DETAIL_EXCLUDING_TAX_TOTAL_PRICE_ERROR("FLOW_LIST_DETAIL_TOTAL_PRICE_ERROR", "商品明细行不含税金额与数量乘积与合计金额不一致"),
    INVOICE_ISSUE_DETAIL_GOODS_PRICE_ERROR("FLOW_LIST_DETAIL_GOODS_QUANTITY_ERROR", "商品明细单价必须为正数");
    private final String code;

    private final String message;

    InvoiceIssueErrorCode(String code, String message) {
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
