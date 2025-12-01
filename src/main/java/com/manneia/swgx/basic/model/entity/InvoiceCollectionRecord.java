package com.manneia.swgx.basic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票采集记录（基础信息）
 *
 * @author lk
 */
@Getter
@Setter
@TableName("invoice_collection_record")
public class InvoiceCollectionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发票类型代码
     */
    private String invoiceType;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    private String zpfphm;

    /**
     * 开票日期
     */
    private LocalDateTime invoiceDate;

    /**
     * 购方名称
     */
    private String buyerName;

    /**
     * 购方纳税识别号
     */
    private String buyerTaxNo;

    /**
     * 销方名称
     */
    private String sellerName;

    /**
     * 销方纳税识别号
     */
    private String sellerTaxNo;

    /**
     * 价税合计
     */
    private BigDecimal totalAmountTax;

    /**
     * 发票状态
     */
    private String invoiceStatus;

    /**
     * 勾选状态
     */
    private String checkStatus;

    /**
     * 入账状态
     * 01-未入账
     * 02-已入账（企业所得税提前扣除）
     * 03-已入账（企业所得税不扣除）
     * 06-入账撤销
     */
    private String entryStatus;

    /**
     * 勾选日期
     */
    private LocalDate checkDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


