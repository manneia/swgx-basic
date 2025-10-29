package com.manneia.swgx.basic.service.impl.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.manneia.swgx.basic.common.constant.BasicKey;
import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.config.CustomizeUrlConfig;
import com.manneia.swgx.basic.model.request.PushInvoiceDocDetail;
import com.manneia.swgx.basic.model.request.PushInvoiceDocRequest;
import com.manneia.swgx.basic.model.response.PushInvoiceDocResponse;
import com.manneia.swgx.basic.service.invoice.InvoiceDocService;
import com.manneia.swgx.basic.utils.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * @author lkx
 * @description 发票单据服务实现
 * @created 2025-10-29 16:34:54
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceDocServiceImpl implements InvoiceDocService {

    private final HttpUtil httpUtil;
    private final CustomizeUrlConfig customizeUrlConfig;

    private static final String GOODS_SIZE = "1";

    private static final String GOODS_CURRENT = "1";

    @Override
    public SingleResponse<PushInvoiceDocResponse> pushInvoiceDoc(PushInvoiceDocRequest pushInvoiceDocRequest) {
        PushInvoiceDocResponse pushInvoiceDocResponse = new PushInvoiceDocResponse();
        pushInvoiceDocResponse.setDocNo(pushInvoiceDocResponse.getDocNo());
        // 1. 获取企业信息
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_NAME, pushInvoiceDocRequest.getSellerName());
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_REG_NO, pushInvoiceDocRequest.getSellerTaxNo());
        String companyId;
        try {
            String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            JSONObject companyInfoResult = httpUtil.forPost(customizeUrlConfig.getQueryCompanyInfoUrl(), request, JSONObject.class);

            log.info("企业信息:{}", toJSONString(companyInfoResult));
            JSONObject data = companyInfoResult.getJSONObject(BasicKey.INTERFACE_DATA);
            companyId = data.getString(BasicKey.COMPANY_ID);
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", toJSONString(e));
            pushInvoiceDocResponse.setSuccess(false);
            pushInvoiceDocResponse.setErrorMessage("获取企业信息失败");
            return SingleResponse.of(pushInvoiceDocResponse);
        }
        // 2. 组装,单据推送请求参数
        String docNo = pushInvoiceDocRequest.getDocNo();
        String invoiceTypeCode = pushInvoiceDocRequest.getInvoiceTypeCode();
        String invoiceType = pushInvoiceDocRequest.getInvoiceType();
        String sellerName = pushInvoiceDocRequest.getSellerName();
        String sellerTaxNo = pushInvoiceDocRequest.getSellerTaxNo();
        String sellerAddress = pushInvoiceDocRequest.getSellerAddress();
        String sellerTelPhone = pushInvoiceDocRequest.getSellerTelPhone();
        String sellerBankName = pushInvoiceDocRequest.getSellerBankName();
        String sellerBankNumber = pushInvoiceDocRequest.getSellerBankNumber();
        String drawer = pushInvoiceDocRequest.getDrawer();
        BigDecimal invoiceTotalPriceTax = pushInvoiceDocRequest.getInvoiceTotalPriceTax();
        BigDecimal invoiceTotalPrice = pushInvoiceDocRequest.getInvoiceTotalPrice();
        BigDecimal invoiceTotalTax = pushInvoiceDocRequest.getInvoiceTotalTax();
        String remarks = pushInvoiceDocRequest.getRemarks();
        String loginAccount = pushInvoiceDocRequest.getLoginAccount();
        List<PushInvoiceDocDetail> invoiceDocDetails = pushInvoiceDocRequest.getInvoiceDocDetails();

        JSONObject pushInvoiceDocJsonRequest = new JSONObject();
        pushInvoiceDocJsonRequest.put(BasicKey.COMPANY_ID, companyId);
        pushInvoiceDocJsonRequest.put(BasicKey.DOC_NO, docNo);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TYPE_CODE, invoiceTypeCode);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TYPE, invoiceType);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_NAME, sellerName);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_TAX_NO, sellerTaxNo);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_TAX_REG_NO, sellerTaxNo);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_ADDRESS, sellerAddress);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_TEL_PHONE, sellerTelPhone);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_BANK_NAME, sellerBankName);
        pushInvoiceDocJsonRequest.put(BasicKey.SELLER_BANK_NUMBER, sellerBankNumber);
        pushInvoiceDocJsonRequest.put(BasicKey.DRAWER, drawer);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE_TAX, invoiceTotalPriceTax);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE, invoiceTotalPrice);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TOTAL_TAX, invoiceTotalTax);
        pushInvoiceDocJsonRequest.put(BasicKey.LOGIN_ACCOUNT, loginAccount);
        pushInvoiceDocJsonRequest.put(BasicKey.REMARKS, remarks);
        JSONArray goodsList = new JSONArray();
        for (PushInvoiceDocDetail invoiceDocDetail : invoiceDocDetails) {
            //2.1 查询商品信息
            JSONObject queryProductRequest = new JSONObject();
            queryProductRequest.put(BasicKey.COMPANY_ID, companyId);
            queryProductRequest.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
            queryProductRequest.put(BasicKey.SIZE, GOODS_SIZE);
            queryProductRequest.put(BasicKey.CURRENT, GOODS_CURRENT);
            JSONObject productResult = null;
            try {
                String queryProductRequestStr = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(queryProductRequest).getBytes(StandardCharsets.UTF_8));
                productResult = httpUtil.forPost(customizeUrlConfig.getQueryGoodsInfoUrl(), queryProductRequestStr, JSONObject.class);
                log.info("查询商品信息:{}", toJSONString(productResult));
            } catch (Exception e) {
                log.error("查询商品信息失败:{}", toJSONString(e));
                pushInvoiceDocResponse.setSuccess(false);
                pushInvoiceDocResponse.setErrorMessage("查询商品信息失败,接口调用异常");
            }
            if (productResult == null) {
                pushInvoiceDocResponse.setSuccess(false);
                pushInvoiceDocResponse.setErrorMessage("商品编码: " + invoiceDocDetail.getGoodsPersonalCode() + " 在百旺系统不存在,请维护该商品");
                return SingleResponse.of(pushInvoiceDocResponse);
            }
            JSONObject data = productResult.getJSONObject(BasicKey.INTERFACE_DATA);
            JSONArray goodsDetails = data.getJSONArray(BasicKey.RECORDS);
            if (CollUtil.isEmpty(goodsDetails)) {
                pushInvoiceDocResponse.setSuccess(false);
                pushInvoiceDocResponse.setErrorMessage("商品编码: " + invoiceDocDetail.getGoodsPersonalCode() + " 在百旺系统不存在,请维护该商品");
                return SingleResponse.of(pushInvoiceDocResponse);
            }

            for (int i = 0; i < goodsDetails.size(); i++) {
                JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                JSONObject goods = new JSONObject();
                goods.put(BasicKey.GOODS_NAME, goodsDetail.getString(BasicKey.GOODS_NAME));
                goods.put(BasicKey.GOODS_CODE, goodsDetail.getString(BasicKey.GOODS_CODE));
                goods.put(BasicKey.GOODS_SPECIFICATION, goodsDetail.getString(BasicKey.GOODS_SPECIFICATION));
                goods.put(BasicKey.GOODS_UNIT, goodsDetail.getString(BasicKey.GOODS_UNIT));
                goods.put(BasicKey.GOODS_PRICE, goodsDetail.getString(BasicKey.GOODS_PRICE));
                goods.put(BasicKey.GOODS_QUANTITY, invoiceDocDetail.getGoodsQuantity());
                goods.put(BasicKey.GOODS_TOTAL_PRICE_TAX, invoiceDocDetail.getGoodsTotalPriceTax());
                goods.put(BasicKey.GOODS_TOTAL_TAX, invoiceDocDetail.getGoodsTotalTax());
                goods.put(BasicKey.GOODS_TAX_RATE, goodsDetail.getString(BasicKey.VAT_RATE));
                goods.put(BasicKey.INVOICE_ITEM_TYPE, "0");
                goods.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
                goodsList.add(goods);
            }
        }
        pushInvoiceDocJsonRequest.put(BasicKey.PRODUCT_PARAMS, goodsList);
        // 3. 调用单据推送接口
        return pushInvoiceDoc(pushInvoiceDocJsonRequest, pushInvoiceDocResponse);
    }

    private SingleResponse<PushInvoiceDocResponse> pushInvoiceDoc(JSONObject pushInvoiceDocJsonRequest, PushInvoiceDocResponse pushInvoiceDocResponse) {
        try {
            String pushInvoiceDocRequestStr = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(pushInvoiceDocJsonRequest).getBytes(StandardCharsets.UTF_8));
            JSONObject pushDocResult = httpUtil.forPost(customizeUrlConfig.getPushInvoiceDocUrl(), pushInvoiceDocRequestStr, JSONObject.class);
            log.info("推送开票申请并生成开票链接结果:{}", toJSONString(pushDocResult));
            if (pushDocResult != null) {
                String code = pushDocResult.getString(BasicKey.RESPONSE_CODE);
                if (StringUtils.isNotBlank(code) && "0".equals(code)) {
                    pushInvoiceDocResponse.setSuccess(true);
                    pushInvoiceDocResponse.setIssueUrl(pushDocResult.getString(BasicKey.INTERFACE_DATA));
                } else {
                    pushInvoiceDocResponse.setSuccess(false);
                    pushInvoiceDocResponse.setErrorMessage(pushDocResult.getString(BasicKey.ERROR_MESSAGE));
                }
            } else {
                pushInvoiceDocResponse.setSuccess(false);
                pushInvoiceDocResponse.setErrorMessage("推送开票申请并生成开票链接失败,接口调用异常");
            }
            return SingleResponse.of(pushInvoiceDocResponse);
        } catch (Exception e) {
            log.error("推送开票申请并生成开票链接失败:{}", toJSONString(e));
            pushInvoiceDocResponse.setSuccess(false);
            pushInvoiceDocResponse.setErrorMessage("推送开票申请并生成开票链接失败,接口调用异常");
            return SingleResponse.of(pushInvoiceDocResponse);
        }
    }
}
