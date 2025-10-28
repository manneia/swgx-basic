package com.manneia.swgxgxhfwylf.exception;

import com.manneia.baiwangbasic.common.ErrorCode;

/**
 * @author luokaixuan
 * @description com.manneia.baiwangbasic.exception
 * @created 2025/5/13 15:47
 */
@SuppressWarnings("unused")
public enum FlowListErrorCode implements ErrorCode {

    FLOW_LIST_UPLOAD_ERROR("FLOW_LIST_UPLOAD_ERROR", "流水单上传失败"),
    FLOW_LIST_PARAMS_NOT_ERROR("FLOW_LIST_PARAMS_NOT_ERROR", "流水单参数接收失败"),
    FLOW_LIST_INVOICE_TOTAL_PRICE_ERROR("FLOW_LIST_INVOICE_TOTAL_PRICE_ERROR", "流水单价税合计与明细价税合计不同"),
    FLOW_LIST_DETAIL_INCLUDING_TAX_TOTAL_PRICE_ERROR("FLOW_LIST_DETAIL_TOTAL_PRICE_ERROR", "商品明细行含税金额与数量乘积与合计金额不一致"),
    FLOW_LIST_DETAIL_EXCLUDING_TAX_TOTAL_PRICE_ERROR("FLOW_LIST_DETAIL_TOTAL_PRICE_ERROR", "商品明细行不含税金额与数量乘积与合计金额不一致"),
    FLOW_LIST_DETAIL_GOODS_PRICE_ERROR("FLOW_LIST_DETAIL_GOODS_QUANTITY_ERROR", "商品明细单价必须为正数"),
    ;
    private final String code;

    private final String message;

    FlowListErrorCode(String code, String message) {
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
