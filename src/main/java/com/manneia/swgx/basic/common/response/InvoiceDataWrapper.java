package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 发票数据包装类
 * 对应返回中的 data 字段
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceDataWrapper implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 发票信息列表
	 */
	private List<InvoiceDTO> fpxxList;
}
