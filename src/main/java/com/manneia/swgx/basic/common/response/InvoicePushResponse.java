package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 发票推送接口响应
 *
 * @author lk
 */
@Getter
@Setter
public class InvoicePushResponse implements Serializable {

    private static final long serialVersionUID = 1L;


    private String status;


    private String outJson;


    private String msg;

}

