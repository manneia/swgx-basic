package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 发票明细
 *
 * 对应返回中的 data[i].fpmxList 元素
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceItemDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String xh;
	private String spmc;
	private String spbm;
	private String ggxh;
	private String dw;
	private String dj;
	private String spsl;
	private String je;
	private String sl;
	private String se;
}


