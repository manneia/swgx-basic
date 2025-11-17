package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发票信息
 *
 * 对应返回中的 data 数组元素
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String fplxdm;
	private String fpdm;
	private String fphm;
	private String jym;
	private String kprq;
	private String gfmc;
	private String gfsh;
	private String gfyhzh;
	private String gfdzdh;
	private String xfmc;
	private String xfsh;
	private String xfyhzh;
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

	private String bz;
	private String kpr;
	private String fhr;
	private String skr;
	private String mmq;
	private String jqbh;
	private String jdhm;

	private List<InvoiceItemDTO> fpmxList;
}


