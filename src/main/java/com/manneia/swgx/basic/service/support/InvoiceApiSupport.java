package com.manneia.swgx.basic.service.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 发票相关接口调用辅助，负责构建请求头和签名。
 *
 * @author lk
 */
@Slf4j
@Component
public class InvoiceApiSupport {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Value("${invoice.api.app-id:KJmROaZegM}")
    private String appId;

    @Value("${invoice.api.app-secret:752A7B6360A178BEA74D105BA83A9097}")
    private String appSecret;

    /**
     * 构建带签名的请求头
     */
    public HeaderPackage buildHeaders() {
        String timestamp = "S" + LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String signSource = appId + timestamp + requestId + appSecret;
        String sign = md5(signSource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AppID", appId);
        headers.set("TimeStamp", timestamp);
        headers.set("RequestID", requestId);
        headers.set("Sign", sign);

        return new HeaderPackage(headers, requestId, timestamp, sign);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            throw new RuntimeException("计算MD5失败", e);
        }
    }

    @Getter
    public static class HeaderPackage {
        private final HttpHeaders headers;
        private final String requestId;
        private final String timestamp;
        private final String sign;

        public HeaderPackage(HttpHeaders headers, String requestId, String timestamp, String sign) {
            this.headers = headers;
            this.requestId = requestId;
            this.timestamp = timestamp;
            this.sign = sign;
        }
    }
}


