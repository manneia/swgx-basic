package com.manneia.swgx.basic.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @author lkx
 * @TableName ZL_INFO
 */
@TableName(value ="ZL_INFO")
@Data
public class ZlInfo implements Serializable {
    /**
     * 资料编码
     */
    @TableId(value = "ZLCODE")
    private String dataCode;

    /**
     * 工作流实例ID(存放发布流程)
     */
    @TableField(value = "ZLWFID")
    private String dataFlowId;

    /**
     * 工作流活动ID
     */
    @TableField(value = "ACTIVITYID")
    private String activityId;

    /**
     * 资料简码
     */
    @TableField(value = "ZLJM")
    private String dataBrevityCode;

    /**
     * 标题
     */
    @TableField(value = "ZLTITLE")
    private String dataTitle;

    /**
     * 关键字
     */
    @TableField(value = "ZLGJZ")
    private String dataKeyWord;

    /**
     * 库种(1法规库  2问题库  3办事指南 4业务专题 5表证单书 6相关法规 7学习园地 8通知公告 9通讯录)
     */
    @TableField(value = "ZLFLAG")
    private String dataFlag;

    /**
     * 类别(所属资料类别)
     */
    @TableField(value = "ZLTYPE")
    private String dataType;

    /**
     * 资料行业类别
     */
    @TableField(value = "ZLHYTYPE")
    private String dataIndustryType;

    /**
     * 录音文件
     */
    @TableField(value = "ZLLYWJ")
    private String dataAudioFile;

    /**
     * 传真文件
     */
    @TableField(value = "ZLCZWJ")
    private String dataFaxDocument;

    /**
     * 资料录入人
     */
    @TableField(value = "ZLLRR")
    private String lrr;

    /**
     * 资料录入时间
     */
    @TableField(value = "ZLLRSJ")
    private Date lrSj;

    /**
     * 录入单位
     */
    @TableField(value = "ZLLRDW")
    private String lrDw;

    /**
     * 状态
     */
    @TableField(value = "ZLZT")
    private String zt;

    /**
     * 是否有效(0有效，1全文无效，2部分有效)
     */
    @TableField(value = "ZLSFYX")
    private String sfYx;

    /**
     * 是否发布
     */
    @TableField(value = "ZLSFFB")
    private String sfFb;

    /**
     * 发布人
     */
    @TableField(value = "ZLFBR")
    private String fbr;

    /**
     * 资料来源
     */
    @TableField(value = "ZLLY")
    private String ly;

    /**
     * 税种(1国税  2地税 3 通用)
     */
    @TableField(value = "ZLSZ")
    private String sz;

    /**
     * 文号
     */
    @TableField(value = "ZLWH")
    private String wh;

    /**
     * 发文单位
     */
    @TableField(value = "ZLFWDW")
    private String fwDw;

    /**
     * 发文日期
     */
    @TableField(value = "ZLBFRQ")
    private Date fwRq;

    /**
     * 失效日期
     */
    @TableField(value = "ZLSXRQ")
    private Date sqRq;

    /**
     * 实施日期
     */
    @TableField(value = "ZLSSRQ")
    private Date ssRq;

    /**
     * 资料文件等级(法律、法规、规范性文件、其他)
     */
    @TableField(value = "ZLWJDJ")
    private String wdDj;

    /**
     * 适用等级 (全国0、省1、地市2、区县3)
     */
    @TableField(value = "ZLSYDJ")
    private String syDj;

    /**
     * 资料当前审核人
     */
    @TableField(value = "ZLNOWSHR")
    private String currentShr;

    /**
     * 资料当前审核角色
     */
    @TableField(value = "ZLNOWSHJS")
    private String currentShJs;

    /**
     * 当前资料处理机构
     */
    @TableField(value = "ZLNOWJG")
    private String currentClJg;

    /**
     * 当前资料处理部门
     */
    @TableField(value = "ZLNOWBM")
    private String currentClBm;

    /**
     * 资料最后操作人
     */
    @TableField(value = "ZLZHCZR")
    private String zhCzr;

    /**
     * 资料最后操作时间
     */
    @TableField(value = "ZLZHCZSJ")
    private Date zhCzSj;

    /**
     * 前次操作人
     */
    @TableField(value = "LASTCZR")
    private String qcCzr;

    /**
     * 知识权重值
     */
    @TableField(value = "ZLQZZ")
    private String zsQzz;

    /**
     * 无效原因
     */
    @TableField(value = "ZLWXYY")
    private String wxYy;

    /**
     * 资料摘要
     */
    @TableField(value = "ZLZY")
    private String zlZy;

    /**
     * 资料批注
     */
    @TableField(value = "ZLPZ")
    private String zlPz;

