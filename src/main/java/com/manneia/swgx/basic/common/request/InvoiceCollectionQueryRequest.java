package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 发票采集查询请求
 *
 * <p>对应请求体格式：</p>
 * <pre>
 * {
 *     "czlsh": "",
 *     "accountId": "",
 *     "czr": "",
 *     "gssh": "",
 *     "fpdm": "",
 *     "fphm": "",
 *     "kprqq": "",
 *     "kprqz": "",
 *     "xfmc": "",
 *     "xfsh": "",
 *     "lrr": "",
 *     "lrsjq": "",
 *     "lrsjz": "",
 *     "fplxdm": "",
 *     "fpzt": "",
 *     "fpzw": "",
 *     "cjpc": "",
 *     "cjztdm": "",
 *     "cjr": "",
 *     "cjrqq": "",
 *     "cjrqz": "",
 *     "bxdh": "",
 *     "bxr": "",
 *     "bxsjq": "",
 *     "bxsjz": "",
 *     "bxzt": "",
 *     "pzh": "",
 *     "jzr": "",
 *     "jzsjq": "",
 *     "jzsjz": "",
 *     "jzzt": "",
 *     "qspc": "",
 *     "qsr": "",
 *     "qssjq": "",
 *     "qssjz": "",
 *     "qszt": "",
 *     "pageNumber": "",
 *     "pageSize": ""
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceCollectionQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private String czlsh;

    private String accountId;

    private String czr;

    private String gssh;

    private String fpdm;

    private String fphm;

    private String kprqq;

    private String kprqz;

    private String xfmc;

    private String xfsh;

    private String lrr;

    private String lrsjq;

    private String lrsjz;

    private String fplxdm;

    private String fpzt;

    private String fpzw;

    private String cjpc;

    private String cjztdm;

    private String cjr;

    private String cjrqq;

    private String cjrqz;

    private String bxdh;

    private String bxr;

    private String bxsjq;

    private String bxsjz;

    private String bxzt;

    private String pzh;

    private String jzr;

    private String jzsjq;

    private String jzsjz;

    private String jzzt;

    private String qspc;

    private String qsr;

    private String qssjq;

    private String qssjz;

    private String qszt;

    private Integer pageNumber;

    private Integer pageSize;
}


