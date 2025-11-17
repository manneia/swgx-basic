package com.manneia.swgx.basic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发票状态变更调用记录
 *
 * @author lk
 */
@Getter
@Setter
@TableName("invoice_status_change_log")
public class InvoiceStatusChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String invoiceCode;

    private String invoiceNumber;

    private String previousInvoiceStatus;

    private String previousCheckStatus;

    private String currentInvoiceStatus;

    private String currentCheckStatus;

    private String requestId;

    private String requestBody;

    private Integer responseStatus;

    private String responseMsg;

    private String responseOutJson;

    private LocalDateTime createdAt;
}


