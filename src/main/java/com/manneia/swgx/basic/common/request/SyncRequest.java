package com.manneia.swgx.basic.common.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 同步请求参数
 *
 * <p>格式示例：
 * {
 *   "nsrsbh": "",
 *   "qqlx": "",
 *   "kprqq": "",
 *   "kprqz": "",
 *   "czlsh": "",
 *   "zzhlx": ""
 * }
 * </p>
 *
 * @author lk
 */
@Setter
@Getter
public class SyncRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 纳税人识别号，购方税号
     */
    @NotBlank(message = "纳税人识别号不能为空")
    private String nsrsbh;

    /**
     * 请求类型；1-缓存（弃用）、3-超快实时（仅主从信息）
     */
    @NotBlank(message = "请求类型不能为空")
    private String qqlx;

    /**
     * 开票日期起，格式：yyyyMMdd
     */
    @NotBlank(message = "开票日期起不能为空")
    @Pattern(regexp = "^\\d{8}$", message = "开票日期起格式应为yyyyMMdd")
    private String kprqq;

    /**
     * 开票日期止，格式：yyyyMMdd
     */
    @NotBlank(message = "开票日期止不能为空")
    @Pattern(regexp = "^\\d{8}$", message = "开票日期止格式应为yyyyMMdd")
    private String kprqz;

    /**
     * 操作流水号，不允许为空，长度32以内，建议UUID
     */
    @NotBlank(message = "操作流水号不能为空")
    @Size(max = 32, message = "操作流水号长度不能超过32")
    private String czlsh;

    /**
     * 子账号类型；请求类型为1或3时可用：空-进销项，0-进项，1-销项
     * 允许为空或取值为0/1
     */
    @Pattern(regexp = "^(|0|1)$", message = "子账号类型仅支持空、0（进项）、1（销项）")
    private String zzhlx;
}


