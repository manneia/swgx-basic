package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 海关缴款书查询请求参数
 */
@Getter
@Setter
public class CustomsPaymentQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 操作流水号
     */
    private String czlsh;

    /**
     * 纳税人识别号
     */
    private String nsrsbh;

    /**
     * 退付日期起（格式：yyyyMMdd）
     */
    private String tfrqq;

    /**
     * 退付日期止（格式：yyyyMMdd）
     */
    private String tfrqz;

    /**
     * 入账状态
     */
    private String rzzt;
}
