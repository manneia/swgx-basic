package com.manneia.swgx.basic.utils;


import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.alibaba.fastjson.util.IOUtils.UTF8;


/**
 * @author xhq
 * @Description
 * @Date: 2023/6/17
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
//    public static String ak_id = "ThoyBgfU40tWYjafR4ewu2b8";
//    public static String ak_secret = "gfU7snUoANL5yU4fRRB1cqyEHYGErS";

    @Value("${swgx.http.ak_id}")
    private String ak_id;

    @Value("${swgx.http.ak_secret}")
    private String ak_secret;


    public String httpPostRequest(String url, String params, String ContentType) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;
        try {
            //  httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            StringEntity entity = new StringEntity(params, "utf-8");
            httpPost.setEntity(entity);
            if ("json".equals(ContentType)) {
                httpPost.setHeader("Content-type", "application/json");
            } else if ("multipart/form-data".equals(ContentType)) {
                httpPost.setHeader("Content-type", "multipart/form-data");
            } else {
                httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            }
            httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
//            HtjsApiSdkClient hsc = new HtjsApiSdkClient("网关KEY", "网关SECRET");
            HtjsApiSdkClient hsc = new HtjsApiSdkClient(ak_id, ak_secret);

            Map header = hsc.get_signed_headers("POST", url);
            Set keyset = header.keySet();
            Iterator it = keyset.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                httpPost.setHeader(key, (String) header.get(key));
                log.info("key值：" + key + "对应值：" + (String) header.get(key));
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


    public String doPostFormDataFile(String url, File file, Object data) {
        long start = System.currentTimeMillis();
        // 创建Http实例
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建HttpPost实例
        HttpPost httpPost = new HttpPost(url);

        // 请求参数配置
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000)
                .setConnectionRequestTimeout(10000).build();
        httpPost.setConfig(requestConfig);

        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));

            // 添加文件参数
            FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
            builder.addPart("file", fileBody);


            StringBody modelBody = new StringBody(JSON.toJSONString(data), ContentType.APPLICATION_JSON);
            builder.addPart("params", modelBody);


            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            setHttpHeader(httpPost, "POST", url);
            HttpResponse response = httpClient.execute(httpPost);// 执行提交

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 返回
                String res = EntityUtils.toString(response.getEntity(), java.nio.charset.Charset.forName("UTF-8"));
                return res;
            } else {
                String errMsg = EntityUtils.toString(response.getEntity(), UTF8);
                String msg = String.format("Http请求返回错误，错误代码：%s,错误原因：%s，耗时：", response.getStatusLine().getStatusCode(), errMsg, System.currentTimeMillis() - start);
                log.error(msg);
                throw new RuntimeException(msg);
            }

        } catch (Exception e) {
            throw new RuntimeException("请求失败:"+e.getMessage());
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setHttpHeader(HttpRequestBase httBase, String method, String url) throws HtjsApiSdkError {
        httBase.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        HtjsApiSdkClient hsc = new HtjsApiSdkClient(ak_id, ak_secret);
        Map header = hsc.get_signed_headers(method, url);
        Set keyset = header.keySet();
        Iterator it = keyset.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            httBase.setHeader(key, (String) header.get(key));
        }
    }

    /**
     * 发送GET请求获取字符串响应
     */
    public String doGet(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }

    /**
     * 发送GET请求获取字节数组响应
     */
    public byte[] doGetBytes(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

}
