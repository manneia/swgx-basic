package com.manneia.swgx.basic.utils;

import com.alibaba.fastjson.JSON;
import com.manneia.swgx.basic.exception.BizErrorCode;
import com.manneia.swgx.basic.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.text.CharSequenceUtil.isNotBlank;
import static org.apache.http.impl.client.HttpClients.createDefault;

/**
 * @author luokaixuan
 * @created 2024/09/23 21:42
 * @description: http请求工具类
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class HttpUtil {

    @Resource
    private RestTemplate restTemplate;

    private static final String HEADER_KEY = "Content-Type";

    private static final String HEADER_VALUE = "application/json";


    /**
     * 获取 rest拦截器
     *
     * @return 拦截器列表
     */
    public List<ClientHttpRequestInterceptor> getInterceptor() {
        return restTemplate.getInterceptors();
    }

    private <T> T send(String url, Object body, Class<T> responseType) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(HEADER_KEY, HEADER_VALUE);
        HttpEntity<Object> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<T> exchange = restTemplate.postForEntity(url, httpEntity, responseType);
        return exchange.getBody();
    }

    private <T> T send(String url, Object body, Map<String, String> headerParams, Class<T> responseType) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headerParams.forEach(headers::add);
        HttpEntity<Object> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<T> exchange = restTemplate.postForEntity(url, httpEntity, responseType);
        return exchange.getBody();
    }

    private <T> T send(String url, Object body, String authorization, Class<T> responseType) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Authorization", authorization);
        HttpEntity<Object> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<T> exchange = restTemplate.postForEntity(url, httpEntity, responseType);
        return exchange.getBody();
    }

    /**
     * 测试get请求
     *
     * @param url               请求路径
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forGet(String url, Class<T> responseTypeClass) {
        return send(url, null, responseTypeClass);
    }

    /**
     * 测试POST请求
     *
     * @param url               请求路径
     * @param body              请求体
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forPost(String url, Object body, Class<T> responseTypeClass) {
        return send(url, body, responseTypeClass);
    }

    /**
     * 测试POST请求
     *
     * @param url               请求路径
     * @param body              请求体
     * @param headerParams      请求头
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forPost(String url, Object body, Map<String, String> headerParams, Class<T> responseTypeClass) {
        return send(url, body, headerParams, responseTypeClass);
    }

    /**
     * 测试POST请求
     *
     * @param url               请求路径
     * @param body              请求体
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forPost(String url, Object body, String authorization, Class<T> responseTypeClass) {
        return send(url, body, authorization, responseTypeClass);
    }

    /**
     * 执行PUT请求
     *
     * @param url               请求路径
     * @param body              请求体
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forPut(String url, Object body, Class<T> responseTypeClass) {
        return send(url, body, responseTypeClass);
    }

    /**
     * 执行delete请求
     *
     * @param url               请求路径
     * @param responseTypeClass 返回类型
     *
     * @return 请求结果
     */
    public <T> T forDelete(String url, Class<T> responseTypeClass) {
        return send(url, null, responseTypeClass);
    }

    /**
     * doPost方法
     *
     * @param url          url
     * @param body         请求体
     * @param headerParams 请求头
     *
     * @return 返回
     */
    public static String doPost(String url, String body, Map<String, String> headerParams) {
        try (CloseableHttpClient httpClient = createDefault()) {
            HttpPost post = new HttpPost(url);

            // 打印请求地址
            log.info("请求地址:{}", url);

            // 设置请求头
            if (headerParams != null && !headerParams.isEmpty()) {
                for (Map.Entry<String, String> e : headerParams.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    if (isNotBlank(value)) {
                        post.setHeader(key, value);
                        log.info("请求头, key:{}, value:{}", key, value);
                    }
                }
            }
            // 打印请求体
            if (body != null && !body.isEmpty()) {
                log.info("请求体:{} ", body);
            }
            // 设置请求体
            assert body != null;
            StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
            entity.setContentType(HEADER_VALUE);
            post.setEntity(entity);
            // 发送请求并获取响应
            HttpResponse response = httpClient.execute(post);
            org.apache.http.HttpEntity responseEntity = response.getEntity();
            log.info("响应内容:{}", EntityUtils.toString(responseEntity, StandardCharsets.UTF_8));
            return EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.info("http请求失败:{}", JSON.toJSONString(e.getLocalizedMessage()));
            throw new SystemException(BizErrorCode.HTTP_SERVER_ERROR);
        }
    }
}
