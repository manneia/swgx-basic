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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // 原价商品列表
        BigDecimal originalGoodsTotalPrice = BigDecimal.ZERO;
        // 计算折扣商品列表
        JSONArray discountGoodsList = new JSONArray();
        BigDecimal discountGoodsTotalPrice = BigDecimal.ZERO;
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
            String goodsDiscountType = invoiceDocDetail.getGoodsDiscountType();
            if ("0".equals(goodsDiscountType)) {
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    originalGoodsTotalPrice = originalGoodsTotalPrice.add(invoiceDocDetail.getGoodsTotalPriceTax());
                    JSONObject goods = new JSONObject();
                    handlerDiscountInfo(invoiceDocDetail, goodsDetail, goods);
                    goods.put(BasicKey.GOODS_TOTAL_PRICE_TAX, invoiceDocDetail.getGoodsTotalPriceTax());
                    goods.put(BasicKey.GOODS_TOTAL_TAX, invoiceDocDetail.getGoodsTotalTax());
                    goods.put(BasicKey.GOODS_TAX_RATE, goodsDetail.getString(BasicKey.VAT_RATE));
                    goods.put(BasicKey.INVOICE_ITEM_TYPE, "0");
                    goods.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
                    goodsList.add(goods);
                }
            }
            if ("2".equals(goodsDiscountType)) {
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    discountGoodsTotalPrice = discountGoodsTotalPrice.add(new BigDecimal(goodsDetail.getString(BasicKey.GOODS_PRICE)));
                    discountGoodsList.add(goodsDetail);
                }
            }
        }
        // 处理计算折扣商品
        if (CollUtil.isNotEmpty(discountGoodsList)) {
            // 减去原价商品
            BigDecimal discountedAmount = invoiceTotalPriceTax.subtract(originalGoodsTotalPrice);
            BigDecimal discountPrice = discountedAmount.subtract(discountGoodsTotalPrice);
            BigDecimal discountRate = discountPrice.divide(discountGoodsTotalPrice, 7, RoundingMode.HALF_UP);
            // 根据折扣率处理待折扣商品，生成折扣行，折扣行金额为负数，fphxz为1，折扣额为含税金额*折扣率，，被折扣行金额为整数，fphxz为2
            Map<String, PushInvoiceDocDetail> docDetailMap = invoiceDocDetails.stream().collect(Collectors.toMap(PushInvoiceDocDetail::getGoodsPersonalCode, invoiceDocDetail -> invoiceDocDetail));
            for (int i = 0; i < discountGoodsList.size(); i++) {
                JSONObject goodInfo = discountGoodsList.getJSONObject(i);
                String goodsZxbm = goodInfo.getString(BasicKey.GOODS_PERSONAL_CODE);
                PushInvoiceDocDetail pushInvoiceDocDetail = docDetailMap.get(goodsZxbm);
                // 创建折扣行（fphxz=1，金额为负数）
                JSONObject discountLine = getDiscountGoodsLine(discountPrice, pushInvoiceDocDetail, goodInfo, discountRate);
                goodsList.add(discountLine);
                // 创建被折扣行（fphxz=2，金额为正数）
                JSONObject originalGoodsLine = getOriginalGoodsLine(discountedAmount, pushInvoiceDocDetail, goodInfo, pushInvoiceDocDetail.getGoodsTotalPriceTax(), discountRate);
                goodsList.add(originalGoodsLine);
            }
        }
        pushInvoiceDocJsonRequest.put(BasicKey.PRODUCT_PARAMS, goodsList);
        // 3. 调用单据推送接口
        return pushInvoiceDoc(pushInvoiceDocJsonRequest, pushInvoiceDocResponse);
    }

    private static JSONObject getDiscountGoodsLine(BigDecimal discountPrice, PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo, BigDecimal discountRate) {
        JSONObject discountLine = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodInfo, discountLine);

        // 金额为负数
        discountLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, discountPrice);

        // 计算折扣税额，保持负数
        BigDecimal discountTax = discountPrice
                .multiply(new BigDecimal(goodInfo.getString(BasicKey.VAT_RATE)))
                .setScale(2, RoundingMode.HALF_UP);
        // 税额也为负数
        discountLine.put(BasicKey.GOODS_TOTAL_TAX, discountTax.negate());
        discountLine.put(BasicKey.GOODS_TAX_RATE, goodInfo.getString(BasicKey.VAT_RATE));
        // 折扣行标识
        discountLine.put(BasicKey.INVOICE_ITEM_TYPE, "1");
        discountLine.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return discountLine;
    }


    private static JSONObject getOriginalGoodsLine(BigDecimal discountedAmount, PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo, BigDecimal originalPriceTax, BigDecimal discountRate) {
        JSONObject originalGoodsLine = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodInfo, originalGoodsLine);

        originalGoodsLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, discountedAmount);
        // 税额为正数
        BigDecimal originalTax = discountedAmount
                .multiply(new BigDecimal(goodInfo.getString(BasicKey.VAT_RATE)))
                .setScale(2, RoundingMode.HALF_UP);
        originalGoodsLine.put(BasicKey.GOODS_TOTAL_TAX, originalTax);
        originalGoodsLine.put(BasicKey.GOODS_TAX_RATE, goodInfo.getString(BasicKey.VAT_RATE));
        // 被折扣行标识
        originalGoodsLine.put(BasicKey.INVOICE_ITEM_TYPE, "2");
        originalGoodsLine.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return originalGoodsLine;
    }

    private static void handlerDiscountInfo(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo, JSONObject originalGoodsLine) {
        originalGoodsLine.put(BasicKey.GOODS_NAME, goodInfo.getString(BasicKey.GOODS_NAME));
        originalGoodsLine.put(BasicKey.GOODS_CODE, goodInfo.getString(BasicKey.GOODS_CODE));
        originalGoodsLine.put(BasicKey.GOODS_SPECIFICATION, goodInfo.getString(BasicKey.GOODS_SPECIFICATION));
        originalGoodsLine.put(BasicKey.GOODS_UNIT, goodInfo.getString(BasicKey.GOODS_UNIT));
        originalGoodsLine.put(BasicKey.GOODS_PRICE, goodInfo.getString(BasicKey.GOODS_PRICE));
        originalGoodsLine.put(BasicKey.GOODS_QUANTITY, invoiceDocDetail.getGoodsQuantity());
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
