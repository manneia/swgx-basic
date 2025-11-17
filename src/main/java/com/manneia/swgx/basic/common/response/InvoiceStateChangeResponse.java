package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 发票状态变更响应
 *
 * <pre>
 * {
 *   "status": 0,
 *   "msg": "",
 *   "outJson": ""
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceStateChangeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer status;

    private String msg;

    private String outJson;
}


