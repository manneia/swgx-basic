package com.manneia.swgx.basic.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author lkx
 * @description com.manneia.swgx.basic.utils
 * @created 2025-11-07 14:14:14
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BwHttpUtil {

    static Map<String, String> headers;

    public static Map<String, String> getHeaders() {
        return headers;
    }

    public static void setHeaders(Map<String, String> headers) {
        BwHttpUtil.headers = headers;
    }

    //上海百旺网关调用授权

    @Value("${swgx.http.ak_id}")
    private String akId;

    @Value("${swgx.http.ak_secret}")
    private String akSecret;


    public String httpPostRequest(String url, String params, String ContentType) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;
        try {
            StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            if ("json".equals(ContentType)) {
                httpPost.setHeader("Content-type", "application/json");
            } else if ("multipart/form-data".equals(ContentType)) {
                httpPost.setHeader("Content-type", "multipart/form-data");
            } else {
                httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            }
            httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            HtjsApiSdkClient hsc = new HtjsApiSdkClient(akId, akSecret);

            Map header = hsc.get_signed_headers("POST", url);
            Set keyset = header.keySet();
            Iterator it = keyset.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                httpPost.setHeader(key, (String) header.get(key));
                log.info("key值：{}对应值：{}", key, header.get(key));
            }

            response = httpClient.execute(httpPost);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        String result = "";
        if (response.getStatusLine().getStatusCode() == 200) {
            try {
                result = EntityUtils.toString(response.getEntity(), "utf-8");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        try {
            httpClient.close();
            response.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return result;
    }

}
