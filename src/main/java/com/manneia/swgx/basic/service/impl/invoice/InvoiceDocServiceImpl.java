package com.manneia.swgx.basic.service.impl.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.manneia.swgx.basic.common.constant.BasicKey;
import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.config.CustomizeUrlConfig;
import com.manneia.swgx.basic.exception.BizErrorCode;
import com.manneia.swgx.basic.exception.BizException;
import com.manneia.swgx.basic.model.request.PushInvoiceDocDetail;
import com.manneia.swgx.basic.model.request.PushInvoiceDocRequest;
import com.manneia.swgx.basic.model.response.PushInvoiceDocResponse;
import com.manneia.swgx.basic.service.invoice.InvoiceDocService;
import com.manneia.swgx.basic.utils.BwHttpUtil;
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
import java.util.concurrent.atomic.AtomicReference;
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

    private final CustomizeUrlConfig customizeUrlConfig;

    private final BwHttpUtil httpUtil;

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
            String response = httpUtil.httpPostRequest(customizeUrlConfig.getQueryCompanyInfoUrl(), request, "json");
            JSONObject companyInfoResult = JSON.parseObject(response);
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
        pushInvoiceDocJsonRequest.put(BasicKey.BIZ_DOC_TYPE, "invoice_issue");
        JSONArray goodsList = new JSONArray();
        // 原价商品列表
        AtomicReference<BigDecimal> originalGoodsTotalPrice = new AtomicReference<>(BigDecimal.ZERO);
        // 计算折扣商品列表
        JSONArray discountGoodsList = new JSONArray();
        AtomicReference<BigDecimal> discountGoodsTotalPrice = new AtomicReference<>(BigDecimal.ZERO);
        for (PushInvoiceDocDetail invoiceDocDetail : invoiceDocDetails) {
            //2.1 查询商品信息
            JSONObject queryProductRequest = new JSONObject();
            queryProductRequest.put(BasicKey.COMPANY_ID, companyId);
            queryProductRequest.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
            queryProductRequest.put(BasicKey.SIZE, GOODS_SIZE);
            queryProductRequest.put(BasicKey.CURRENT, GOODS_CURRENT);
            JSONObject productResult = getGoodsInfo(queryProductRequest, pushInvoiceDocResponse);
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
            handlerGoodsList(goodsDiscountType, invoiceDocDetail, goodsList, goodsDetails, discountGoodsList, originalGoodsTotalPrice, discountGoodsTotalPrice);
        }
        // 处理计算折扣商品
        if (CollUtil.isNotEmpty(discountGoodsList)) {
            // 减去原价商品
            BigDecimal discountedAmount = invoiceTotalPriceTax.subtract(originalGoodsTotalPrice.get());
            BigDecimal discountPrice = discountedAmount.subtract(discountGoodsTotalPrice.get());
            BigDecimal discountRate = discountPrice.divide(discountGoodsTotalPrice.get(), 7, RoundingMode.HALF_UP);
            // 根据折扣率处理待折扣商品，生成折扣行，折扣行金额为负数，fphxz为1，折扣额为含税金额*折扣率，，被折扣行金额为整数，fphxz为2
            Map<String, PushInvoiceDocDetail> docDetailMap = invoiceDocDetails.stream().collect(Collectors.toMap(PushInvoiceDocDetail::getGoodsPersonalCode, invoiceDocDetail -> invoiceDocDetail));
            try {
                for (int i = 0; i < discountGoodsList.size(); i++) {
                    JSONObject goodInfo = discountGoodsList.getJSONObject(i);
                    String goodsZxbm = goodInfo.getString(BasicKey.GOODS_PERSONAL_CODE);
                    PushInvoiceDocDetail pushInvoiceDocDetail = docDetailMap.get(goodsZxbm);
                    // 创建折扣行（fphxz=1，金额为负数）
                    JSONObject discountLine = getDiscountGoodsLine(pushInvoiceDocDetail, goodInfo, discountRate);
                    if (goodsList.isEmpty()) {
                        discountLine.put(BasicKey.SERIAL_NUM, 1);
                    } else {
                        discountLine.put(BasicKey.SERIAL_NUM, goodsList.size() + 1);
                    }
                    goodsList.add(discountLine);
                    // 创建被折扣行（fphxz=2，金额为正数）
                    JSONObject originalGoodsLine = getOriginalGoodsLine(pushInvoiceDocDetail, goodInfo);
                    if (goodsList.isEmpty()) {
                        originalGoodsLine.put(BasicKey.SERIAL_NUM, 1);
                    } else {
                        originalGoodsLine.put(BasicKey.SERIAL_NUM, goodsList.size() + 1);
                    }
                    goodsList.add(originalGoodsLine);
                }
            } catch (Exception e) {
                log.error("折扣行处理失败:{}", toJSONString(e));
                throw new BizException("折扣行处理失败", BizErrorCode.DISCOUNT_GOODS_LIST_HANDLER_ERROR);
            }
        }
        pushInvoiceDocJsonRequest.put(BasicKey.PRODUCT_PARAMS, goodsList);
        BigDecimal totalTax = BigDecimal.ZERO;
        for (int i = 0; i < goodsList.size(); i++) {
            JSONObject good = goodsList.getJSONObject(i);
            String fphxz = good.getString(BasicKey.INVOICE_ITEM_TYPE);
            if ("0".equals(fphxz)) {
                totalTax = totalTax.add(good.getBigDecimal(BasicKey.GOODS_TOTAL_TAX));
            }
            if ("1".equals(fphxz)) {
                totalTax = totalTax.add(good.getBigDecimal(BasicKey.GOODS_TOTAL_TAX));
            }
            if ("2".equals(fphxz)) {
                totalTax = totalTax.add(good.getBigDecimal(BasicKey.GOODS_TOTAL_TAX));
            }
        }
        BigDecimal totalAmount = invoiceTotalPriceTax.subtract(totalTax);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE, totalAmount);
        pushInvoiceDocJsonRequest.put(BasicKey.INVOICE_TOTAL_TAX, totalTax);
        // 3. 调用单据推送接口
        log.info("推送开票申请并生成开票链接请求:{}", toJSONString(pushInvoiceDocJsonRequest));
        return pushInvoiceDoc(pushInvoiceDocJsonRequest, pushInvoiceDocResponse);
    }

    private void handlerGoodsList(String goodsDiscountType, PushInvoiceDocDetail invoiceDocDetail, JSONArray goodsList, JSONArray goodsDetails, JSONArray discountGoodsList, AtomicReference<BigDecimal> originalGoodsTotalPrice, AtomicReference<BigDecimal> discountGoodsTotalPrice) {

        switch (goodsDiscountType) {
            case "0":
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    BigDecimal goodsPrice = goodsDetail.getBigDecimal(BasicKey.GOODS_PRICE);
                    BigDecimal goodsPriceExcludeTax = goodsPrice.divide(BigDecimal.ONE.add(goodsDetail.getBigDecimal(BasicKey.VAT_RATE)), RoundingMode.HALF_UP);
                    BigDecimal goodsTotalAmountTax = goodsPrice.multiply(invoiceDocDetail.getGoodsQuantity());
                    originalGoodsTotalPrice.set(originalGoodsTotalPrice.get().add(goodsTotalAmountTax));
                    JSONObject goods = new JSONObject();
                    handlerDiscountInfo(invoiceDocDetail, goodsDetail, goods);
                    if (goodsList.isEmpty()) {
                        goods.put(BasicKey.SERIAL_NUM, 1);
                    } else {
                        goods.put(BasicKey.SERIAL_NUM, goodsList.size() + 1);
                    }
                    goods.put(BasicKey.GOODS_TOTAL_PRICE_TAX, goodsTotalAmountTax);
                    goods.put(BasicKey.GOODS_TOTAL_TAX, goodsTotalAmountTax.subtract(goodsPriceExcludeTax.multiply(invoiceDocDetail.getGoodsQuantity())));
                    goods.put(BasicKey.GOODS_TAX_RATE, goodsDetail.getString(BasicKey.VAT_RATE));
                    goods.put(BasicKey.INVOICE_ITEM_TYPE, "0");
                    goods.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
                    goodsList.add(goods);
                }
                break;
            case "1":
                log.info("商品编码:{}, 折扣类型为满减,不处理", invoiceDocDetail.getGoodsPersonalCode());
                break;
            case "2":
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    BigDecimal goodsPrice = goodsDetail.getBigDecimal(BasicKey.GOODS_PRICE);
                    BigDecimal goodsTotalAmountTax = goodsPrice.multiply(invoiceDocDetail.getGoodsQuantity());
                    discountGoodsTotalPrice.set(discountGoodsTotalPrice.get().add(goodsTotalAmountTax));
                    discountGoodsList.add(goodsDetail);
                }
                break;
            default:
                throw new BizException("商品折扣类型错误", BizErrorCode.GOODS_DISCOUNT_TYPE_ERROR);
        }
    }

    private JSONObject getGoodsInfo(JSONObject queryProductRequest, PushInvoiceDocResponse pushInvoiceDocResponse) {
        JSONObject productResult = null;
        try {
            log.info("查询商品信息:{}", toJSONString(queryProductRequest));
            String queryProductRequestStr = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(queryProductRequest).getBytes(StandardCharsets.UTF_8));
            String response = httpUtil.httpPostRequest(customizeUrlConfig.getQueryGoodsInfoUrl(), queryProductRequestStr, "json");
            productResult = JSON.parseObject(response);
            log.info("查询商品信息:{}", toJSONString(productResult));
        } catch (Exception e) {
            log.error("查询商品信息失败:{}", toJSONString(e));
            pushInvoiceDocResponse.setSuccess(false);
            pushInvoiceDocResponse.setErrorMessage("查询商品信息失败,接口调用异常");
        }
        return productResult;
    }

    private static JSONObject getDiscountGoodsLine(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo, BigDecimal discountRate) {
        JSONObject discountLine = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodInfo, discountLine);
        goodInfo.remove(BasicKey.GOODS_PRICE);
        goodInfo.remove(BasicKey.GOODS_QUANTITY);
        BigDecimal goodsPrice = goodInfo.getBigDecimal(BasicKey.GOODS_PRICE);
        // 金额为负数
        BigDecimal lineDiscountPrice;
        if (StringUtils.isNotBlank(goodsPrice.toString())) {
            lineDiscountPrice = goodsPrice
                    .multiply(invoiceDocDetail.getGoodsQuantity())
                    .multiply(discountRate)
                    .setScale(2, RoundingMode.HALF_UP);
            discountLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, lineDiscountPrice);
        } else {
            throw new BizException("商品单价未维护", BizErrorCode.GOODS_PRICE_NOT_NULL);
        }

        // 计算折扣税额，保持负数
        BigDecimal discountTax = lineDiscountPrice
                .divide(BigDecimal.ONE.add(goodInfo.getBigDecimal(BasicKey.VAT_RATE)), 2, RoundingMode.HALF_UP)
                .multiply(goodInfo.getBigDecimal(BasicKey.VAT_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        // 税额也为负数
        discountLine.put(BasicKey.GOODS_TOTAL_TAX, discountTax);
        discountLine.put(BasicKey.GOODS_TAX_RATE, goodInfo.getString(BasicKey.VAT_RATE));
        // 折扣行标识
        discountLine.put(BasicKey.INVOICE_ITEM_TYPE, "1");
        discountLine.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return discountLine;
    }


    private static JSONObject getOriginalGoodsLine(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo) {
        JSONObject originalGoodsLine = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodInfo, originalGoodsLine);
        BigDecimal goodsPrice = goodInfo.getBigDecimal(BasicKey.GOODS_PRICE);
        BigDecimal goodsTotalAmountTax = goodsPrice.multiply(invoiceDocDetail.getGoodsQuantity());
        originalGoodsLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, goodsTotalAmountTax);
        // 税额为正数
        BigDecimal goodsTotalAmount = goodsTotalAmountTax.divide(BigDecimal.ONE.add(new BigDecimal(goodInfo.getString(BasicKey.VAT_RATE))), 2, RoundingMode.HALF_UP);
        originalGoodsLine.put(BasicKey.GOODS_TOTAL_TAX, goodsTotalAmountTax.subtract(goodsTotalAmount));
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
            String response = httpUtil.httpPostRequest(customizeUrlConfig.getPushInvoiceDocUrl(), pushInvoiceDocRequestStr, "json");
            JSONObject pushDocResult = JSON.parseObject(response);
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
