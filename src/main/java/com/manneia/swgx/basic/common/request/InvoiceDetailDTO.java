package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发票明细DTO
 *
 * @author lk
 */
@Setter
@Getter
public class InvoiceDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行号
     */
    private String rowNo;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 计量单位
     */
    private String uom;

    /**
     * 规格型号
     */
    private String model;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 税额
     */
    private BigDecimal tax;

    /**
     * 含税金额
     */
    private BigDecimal amountTax;

    /**
     * 税率
     */
    private String rate;
}

