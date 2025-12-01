package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 发票状态变更请求参数
 *
 * <pre>
 * {
 *   "InvoiceType": "",
 *   "InvoiceCode": "",
 *   "InvoiceNum": "",
 *   "InvoiceDate": "",
 *   "PurchaseTaxPayer": "",
 *   "PurchaseTaxPayerNo": "",
 *   "SalesTaxPayer": "",
 *   "SalesTaxPayerNo": "",
 *   "TotalAmountTax": "",
 *   "State": "",
 *   "Deductible": "",
 *   "DeductibleDate": ""
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceStateChangeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 发票类型
     */
    private String invoiceType;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNum;

    /**
     * 开票日期（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     */
    private String invoiceDate;

    /**
     * 购方名称
     */
    private String purchaseTaxPayer;

    /**
     * 购方纳税人识别号
     */
    private String purchaseTaxPayerNo;

    /**
     * 销方名称
     */
    private String salesTaxPayer;

    /**
     * 销方纳税人识别号
     */
    private String salesTaxPayerNo;

    /**
     * 价税合计
     */
    private BigDecimal totalAmountTax;

    /**
     * 状态
     */
    private String state;

    /**
     * 勾选状态
     */
    private String deductible;

    /**
     * 勾选时间（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     */
    private String deductibleDate;

    /**
     * 入账状态
     */
    private String PostState;
}


