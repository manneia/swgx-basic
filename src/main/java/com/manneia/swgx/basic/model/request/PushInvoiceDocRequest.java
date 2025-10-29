package com.manneia.swgx.basic.model.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author lkx
 * @description 悠乐芳推送开票单据生成开票链接请求参数
 * @created 2025-10-29 15:59:04
 */
@Data
public class PushInvoiceDocRequest {

    /**
     * 单据编号
     */
    @NotNull(message = "单据编号不能为空")
    private String docNo;

    /**
     * 发票类型代码
     */
    @NotNull(message = "发票类型代码不能为空")
    private String invoiceTypeCode;

    /**
     * 发票类型
     */
    @NotNull(message = "发票类型不能为空")
    private String invoiceType;

    /**
     * 销方名称
     */
    @NotNull(message = "销方名称不能为空")
    private String sellerName;

    /**
     * 销方税号
     */
    @NotNull(message = "销方税号不能为空")
    private String sellerTaxNo;

    /**
     * 销方地址
     */
    private String sellerAddress;

    /**
     * 销方电话
     */
    private String sellerTelPhone;

    /**
     * 销方开户银行
     */
    private String sellerBankName;

    /**
     * 销方银行账号
     */
    private String sellerBankNumber;

    /**
     * 销方开票人
     */
    @NotNull(message = "销方开票人不能为空")
    private String drawer;

    /**
     * 销方发票含税金额
     */
    private BigDecimal invoiceTotalPriceTax;

    /**
     * 销方发票不含税金额
     */
    private BigDecimal invoiceTotalPrice;

    /**
     * 销方发票税额
     */
    private BigDecimal invoiceTotalTax;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 登录账号
     */
    @NotNull(message = "登录账号不能为空")
    private String loginAccount;

    /**
     * 订单明细
     */
    @NotNull(message = "订单明细不能为空")
    @NotEmpty(message = "订单明细不能为空")
    private List<PushInvoiceDocDetail> invoiceDocDetails;
}
