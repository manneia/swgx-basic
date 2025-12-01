package com.manneia.swgx.basic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推送记录实体类
 *
 * @author lk
 */
@Getter
@Setter
@TableName("push_record")
public class PushRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 校验码
     */
    private String jym;

    /**
     * 开票日期
     */
    private String kprq;

    /**
     * 购买方名称
     */
    private String gfmc;

    /**
     * 购买方税号
     */
    private String gfsh;

    /**
     * 购买方银行账号
     */
    private String gfyhzh;

    /**
     * 购买方地址电话
     */
    private String gfdzdh;

    /**
     * 销售方名称
     */
    private String xfmc;

    /**
     * 销售方税号
     */
    private String xfsh;

    /**
     * 销售方银行账号
     */
    private String xfyhzh;

    /**
     * 销售方地址电话
     */
    private String xfdzdh;

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
     * 备注
     */
    private String bz;

    /**
     * 开票人
     */
    private String kpr;

    /**
     * 复核人
     */
    private String fhr;

    /**
     * 收款人
     */
    private String skr;

    /**
     * 密码区
     */
    private String mmq;

    /**
     * 机器编号
     */
    private String jqbh;

    /**
     * 校验码后6位
     */
    private String jdhm;

    /**
     * 发票状态（0-正常，1-作废，2-红冲等）
     */
    private String invoiceStatus;

    /**
     * 入账状态
     * 01-未入账
     * 02-已入账（企业所得税提前扣除）
     * 03-已入账（企业所得税不扣除）
     * 06-入账撤销
     */
    private String entryStatus;

    /**
     * 推送状态（0-未推送，1-推送中，2-已推送）
     */
    private String pushStatus;

    /**
     * 推送是否成功（0-失败，1-成功）
     */
    private String pushSuccess;

    /**
     * 推送错误信息
     */
    private String pushErrorMsg;

    /**
     * 推送时间
     */
    private LocalDateTime pushTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

