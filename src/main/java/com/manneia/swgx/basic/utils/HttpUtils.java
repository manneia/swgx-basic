package com.manneia.swgx.basic.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * http工具类
 *
 * @author zhangshiwen
 * @date 2023/10/31
 */

public class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final Integer TIMEOUT = 5 * 60 * 1000;

    private static final String FORWORD_GET_URL = "https://bw-invoice-test.yfdyf.com/gxhfw/yf/getforward?param=";
    private static final String FORWORD_POST_URL = "https://bw-invoice-test.yfdyf.com/gxhfw/yf/forward";

    private HttpUtils() {
    }

    /**
     * post请求 入参出参为实体类
     *
     * @param url        请求地址
     * @param inputParam 请求内容实体类
     * @param msg        请求说明
     * @param clazz      出参实体类class
     * @param headers    请求头
     * @return 出参实体类
     */
    public static <T> T httpPostRequest(String url, Object inputParam, String msg, Class<T> clazz, Map<String, String> headers) {
        String body;
        if (inputParam instanceof String) {
            body = StringUtilsExt.getString(inputParam);
        } else {
            body = JSON.toJSONString(inputParam);
        }
        String resp = httpPostRequest(url, body, msg, headers);
        T t;
        try {
            t = JSON.parseObject(resp, clazz);
        } catch (Exception e) {
            throw new HttpException("返回报文解析异常,报文:" + resp);
        }
        return t;
    }

    public static <T> T httpPostRequest(String url, Object inputParam, String msg, Class<T> clazz) {
        return httpPostRequest(url, inputParam, msg, clazz, null);
    }


    /**
     * post请求 入参出参为实体类
     *
     * @param url        请求地址
     * @param inputParam 请求内容实体类
     * @param msg        请求说明
     * @param headers    请求头
     * @return 返回内容json
     */
    public static String httpPostRequest(String url, Object inputParam, String msg, Map<String, String> headers) {
        return httpPostRequest(url, JSON.toJSONString(inputParam), msg, headers);
    }

    public static String httpPostRequest(String url, Object inputParam, String msg) {
        return httpPostRequest(url, JSON.toJSONString(inputParam), msg, new HashMap<>(1));
    }


    /**
     * post请求
     *
     * @param url     请求地址
     * @param json    请求json内容
     * @param msg     请求说明
     * @param headers 请求头
     * @return 返回内容json
     */
    public static String httpPostRequest(String url, String json, String msg, Map<String, String> headers) {
        String temp = "{}请求地址:{} 参数: {}";
        HttpRequest request = HttpRequest.post(url).body(json);
        if (CollectionUtils.isNotEmpty(headers)) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.header(header.getKey(), header.getValue());
            }
            log.info(temp, msg, url, request);
        } else {
            log.info(temp, msg, url, json);
        }
        return executeHttpRequest(msg, request);
    }

    public static String httpPostRequest(String url, String json, String msg) {
        return httpPostRequest(url, json, msg, new HashMap<>(1));
    }

    public static String httpPostRequestBase64(String url, Object obj, String msg) {
        return httpPostRequestJsonBase64(url, JSON.toJSONString(obj), msg, new HashMap<>(1));
    }

    public static String httpPostRequestBase64(String url, Object obj, String msg, Map<String, String> headers) {
        return httpPostRequestJsonBase64(url, JSON.toJSONString(obj), msg, headers);
    }

    public static String httpPostRequestJsonBase64(String url, String json, String msg, Map<String, String> headers) {
        log.info("{}Base64明文参数:{}", msg, json);
        return httpPostRequest(url, Base64.encode(json), msg, headers);
    }


    /**
     * get请求
     *
     * @param url 请求地址
     * @param msg 请求说明
     */
    public static String httpGetRequest(String url, String msg) {
        log.info("{}请求地址:{}", msg, url);
        HttpRequest request = HttpRequest.get(url);
        return executeHttpRequest(msg, request);
    }

    public static String httpGetRequest(String url, Object obj, String msg) {
        String queryParam = HttpUtil.toParams(BeanUtil.beanToMap(obj, false, true));
        url = url + "?" + queryParam;
        return httpGetRequest(url, msg);
    }


    public static String httpGetRequest(String url, String msg, Map<String, String> headers) {
        log.info("{}请求地址:{}", msg, url);
        HttpRequest request = HttpRequest.get(url);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.header(header.getKey(), header.getValue());
        }
        return executeHttpRequest(msg, request);
    }


    private static String executeHttpRequest(String msg, HttpRequest request) {
        request.setConnectionTimeout(TIMEOUT);
        request.contentType("application/json");
        TimeInterval timer = DateUtil.timer();
        HttpResponse resp = request.execute();
        if (!resp.isOk()) {
            throw new HttpException("{}异常,返回状态码:{},内容:{}", msg, resp.getStatus(), resp.body());
        }
        String respBody = resp.body();
        if (StrUtil.isBlank(respBody)) {
            throw new HttpException(msg + "返回结果为空!");
        }
        log.info("{}返回参数: {}", msg, respBody);
        log.info("请求用时:{}ms", timer.interval());
        return respBody;
    }


    public static String httpGetForword(String url, String msg) {
        return httpGetRequest(FORWORD_GET_URL + url, msg);
    }

    public static String httpPostForword(String url, String param, String msg) {
        JSONObject obj = new JSONObject();
        obj.put("param", param);
        obj.put("url", url);
        return httpPostRequest(FORWORD_POST_URL, obj.toJSONString(), msg);
    }
}
