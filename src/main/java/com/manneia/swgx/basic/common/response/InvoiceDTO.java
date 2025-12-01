package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发票信息
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
    private String zpfphm;
	private String kprq;
	
	/**
	 * 发票状态
	 */
	private String fpzt;
	
	/**
	 * 发票状态描述
	 */
	private String fpztms;
	
	private String xfmc;
	private String xfsh;
	private String gfmc;
	private String gfsh;
	
	/**
	 * 销售方地址
	 */
	private String xfdz;
	
	/**
	 * 销售方电话
	 */
	private String xfdh;
	
	/**
	 * 销售方银行
	 */
	private String xfyh;
	
	/**
	 * 销售方账号
	 */
	private String xfzh;
	
	private String xfdzdh;
	private String xfyhzh;
	
	private String gfdzdh;
	private String gfyhzh;
	
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

	private String bz;
	private String kpr;
	private String fhr;
	private String skr;
	private String mmq;
	private String jqbh;
	private String jdhm;
	
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
	 * 发票明细列表
	 */
	private List<InvoiceItemDTO> fpmxList;
}


