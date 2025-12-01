package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 往期勾选发票信息查询请求参数
 *
 * <p>对应请求体：</p>
 * <pre>
 * {
 *     "nsrsbh": "914**************82",
 *     "czlsh": "237654***********43",
 *     "tjyf": "202309"
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceHistoryQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 纳税人识别号
     */
    private String nsrsbh;

    /**
     * 操作流水号
     */
    private String czlsh;

    /**
     * 统计月份（格式：YYYYMM，如：202309）
     */
    private String tjyf;
}
