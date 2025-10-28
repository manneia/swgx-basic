package com.manneia.swgx.basic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author luokaixuan
 * @description com.manneia.baiwangbasic.config
 * @created 2025/5/13 15:18
 */
@Configuration
@SuppressWarnings("unused")
@ConfigurationProperties(prefix = "customize")
public class CustomizeUrlConfig {

    /**
     * 流水单上传接口
     */
    private String uploadLsdUrl;

    /**
     * 发票开具接口
     */
    private String invoiceIssueUrl;


}
