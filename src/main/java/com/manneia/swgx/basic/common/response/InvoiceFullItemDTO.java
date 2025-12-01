package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 全量进项发票信息
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceFullItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发票ID
     */
    private String fpid;

    /**
     * 发票类型名称
     */
    private String fplxMc;

    /**
     * 发票类型代码
     */
    private String fplxdm;

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
     * 发票状态
     */
    private String fpzt;

    /**
     * 发票状态描述
     */
    private String fpztms;

    /**
     * 销方名称
     */
    private String xfmc;

    /**
     * 销方识别号
     */
    private String xfsh;

    /**
     * 购方名称
     */
    private String gfmc;

    /**
     * 购方识别号
     */
    private String gfsh;

    /**
     * 销方地址电话
     */
    private String xfdzdh;

    /**
     * 销方银行账号
     */
    private String xfyhzh;

    /**
     * 购方地址电话
     */
    private String gfdzdh;

    /**
     * 购方银行账号
     */
    private String gfyhzh;

    /**
     * 校验码
     */
    private String jym;

    /**
     * 合计金额
     */
    private BigDecimal hjje;

    /**
     * 合计税额
     */
    private BigDecimal hjse;

    /**
     * 价税合计
     */
    private BigDecimal jshj;

    /**
     * 密码区
     */
    private String mmq;

    /**
     * 备注
     */
    private String bz;

    /**
     * 开票人
     */
    private String kpr;

    /**
     * 收款人
     */
    private String skr;

    /**
     * 复核人
     */
    private String fhr;

    /**
     * 机器编号
     */
    private String jqbh;

    /**
     * 机动号码
     */
    private String jdhm;

    /**
     * 认证状态
     */
    private String rzzt;

    /**
     * 认证日期
     */
    private String rzrq;

    /**
     * 下载URL
     */
    private String dlurl;

    /**
     * PDF文件URL
     */
    private String pdfurl;

    /**
     * OFD文件URL
     */
    private String ofdurl;

    /**
     * XML文件URL
     */
    private String xmlurl;

    /**
     * 有效税额
     */
    private BigDecimal yxse;

    /**
     * 风险等级
     */
    private String fxdj;

    /**
     * 优惠政策代标志
     */
    private String yhzsdbz;

    /**
     * 采集批次
     */
    private String cjpc;

    /**
     * 采集状态
     */
    private String cjzt;

    /**
     * 采集人
     */
    private String cjr;

    /**
     * 采集时间
     */
    private String cjsj;

    /**
     * 抵押批次
     */
    private String cypc;

    /**
     * 抵押状态
     */
    private String cyzt;

    /**
     * 抵押人
     */
    private String cyr;

    /**
     * 抵押时间
     */
    private String cysj;

    /**
     * 报销批次
     */
    private String bxpc;

    /**
     * 报销状态
     */
    private String bxzt;

    /**
     * 报销人
     */
    private String bxr;

    /**
     * 报销时间
     */
    private String bxsj;

    /**
     * 签收批次
     */
    private String qspc;

    /**
     * 签收状态
     */
    private String qszt;

    /**
     * 签收人
     */
    private String qsr;

    /**
     * 签收时间
     */
    private String qssj;

    /**
     * 记账批次
     */
    private String jzpc;

    /**
     * 记账状态
     */
    private String jzzt;

    /**
     * 记账人
     */
    private String jzr;

    /**
     * 记账时间
     */
    private String jzsj;

    /**
     * 匹配批次
     */
    private String pdpc;

    /**
     * 匹配状态
     */
    private String pdzt;

    /**
     * 匹配人
     */
    private String pdr;

    /**
     * 匹配时间
     */
    private String pdsj;

    /**
     * 匹配操作人
     */
    private String pdczr;

    /**
     * 匹配操作日期
     */
    private String pdczrq;

    /**
     * 剩余抵扣金额
     */
    private BigDecimal syppje;

    /**
     * 剩余抵扣税额
     */
    private BigDecimal syppse;

    /**
     * 勾选批次
     */
    private String gxpc;

    /**
     * 勾选状态
     */
    private String gxzt;

    /**
     * 勾选类型
     */
    private String gxlx;

    /**
     * 勾选方式
     */
    private String gxfs;

    /**
     * 勾选人
     */
    private String gxr;

    /**
     * 勾选时间
     */
    private String gxsj;

    /**
     * 认证批次
     */
    private String rzpc;

    /**
     * 入账用途
     */
    private String rzyt;

    /**
     * 认证人
     */
    private String rzr;

    /**
     * 认证时间
     */
    private String rzsj;

    /**
     * 发票明细列表
     */
    private List<InvoiceItemDTO> fpmxList;

    /**
     * 销方地址
     */
    private String xfdz;

    /**
     * 销方电话
     */
    private String xfdh;

    /**
     * 销方银行
     */
    private String xfyh;

    /**
     * 销方账号
     */
    private String xfzh;

    /**
     * 组织机构代码
     */
    private String zzjgdm;

    /**
     * 税务机关名称
     */
    private String swjgmc;

    /**
     * 税务机关代码
     */
    private String swjgdm;

    /**
     * 车辆类型
     */
    private String cllx;

    /**
     * 厂牌型号
     */
    private String cpxh;

    /**
     * 产地
     */
    private String cd;

    /**
     * 合格证书
     */
    private String hgzs;

    /**
     * 进口证明书号
     */
    private String jkzmsh;

    /**
     * 商检单号
     */
    private String sjdh;

    /**
     * 发动机号码
     */
    private String fdjhm;

    /**
     * 车架号码
     */
    private String cjhm;

    /**
     * 税率
     */
    private String slv;

    /**
     * 完税凭证号码
     */
    private String wspzhm;

    /**
     * 吨位
     */
    private String dw;

    /**
     * 限乘人数
     */
    private String xcrs;

    /**
     * 勾选所期
     */
    private String gxsq;

    /**
     * 扩展字段1
     */
    private String kz1;

    /**
     * 扩展字段2
     */
    private String kz2;

    /**
     * 扩展字段3
     */
    private String kz3;
}
