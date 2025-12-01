package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 同步接口返回结果
 *
 * {
 *   "success": true,
 *   "code": 0,
 *   "msg": "",
 *   "data": {
 *     "fpxxList": [ InvoiceDTO... ]
 *   }
 * }
 *
 * @author lk
 */
@Getter
@Setter
public class SyncResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private Boolean success;

	private Integer code;

	private String msg;

	private InvoiceDataWrapper data;
	
	/**
	 * 获取发票列表的便捷方法
	 */
	public List<InvoiceDTO> getFpxxList() {
		return data != null ? data.getFpxxList() : null;
	}
}


