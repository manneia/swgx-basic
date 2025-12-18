package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 全量进项发票查询请求
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceFullQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作流水号
     */
    private String czlsh;

    /**
     * 纳税人识别号
     */
    private String nsrsbh;

    /**
     * 发票类型代码
     */
    private String fplxdm;

    /**
     * 发票状态
     */
    private String fpzt;

    /**
     * 发票代码
     */
    private String fpdm;

    /**
     * 发票号码
     */
    private String fphm;

    /**
     * 销方名称
     */
    private String xfmc;

    /**
     * 销方识别号
     */
    private String xfsh;

    /**
     * 开票日期起
     */
    private String kprqq;

    /**
     * 开票日期止
     */
    private String kprqz;

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
     * 采集时间起
     */
    private String cjsjq;

    /**
     * 采集时间止
     */
    private String cjsjz;

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
     * 抵押时间起
     */
    private String cysjq;

    /**
     * 抵押时间止
     */
    private String cysjz;

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
     * 报销时间起
     */
    private String bxsjq;

    /**
     * 报销时间止
     */
    private String bxsjz;

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
     * 签收时间起
     */
    private String qssjq;

    /**
     * 签收时间止
     */
    private String qssjz;

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
     * 记账时间起
     */
    private String jzsjq;

    /**
     * 记账时间止
     */
    private String jzsjz;

    /**
     * 勾选状态
     */
    private String gxzt;

    /**
     * 勾选类型
     */
    private String gxlx;

    /**
     * 勾选人
     */
    private String gxr;

    /**
     * 勾选时间起
     */
    private String gxsjq;

    /**
     * 勾选时间止
     */
    private String gxsjz;

    /**
     * 所属期起
     */
    private String skssq;

    /**
     * 认证用途
     */
    private String rzyt;

    /**
     * 认证人
     */
    private String rzr;

    /**
     * 认证时间起
     */
    private String rzsjq;

    /**
     * 认证时间止
     */
    private String rzsjz;

    /**
     * 匹配状态
     */
    private String pdzt;

    /**
     * 匹配操作人
     */
    private String pdczr;

    /**
     * 匹配操作日期起
     */
    private String pdczrqq;

    /**
     * 匹配操作日期止
     */
    private String pdczrqz;

    /**
     * 页码
     */
    private String pageNumber;

    /**
     * 每页大小
     */
    private String pageSize;


    @Override
    public String toString() {
        return "{" +
                "czlsh='" + czlsh + '\'' +
                ", nsrsbh='" + nsrsbh + '\'' +
                ", fplxdm='" + fplxdm + '\'' +
                ", fpzt='" + fpzt + '\'' +
                ", fpdm='" + fpdm + '\'' +
                ", fphm='" + fphm + '\'' +
                ", xfmc='" + xfmc + '\'' +
                ", xfsh='" + xfsh + '\'' +
                ", kprqq='" + kprqq + '\'' +
                ", kprqz='" + kprqz + '\'' +
                ", cjpc='" + cjpc + '\'' +
                ", cjzt='" + cjzt + '\'' +
                ", cjr='" + cjr + '\'' +
                ", cjsjq='" + cjsjq + '\'' +
                ", cjsjz='" + cjsjz + '\'' +
                ", cypc='" + cypc + '\'' +
                ", cyzt='" + cyzt + '\'' +
                ", cyr='" + cyr + '\'' +
                ", cysjq='" + cysjq + '\'' +
                ", cysjz='" + cysjz + '\'' +
                ", bxpc='" + bxpc + '\'' +
                ", bxzt='" + bxzt + '\'' +
                ", bxr='" + bxr + '\'' +
                ", bxsjq='" + bxsjq + '\'' +
                ", bxsjz='" + bxsjz + '\'' +
                ", qspc='" + qspc + '\'' +
                ", qszt='" + qszt + '\'' +
                ", qsr='" + qsr + '\'' +
                ", qssjq='" + qssjq + '\'' +
                ", qssjz='" + qssjz + '\'' +
                ", jzpc='" + jzpc + '\'' +
                ", jzzt='" + jzzt + '\'' +
                ", jzr='" + jzr + '\'' +
                ", jzsjq='" + jzsjq + '\'' +
                ", jzsjz='" + jzsjz + '\'' +
                ", gxzt='" + gxzt + '\'' +
                ", gxlx='" + gxlx + '\'' +
                ", gxr='" + gxr + '\'' +
                ", gxsjq='" + gxsjq + '\'' +
                ", gxsjz='" + gxsjz + '\'' +
                ", skssq='" + skssq + '\'' +
                ", rzyt='" + rzyt + '\'' +
                ", rzr='" + rzr + '\'' +
                ", rzsjq='" + rzsjq + '\'' +
                ", rzsjz='" + rzsjz + '\'' +
                ", pdzt='" + pdzt + '\'' +
                ", pdczr='" + pdczr + '\'' +
                ", pdczrqq='" + pdczrqq + '\'' +
                ", pdczrqz='" + pdczrqz + '\'' +
                ", pageNumber='" + pageNumber + '\'' +
                ", pageSize='" + pageSize + '\'' +
                '}';
    }
}
