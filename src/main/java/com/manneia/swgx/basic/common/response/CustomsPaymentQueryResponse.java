package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 海关缴款书查询响应
 *
 * 对应结构：
 * {
 *   "code": 0,
 *   "msg": "操作成功",
 *   "data": [ { ... } ],
 *   "success": true
 * }
 */
@Getter
@Setter
public class CustomsPaymentQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private List<CustomsPaymentItem> data;

    private Boolean success;

    @Getter
    @Setter
    public static class CustomsPaymentItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String nsrsbh;
        private String nsrmc;
        private String jkshm;
        private String tfrq;
        private BigDecimal se;
        private BigDecimal yxse;
        private String xxly;
        private String gxzt;
        private String gxrq;
        private String glzt;
        private String sqhdzt;

        private List<CustomsPaymentDetail> jksmxList;
    }

    @Getter
    @Setter
    public static class CustomsPaymentDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 税号
         */
        private String sh;

        /**
         * 货物名称
         */
        private String hwmc;

        /**
         * 商品数量（可能带小数）
         */
        private String spsl;

        /**
         * 单位
         */
        private String dw;

        /**
         * 无税价格
         */
        private BigDecimal wsjg;

        /**
         * 税率（例如："13%"）
         */
        private String sl;

        /**
         * 税款金额
         */
        private BigDecimal skje;

        /**
         * 税率（数值形式）
         */
        private Integer bigDecimalSl;
    }
}
