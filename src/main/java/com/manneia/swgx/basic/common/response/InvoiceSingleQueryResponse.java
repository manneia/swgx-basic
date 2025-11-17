package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 单票信息查询响应
 *
 * <p>对应返回结构：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "操作成功",
 *   "data": {
 *     "fpdm": "245020000000",
 *     "fphm": "9******3",
 *     "fplx": "81",
 *     "kprq": "2024-10-22 07:42:25",
 *     "xfmc": "重庆*******有限公司",
 *     "xfsh": "91***************52",
 *     "je": 754.72,
 *     "se": 45.28,
 *     "fpzt": "0",
 *     "yxse": 45.28,
 *     "yclb": "0",
 *     "skssq": "",
 *     "sfgx": "0",
 *     "gxsj": "",
 *     "gxyt": "",
 *     "sfqr": "0",
 *     "qrsj": ""
 *   }
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceSingleQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private InvoiceSingleData data;

    @Getter
    @Setter
    public static class InvoiceSingleData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String fpdm;
        private String fphm;
        private String fplx;
        private String kprq;
        private String xfmc;
        private String xfsh;
        private BigDecimal je;
        private BigDecimal se;
        private String fpzt;
        private BigDecimal yxse;
        private String yclb;
        private String skssq;
        private String sfgx;
        private String gxsj;
        private String gxyt;
        private String sfqr;
        private String qrsj;
    }
}


