package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 全量进项发票查询响应
 *
 * <p>响应结构：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "操作成功",
 *   "pageNumber": null,
 *   "pageSize": null,
 *   "total": null,
 *   "data": [ InvoiceFullItemDTO... ],
 *   "success": true
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceFullQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 页码
     */
    private Integer pageNumber;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 发票数据列表
     */
    private List<InvoiceFullItemDTO> data;

    /**
     * 是否成功
     */
    private Boolean success;
}
