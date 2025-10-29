package com.manneia.swgx.basic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author luokaixuan
 * @description com.manneia.baiwangbasic.config
 * @created 2025/5/13 15:18
 */
@Configuration
@Data
@SuppressWarnings("unused")
@ConfigurationProperties(prefix = "customize")
public class CustomizeUrlConfig {

    /**
     * 获取企业信息接口
     */
    private String queryCompanyInfoUrl;

    /**
     * 查询商品信息接口
     */
    private String queryGoodsInfoUrl;

    /**
     * 推送单据接口
     */
    private String pushInvoiceDocUrl;

}
