package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 发票采集查询响应
 *
 * <p>对应接口返回结构：</p>
 * <pre>
 * {
 *   "success": true,
 *   "code": 0,
 *   "msg": "",
 *   "pageNumber": 1,
 *   "pageSize": 1,
 *   "total": 1,
 *   "data": [ ... ]
 * }
 * </pre>
 *
 * @author lk
 */
@Getter
@Setter
public class InvoiceCollectionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private Integer code;

    private String msg;

    private Integer pageNumber;

    private Integer pageSize;

    private Long total;

    private List<InvoiceCollectionItem> data;

    @Getter
    @Setter
    public static class InvoiceCollectionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String qspc;
        private String fplxdm;
        private String fpdm;
        private String fphm;
        private String fpzt;
        private String jym;
        private String jqbm;
        private String kprq;
        private String hjje;
        private String hjse;
        private String jshj;
        private String xfnsrsbh;
        private String xfmc;
        private String xfkhhzh;
        private String xfdzdh;
        private String gfnsrsbh;
        private String gfmc;
        private String gfkhhzh;
        private String gfdzdh;
        private String skr;
        private String fhr;
        private String kpr;
        private String fpbz;
        private String fpslv;
        private String fpslvLable;
        private String fpdkse;
        private String tdyslxdm;
        private String fpywjUrl;
        private String fpylUrl;
        private String wjlx;
        private String sjly;
        private String tpxzjd;
        private String gssh;
        private String gsmc;
        private String fpzw;
        private String fpyqzt;
        private String cjpc;
        private String cjztDm;
        private String cjr;
        private String cjrq;
        private String cypc;
        private String cyztDm;
        private String cyr;
        private String cyrq;
        private String bxztDm;
        private String bxpc;
        private String bxczr;
        private String bxczrq;
        private String qsztDm;
        private String qsczr;
        private String qsczrq;
        private String jzztDm;
        private String jzpc;
        private String jzczr;
        private String jzczrq;
        private String pdztDm;
        private String pdpc;
        private String pdczr;
        private String pdczrq;
        private String gxztDm;
        private String gxlx;
        private String gxpc;
        private String gxczr;
        private String gxczrq;
        /**
         * 入账状态代码
         * 01-未入账
         * 02-已入账（企业所得税提前扣除）
         * 03-已入账（企业所得税不扣除）
         * 06-入账撤销
         */
        private String rzztDm;
        private String rzpc;
        private String rzczr;
        private String rzczrq;
        private String xgrq;
        private String xgr;

        private List<InvoiceDetail> fpmxList;
        private VehicleInfo jdcxx;
        private SecondHandCarInfo escxx;
        private RailwayTicketInfo tldzkpxx;
        private TravelTicketInfo fyfpxx;
        private AirTicketInfo hkxcd;
        private List<MedicineWriteOffInfo> wszmmxList;
        private List<LogisticsInfo> hwaysmxList;
        private List<PassengerTicketInfo> lkysmxList;
        private List<ConstructionServiceInfo> jzfwmxList;
        private List<RealEstateInfo> bdczlmxList;
        private List<RealEstateInfo> bdcxsmxList;
        private List<TaxRefundInfo> txfmxList;
    }

    @Getter
    @Setter
    public static class InvoiceDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        private String spmc;
        private String spbm;
        private String ggxh;
        private String dw;
        private String dj;
        private String spsl;
        private String je;
        private String sl;
        private String se;
    }

    @Getter
    @Setter
    public static class VehicleInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cd;
        private String cjh;
        private String cllx;
        private String cpxh;
        private String dw;
        private String fdjhm;
        private String hgzh;
        private String jddm;
        private String jdhm;
        private String jkzmsh;
        private String sjdh;
        private String wspzhm;
        private String xcrs;
        private String zgswjgdm;
        private String zgswjgmc;
    }

    @Getter
    @Setter
    public static class SecondHandCarInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cjh;
        private String cllx;
        private String cpxh;
        private String cpzh;
        private String djzh;
        private String escscdh;
        private String escscdz;
        private String escsckhhjzh;
        private String escscmc;
        private String escsctyshxydm;
        private String jddm;
        private String jdhm;
        private String jypmdw;
        private String jypmdwdh;
        private String jypmdwdz;
        private String jypmdwkhjzh;
        private String jypmdwtyshxydm;
        private String zrdclglsmc;
    }

    @Getter
    @Setter
    public static class RailwayTicketInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cc;
        private String ccrq;
        private String cfsj;
        private String cfz;
        private String cx;
        private String ddz;
        private String dzkph;
        private String kpdw;
        private String kpdwdm;
        private String kttz;
        private String pz;
        private String pzlx;
        private String xb;
        private String xm;
        private String xw;
        private String yhbs;
        private String ypcfz;
        private String ypddz;
        private String yppj;
        private String ytje;
        private String ywlx;
        private String zfje;
        private String zjh;
    }

    @Getter
    @Setter
    public static class TravelTicketInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String cfd;
        private String ddd;
        private String lksfzh;
        private String lkXm;
        private String xcrq;
        private String xcsj;
    }

    @Getter
    @Setter
    public static class AirTicketInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String bxf;
        private String chyy;
        private String cyr;
        private String cyrq;
        private String dzkphm;
        private String gngjbs;
        private String gpdh;
        private String hbh;
        private String hd;
        private String jehj;
        private String kpjb;
        private String kpsxr;
        private String kpzt;
        private String lkxm;
        private String mdz;
        private String mfxle;
        private String mhfzj;
        private String pj;
        private String qfsj;
        private String qtsf;
        private String qz;
        private String ryfjf;
        private String sdphm;
        private String sfz;
        private String tkdw;
        private String tkrg;
        private String tsxx;
        private String xswddh;
        private String ysdphm;
        private String yxjzr;
        private String yxsfzhm;
        private String yzm;
        private String zwdj;
        private String zzSslmc;
        private String zzsse;
        private String zzssl;
    }

    @Getter
    @Setter
    public static class MedicineWriteOffInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String pmmc;
        private String rtkrq;
        private String sjje;
        private String sksssq;
        private String sz;
        private String ypzh;
    }

    @Getter
    @Setter
    public static class LogisticsInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String xh;
        private String ddd;
        private String qyd;
        private String ysgiph;
        private String ysgjzl;
        private String yshwmc;
    }

    @Getter
    @Setter
    public static class PassengerTicketInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String xh;
        private String cfd;
        private String cxr;
        private String cxrq;
        private String ddd;
        private String dj;
        private String jtgjlx;
        private String yxsfzjh;
    }

    @Getter
    @Setter
    public static class ConstructionServiceInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String xh;
        private String je;
        private String jzfwfsd;
        private String jzxmmc;
        private String se;
        private String slv;
        private String slvmc;
        private String xmmc;
    }

    @Getter
    @Setter
    public static class RealEstateInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String xh;
        private String bdcqzsh;
        private String dj;
        private String je;
        private String mjdw;
        private String se;
        private String sl;
        private String slv;
        private String slvmc;
        private String xmmc;
    }

    @Getter
    @Setter
    public static class TaxRefundInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String xh;
        private String cph;
        private String je;
        private String lx;
        private String se;
        private String slv;
        private String slvmc;
        private String txrqq;
        private String txrqz;
        private String xmmc;
    }
}


