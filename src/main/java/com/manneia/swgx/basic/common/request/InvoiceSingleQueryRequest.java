package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 单票信息查询请求参数
 *
 * <p>对应请求体：</p>
 * <pre>
 * {
 *     "fpdm": "",
 *     "fphm": "2450200000009******3",
 *     "nsrsbh": "91***********************R",
 *     "czlsh": "6372****************12564"
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceSingleQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 发票代码
     */
    private String fpdm;

    /**
     * 发票号码
     */
    private String fphm;

    /**
     * 纳税人识别号
     */
    private String nsrsbh;

    /**
     * 操作流水号
     */
    private String czlsh;
}