    /**
     * 资料是否公开(0主动公开 1依申请公开 2不予公开)
     */
    @TableField(value = "ZLSFGK")
    private String zlSfGk;

    /**
     * 资料发布日期
     */
    @TableField(value = "ZLFBRQ")
    private Date zlFbRq;

    /**
     * 资料撤销原因
     */
    @TableField(value = "ZLCXYY")
    private String cxYy;

    /**
     * 资料是否拟转OA  0 转OA 1转OA返回
     */
    @TableField(value = "ZLNZOA")
    private String sfNzOa;

    /**
     * 知识转OA编号
     */
    @TableField(value = "ZLOAID")
    private String zOaCode;

    /**
     * 资料所属地区
     */
    @TableField(value = "ZLDQ")
    private String zlSsDq;

    /**
     * 资料所属地区名称
     */
    @TableField(value = "ZLDQMC")
    private String zlSsDqMc;

    /**
     * 资料发文字轨(编码为2位，01代表主席令，02代表国务院令，03代表国发，04代表财政部令，05代表税务总局令，06代表财税字，07代表国税发，08代表国税函，20代表苏国税发，21代表苏国税函，22代表苏国税明电，30代表苏地税发，31代表苏地税函)
     */
    @TableField(value = "ZLFWZG")
    private String zlFwZz;

    /**
     * 是否段落(单选是否)
     */
    @TableField(value = "ZLSFDL")
    private String sfDl;

    /**
     * 是否政策优惠(单选是否)
     */
    @TableField(value = "ZLSFZCYH")
    private String sfZcYh;

    /**
     * 适用对象类型：（企业、个体、自然人）
     */
    @TableField(value = "ZLDXLX")
    private String syDxLx;

    /**
     * 资料当前版本号
     */
    @TableField(value = "ZLDQBBH")
    private String currentVersionCode;

    /**
     * 是否地方法规
     */
    @TableField(value = "ZLDFFG")
    private String dfFg;

    /**
     * 资料下发 （0代表数据待写入下发表，1代表数据写入下发表成功，2代表数据写入下发表失败）
     */
    @TableField(value = "ZLXF")
    private String zlXf;

    /**
     * 业务转知识库操作人
     */
    @TableField(value = "ZLYWCZR")
    private String ywToZskCzr;

    /**
     * 资料类别名称
     */
    @TableField(value = "ZLTYPEMC")
    private String dataTypeMc;

    /**
     * 资料所属行业类别名称
     */
    @TableField(value = "ZLHYTYPEMC")
    private String dataIndustryTypeMc;

    /**
     * 资料分值
     */
    @TableField(value = "ZLFZ")
    private Long dataFz;

    /**
     *  删除标志
     */
    @TableField(value = "ZLISDEL")
    private String dataIsDelete;

    /**
     * 资料完整法规编号
     */
    @TableField(value = "ZLWZFGBH")
    private String dataWzFgCode;

    /**
     * 适用对象类型名称
     */
    @TableField(value = "ZLDXLXMC")
    private String syDxTypeMc;

    /**
     * 工作流实例ID(存放撤销流程)
     */
    @TableField(value = "ZLCXLCID")
    private String flowId;

    /**
     * 更新时间（倒库用）
     */
    @TableField(value = "GXSJ")
    private String updateTime;

    /**
     * 版本号（倒库用）
     */
    @TableField(value = "BBH")
    private String version;

    /**
     * 交换状态
     */
    @TableField(value = "JHZT")
    private String jhZt;

    /**
     * 行政区划
     */
    @TableField(value = "XZQH")
    private String xzQh;

    /**
     * 撤销申请人
     */
    @TableField(value = "ZLCXSQR")
    private String cxSqr;

    /**
     * 资料段落所属ID
     */
    @TableField(value = "ZLDLSSID")
    private String dataSsId;

    /**
     * 资料段落序号
     */
    @TableField(value = "ZLDLXH")
    private String dataDlXh;

    /**
     * 资料失效处理标识(1代表已操作)
     */
    @TableField(value = "ZLSXCZBS")
    private String zlSxClBs;

    /**
     * 资料是否检索(0默认检索，1不进行检索)
     */
    @TableField(value = "ZLSFJS")
    private String dataSfJs;

    /**
     * 资料是否总局审核(0默认未审核，1已审核)
     */
    @TableField(value = "ZLSFZJSH")
    private String dataSfZjSh;

    /**
     * 资料主管单位
     */
    @TableField(value = "ZLZGDW")
    private String dataZgDw;

    /**
     * 资料主管单位名称
     */
    @TableField(value = "ZLZGDWMC")
    private String dataZgDwMc;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}