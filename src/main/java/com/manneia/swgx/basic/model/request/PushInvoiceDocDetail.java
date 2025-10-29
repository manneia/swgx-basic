package com.manneia.swgx.basic.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author lkx
 * @description 发票单据明细
 * @created 2025-10-29 16:07:42
 */
@Data
public class PushInvoiceDocDetail {

    /**
     * 序号
     */
    private Integer lineNumber;

    /**
     * 商品自定义编码
     */
    @NotNull(message = "商品自定义编码不能为空")
    private String goodsPersonalCode;

    /**
     * 商品数量
     */
    private BigDecimal goodsQuantity;

    /**
     * 商品含税金额
     */
    private BigDecimal goodsTotalPriceTax;

    /**
     * 商品不含税金额
     */
    private BigDecimal goodsTotalPrice;

    /**
     * 商品税额
     */
    private BigDecimal goodsTotalTax;

    /**
     * 商品折扣类型
     */
    @NotNull(message = "商品折扣类型不能为空")
    private String goodsDiscountType;
}
