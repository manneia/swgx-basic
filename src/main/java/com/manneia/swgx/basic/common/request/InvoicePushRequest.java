package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发票推送请求参数
 *
 * @author lk
 */
@Setter
@Getter
public class InvoicePushRequest extends BaseRequest {

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
     * 状态
     */
    private String state;

    /**
     * 开票日期
     */
    private String invoiceDate;

    /**
     * 旧发票代码
     */
    private String oldInvoiceCode;

    /**
     * 旧发票号码
     */
    private String oldInvoiceNum;

    /**
     * 总税额
     */
    private BigDecimal totalTax;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 含税总金额
     */
    private BigDecimal totalAmountTax;

    /**
     * 大写含税总金额
     */
    private String bigTotalAmountTax;

    /**
     * 购买方纳税人名称
     */
    private String purchaseTaxPayer;

    /**
     * 购买方纳税人识别号
     */
    private String purchaseTaxPayerNo;

    /**
     * 购买方地址电话
     */
    private String purchaseAddrTel;

    /**
     * 购买方银行账号
     */
    private String purchaseBankAccount;

    /**
     * 销售方纳税人名称
     */
    private String salesTaxPayer;

    /**
     * 销售方纳税人识别号
     */
    private String salesTaxPayerNo;

    /**
     * 销售方地址电话
     */
    private String salesAddrTel;

    /**
     * 销售方银行账号
     */
    private String salesBankAccount;

    /**
     * 备注
     */
    private String memo;

    /**
     * 复核人
     */
    private String checker;

    /**
     * 收款人
     */
    private String payee;

    /**
     * 开票人
     */
    private String drawer;

    /**
     * 校验码
     */
    private String checkCode;

    /**
     * 输入来源
     */
    private String inputSource;

    /**
     * 机器税号
     */
    private String machineTaxNo;

    private String deductible;

    private String deductibleDate;

    /**
     * 文件URL
     */
    private String cileUrl;

    /**
     * 发票状态
     */
    private String invoiceStatus;

    /**
     * 是否为电子发票
     */
    private String isElectronic;

    /**
     * 密文
     */
    private String ciphertext;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 发票明细列表
     */
    private List<InvoiceDetailDTO> detail;
}

