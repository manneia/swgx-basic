package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 往期勾选发票信息查询响应
 *
 * <p>对应返回结构：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "操作成功",
 *   "data": {
 *     "nsrsbh": "913101156072273832",
 *     "tjyf": "202511",
 *     "fpxxList": [
 *       {
 *         "xh": 1,
 *         "sdfphm": "25612000000122859427",
 *         "fpdm": "256120000001",
 *         "fphm": "22859427",
 *         "kprq": "2025-10-24 14:06:48",
 *         "xfsh": "91610131MA6U3F619J",
 *         "xfmc": "陕西富化贸易有限公司",
 *         "je": 1327.43,
 *         "se": 172.57,
 *         "dkse": 172.57,
 *         "gxrq": "2025-11-12 13:40:29",
 *         "fplx": "数电票（增值税专用发票）",
 *         "fplxdm": "81",
 *         "yt": "抵扣",
 *         "fpzt": "0",
 *         "glzt": "正常",
 *         "sjly": "电子发票服务平台"
 *       }
 *     ]
 *   },
 *   "success": true
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceHistoryQueryResponse implements Serializable {

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
     * 响应数据
     */
    private InvoiceHistoryData data;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 往期勾选发票数据
     */
    @Getter
    @Setter
    public static class InvoiceHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 纳税人识别号
         */
        private String nsrsbh;

        /**
         * 统计月份
         */
        private String tjyf;

        /**
         * 发票信息列表
         */
        private List<InvoiceHistoryItemDTO> fpxxList;
    }
}
