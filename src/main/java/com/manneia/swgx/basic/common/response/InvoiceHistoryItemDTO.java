package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 往期勾选发票信息项
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceHistoryItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 序号
     */
    private Integer xh;

    /**
     * 数电发票号码
     */
    private String sdfphm;

    /**
     * 发票代码
     */
    private String fpdm;

    /**
     * 发票号码
     */
    private String fphm;

    /**
     * 开票日期
     */
    private String kprq;

    /**
     * 销售方识别号
     */
    private String xfsh;

    /**
     * 销售方名称
     */
    private String xfmc;

    /**
     * 金额
     */
    private BigDecimal je;

    /**
     * 税额
     */
    private BigDecimal se;

    /**
     * 抵扣税额
     */
    private BigDecimal dkse;

    /**
     * 勾选日期
     */
    private String gxrq;

    /**
     * 发票类型
     */
    private String fplx;

    /**
     * 发票类型代码
     */
    private String fplxdm;

    /**
     * 用途
     */
    private String yt;

    /**
     * 发票状态
     */
    private String fpzt;

    /**
     * 关联状态
     */
    private String glzt;

    /**
     * 数据来源
     */
    private String sjly;
}
