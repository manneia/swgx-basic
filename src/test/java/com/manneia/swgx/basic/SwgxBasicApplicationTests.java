package com.manneia.swgx.basic;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.manneia.swgx.basic.common.constant.BasicKey;
import com.manneia.swgx.basic.config.CustomizeUrlConfig;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static com.alibaba.fastjson.JSON.toJSONString;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest
class SwgxBasicApplicationTests {

    @Resource
    private BwHttpUtil bwHttpUtil;

    @Resource
    private CustomizeUrlConfig customizeUrlConfig;

    /**
     * 获取公司ID
     */
    @Test
    void getCompanyId() {
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_NAME, "悠乐芳（中国）商贸有限公司");
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_REG_NO, "91310000MA1HNBYL8J");
        String companyId;
        try {
            String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(customizeUrlConfig.getQueryCompanyInfoUrl(), request, "json");
            JSONObject companyInfoResult = JSON.parseObject(response);
            log.info("企业信息:{}", toJSONString(companyInfoResult));
            JSONObject data = companyInfoResult.getJSONObject(BasicKey.INTERFACE_DATA);
            companyId = data.getString(BasicKey.COMPANY_ID);
            log.info("companyId:{}", companyId);
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", toJSONString(e));
            throw new RuntimeException("获取企业信息失败");
        }
    }

    @Test
    void testOcr() {
        // 1. 获取企业信息
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put("nsrmc", "上海申东百旺金赋科技有限公司");
        getCompanyInfoRequest.put("nsrsbh", "91310230MA1K2F7A5C");
        String companyId;
        String getCompanyInfoUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/queryUniqueSign";
        try {
            String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(getCompanyInfoUrl, request, "json");
            JSONObject companyInfoResult = JSON.parseObject(response);
            log.info("企业信息:{}", JSONObject.toJSONString(companyInfoResult));
            JSONObject data = companyInfoResult.getJSONObject("data");
            companyId = data.getString("qyId");
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", JSONObject.toJSONString(e));
            throw new RuntimeException(e);
        }
        // 2. 组装,单据推送请求参数
        JSONObject pushDocRequest = new JSONObject();
        //2.1 查询商品信息
        String queryProductUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/query/goods";
        String pushDocUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/addReceiptUrl";
        JSONObject queryProductRequest = new JSONObject();
        queryProductRequest.put("qyId", companyId);
        queryProductRequest.put("zxbm", "glgybrsd");
        queryProductRequest.put("size", "1");
        queryProductRequest.put("current", "1");
        try {
            String queryProductRequestStr = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(queryProductRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(queryProductUrl, queryProductRequestStr, "json");
            JSONObject productResult = JSON.parseObject(response);
            log.info("查询商品信息:{}", JSONObject.toJSONString(productResult));
        } catch (Exception e) {
            log.error("查询商品信息失败:{}", JSONObject.toJSONString(e));
            throw new RuntimeException(e);
        }
        // 3. 调用单据推送接口
        String pushInvoiceDocUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/v2/applyInvoice";
        JSONObject pushInvoiceDocRequest = new JSONObject();
        pushInvoiceDocRequest.put("qyId", companyId);
        pushInvoiceDocRequest.put("ddlsh", "test11111");
        pushInvoiceDocRequest.put("fplxdm", "02");
        pushInvoiceDocRequest.put("kplx", "0");
        pushInvoiceDocRequest.put("qdbz", "0");
        pushInvoiceDocRequest.put("xsfMc", "上海申东百旺金赋科技有限公司");
        pushInvoiceDocRequest.put("sksbh", "91310230MA1K2F7A5C");
        pushInvoiceDocRequest.put("xsfNsrsbh", "91310230MA1K2F7A5C");
        pushInvoiceDocRequest.put("xsfDz", "上海市黄浦区打浦路1号1504-1506室");
        pushInvoiceDocRequest.put("xsfDh", "13303810001");
        pushInvoiceDocRequest.put("xsfYh", "中国工商银行股份有限公司上海市漕河泾开发区支行");
        pushInvoiceDocRequest.put("xsfZh", "1001266309200437342");
//        pushInvoiceDocRequest.put("gmfMc", "安徽百旺金赋信息科技有限公司");
//        pushInvoiceDocRequest.put("gmfNsrsbh", "913401000557562006");
        pushInvoiceDocRequest.put("kpr", "诸谨瑜");
        pushInvoiceDocRequest.put("jshj", "113");
        pushInvoiceDocRequest.put("hjje", "100.00");
        pushInvoiceDocRequest.put("hjse", "13.00");
        pushInvoiceDocRequest.put("dlzh", "15021700319");
        JSONArray goodsList = new JSONArray();
        JSONObject goods = new JSONObject();
        goods.put("spmc", "格兰冠雅铂瑞思单一麦芽苏格兰威士忌");
        goods.put("spbm", "1030306000000000000");
        goods.put("ggxh", "700ML");
        goods.put("dw", "瓶");
        goods.put("dj", "113");
        goods.put("sl", "1");
        goods.put("je", "113.00");
        goods.put("se", "13");
        goods.put("slv", "13");
        goods.put("fphxz", "0");
        goods.put("zxbm", "glgybrsd");
        goodsList.add(goods);
        pushInvoiceDocRequest.put("prodParam", goodsList);
        try {
            String pushInvoiceDocRequestStr = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(pushInvoiceDocRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(pushDocUrl, pushInvoiceDocRequestStr, "json");
            JSONObject pushDocResult = JSON.parseObject(response);
            log.info("推送开票申请结果:{}", JSONObject.toJSONString(pushDocResult));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void testQueryInvoice() {
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put("nsrmc", "上海申东百旺金赋科技有限公司");
        getCompanyInfoRequest.put("nsrsbh", "91310230MA1K2F7A5C");
        String companyId;
        String getCompanyInfoUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/queryUniqueSign";
        try {
            String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(getCompanyInfoUrl, request, "json");
            JSONObject companyInfoResult = JSON.parseObject(response);
            log.info("企业信息:{}", JSONObject.toJSONString(companyInfoResult));
            JSONObject data = companyInfoResult.getJSONObject("data");
            companyId = data.getString("qyId");
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", JSONObject.toJSONString(e));
            throw new RuntimeException(e);
        }
        String queryInvoiceUrl = "https://swgxs.baiwangjs.com/agentiscloud/cloud/queryInvoiceInfo";
        JSONObject queryRequest = new JSONObject();
        queryRequest.put("djbh", "test11111");
        queryRequest.put("qyId", companyId);
        String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(queryRequest).getBytes(StandardCharsets.UTF_8));
        String response = bwHttpUtil.httpPostRequest(queryInvoiceUrl, request, "json");
        JSONObject invoicInfo = JSON.parseObject(response);
        log.info("企业信息:{}", JSONObject.toJSONString(invoicInfo));
    }

    @Test
    void testDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String invoiceStartDate = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).format(formatter);
        String invoiceEndDate = LocalDateTime.now().format(formatter);
        log.info("开始时间:{} 结束时间:{}", invoiceStartDate, invoiceEndDate);
    }

}
