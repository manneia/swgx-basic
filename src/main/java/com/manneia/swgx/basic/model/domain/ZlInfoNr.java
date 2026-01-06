package com.manneia.swgx.basic.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @author lkx
 * @TableName ZL_INFO_NR
 */
@TableName(value ="ZL_INFO_NR")
@Data
public class ZlInfoNr implements Serializable {
    /**
     * 资料编码
     */
    @TableId(value = "ZLCODE")
    private String dataCode;

    /**
     * 摘要
     */
    @TableField(value = "ZLZY")
    private String dataZy;

    /**
     * 库种
     */
    @TableField(value = "ZLFLAG")
    private String dataFlag;

    /**
     * 是否替换
     */
    @TableField(value = "ISREPLACE")
    private String isReplace;

    /**
     * 内容
     */
    @TableField(value = "ZLNR")
    private String dataContent;

    /**
     * 文本内容
     */
    @TableField(value = "ZLNRWB")
    private String dataWbContent;

    /**
     * 超链接内容
     */
    @TableField(value = "ZLCLJNR")
    private String dataCljNr;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}