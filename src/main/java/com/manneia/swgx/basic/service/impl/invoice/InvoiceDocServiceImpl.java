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
    public SingleResponse<PushInvoiceDocResponse> pushInvoiceDoc(PushInvoiceDocRequest request) {
        PushInvoiceDocResponse response = new PushInvoiceDocResponse();
        response.setDocNo(request.getDocNo());

        // 1. 获取企业信息
        String companyId = getCompanyId(request, response);
        if (companyId == null) {
            return SingleResponse.of(response);
        }

        // 2. 构建请求
        JSONObject pushRequest = buildPushRequest(request, companyId);
        JSONArray goodsList = new JSONArray();
        JSONArray discountGoodsList = new JSONArray();
        AtomicReference<BigDecimal> originalGoodsTotalPrice = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> discountGoodsTotalPrice = new AtomicReference<>(BigDecimal.ZERO);

        // 3. 处理单据明细
        if (!processInvoiceDetails(request.getInvoiceDocDetails(), companyId, goodsList,
                discountGoodsList, originalGoodsTotalPrice, discountGoodsTotalPrice, response)) {
            return SingleResponse.of(response);
        }

        // 4. 处理折扣商品
        if (CollUtil.isNotEmpty(discountGoodsList)) {
            BigDecimal discountRate = calculateDiscountRate(request, originalGoodsTotalPrice.get(), discountGoodsTotalPrice.get());
            processDiscountGoods(discountGoodsList, discountRate,
                    request.getInvoiceDocDetails().stream()
                            .collect(Collectors.toMap(PushInvoiceDocDetail::getGoodsPersonalCode, d -> d)),
                    goodsList);
        }

        // 5. 计算总税额并推送
        calculateAndSetTotalTax(goodsList, request, pushRequest);
        pushRequest.put(BasicKey.PRODUCT_PARAMS, goodsList);
        log.info("推送开票申请并生成开票链接请求:{}", toJSONString(pushRequest));
        return pushInvoiceDoc(pushRequest, response);
    }

    /**
     * 计算税额
     * 公式: 税额 = 含税金额 × 税率 / (1 + 税率)
     */
    private static BigDecimal calculateTaxAmount(BigDecimal taxAmount, String taxRate) {
        if (taxAmount == null || taxRate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = new BigDecimal(taxRate);
        return taxAmount.multiply(rate).divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
    }

    private void handlerGoodsList(String goodsDiscountType, PushInvoiceDocDetail invoiceDocDetail, JSONArray goodsList, JSONArray goodsDetails, JSONArray discountGoodsList, AtomicReference<BigDecimal> originalGoodsTotalPrice, AtomicReference<BigDecimal> discountGoodsTotalPrice) {

        switch (goodsDiscountType) {
            case "0":
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    BigDecimal goodsPrice = goodsDetail.getBigDecimal(BasicKey.GOODS_PRICE);
                    BigDecimal goodsTotalAmountTax = goodsPrice.multiply(invoiceDocDetail.getGoodsQuantity());
                    originalGoodsTotalPrice.set(originalGoodsTotalPrice.get().add(goodsTotalAmountTax));
                    String taxRate = goodsDetail.getString(BasicKey.VAT_RATE);
                    JSONObject goods = buildGoodsLine(invoiceDocDetail, goodsDetail, goodsTotalAmountTax, taxRate, "0");
                    setSerialNum(goods, goodsList);
                    goodsList.add(goods);
                }
                break;
            case "1":
                log.info("商品编码:{}, 折扣类型为满减,不处理", invoiceDocDetail.getGoodsPersonalCode());
                for (int i = 0; i < goodsDetails.size(); i++) {
                    JSONObject goodsDetail = goodsDetails.getJSONObject(i);
                    BigDecimal goodsPrice = goodsDetail.getBigDecimal(BasicKey.GOODS_PRICE);
                    BigDecimal goodsTotalAmountTax = goodsPrice.multiply(invoiceDocDetail.getGoodsQuantity());
                    String taxRate = goodsDetail.getString(BasicKey.VAT_RATE);
                    BigDecimal goodsTax = calculateTaxAmount(goodsTotalAmountTax, taxRate);

                    JSONObject normalGoods = buildGoodsLine(invoiceDocDetail, goodsDetail, goodsTotalAmountTax, taxRate, "2");
                    setSerialNum(normalGoods, goodsList);
                    goodsList.add(normalGoods);

                    JSONObject discountGoods = buildDiscountLine(invoiceDocDetail, goodsDetail, goodsTotalAmountTax, goodsTax, taxRate);
                    setSerialNum(discountGoods, goodsList);
                    goodsList.add(discountGoods);
                }
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

    private static JSONObject buildGoodsLine(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodsDetail, BigDecimal totalAmountTax, String taxRate, String invoiceItemType) {
        JSONObject goods = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodsDetail, goods);
        goods.put(BasicKey.GOODS_TOTAL_PRICE_TAX, totalAmountTax);
        goods.put(BasicKey.GOODS_TOTAL_TAX, calculateTaxAmount(totalAmountTax, taxRate));
        goods.put(BasicKey.GOODS_TAX_RATE, taxRate);
        goods.put(BasicKey.INVOICE_ITEM_TYPE, invoiceItemType);
        goods.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return goods;
    }

    private static JSONObject buildDiscountLine(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodsDetail, BigDecimal totalAmountTax, BigDecimal goodsTax, String taxRate) {
        JSONObject discountGoods = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodsDetail, discountGoods);
        discountGoods.remove(BasicKey.GOODS_SPECIFICATION);
        discountGoods.remove(BasicKey.GOODS_UNIT);
        discountGoods.remove(BasicKey.GOODS_PRICE);
        discountGoods.remove(BasicKey.GOODS_QUANTITY);
        discountGoods.put(BasicKey.GOODS_TOTAL_PRICE_TAX, totalAmountTax.negate());
        discountGoods.put(BasicKey.GOODS_TOTAL_TAX, goodsTax.negate());
        discountGoods.put(BasicKey.GOODS_TAX_RATE, taxRate);
        discountGoods.put(BasicKey.INVOICE_ITEM_TYPE, "1");
        discountGoods.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return discountGoods;
    }

    private static void setSerialNum(JSONObject goods, JSONArray goodsList) {
        goods.put(BasicKey.SERIAL_NUM, goodsList.isEmpty() ? 1 : goodsList.size() + 1);
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
        discountLine.remove(BasicKey.GOODS_SPECIFICATION);
        discountLine.remove(BasicKey.GOODS_UNIT);
        discountLine.remove(BasicKey.GOODS_PRICE);
        discountLine.remove(BasicKey.GOODS_QUANTITY);

        BigDecimal goodsPrice = goodInfo.getBigDecimal(BasicKey.GOODS_PRICE);
        if (goodsPrice == null) {
            throw new BizException("商品单价未维护", BizErrorCode.GOODS_PRICE_NOT_NULL);
        }

        // 金额为负数
        BigDecimal lineDiscountPrice = goodsPrice
                .multiply(invoiceDocDetail.getGoodsQuantity())
                .multiply(discountRate)
                .setScale(2, RoundingMode.HALF_UP)
                .negate();
        discountLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, lineDiscountPrice);

        // 使用统一的税额计算方法（lineDiscountPrice 已为负数）
        String taxRate = goodInfo.getString(BasicKey.VAT_RATE);
        discountLine.put(BasicKey.GOODS_TOTAL_TAX, calculateTaxAmount(lineDiscountPrice, taxRate));
        discountLine.put(BasicKey.GOODS_TAX_RATE, taxRate);
        // 折扣行标识
        discountLine.put(BasicKey.INVOICE_ITEM_TYPE, "1");
        discountLine.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
        return discountLine;
    }

    private static JSONObject getOriginalGoodsLine(PushInvoiceDocDetail invoiceDocDetail, JSONObject goodInfo) {
        JSONObject originalGoodsLine = new JSONObject();
        handlerDiscountInfo(invoiceDocDetail, goodInfo, originalGoodsLine);
        BigDecimal goodsTotalAmountTax = goodInfo.getBigDecimal(BasicKey.GOODS_PRICE).multiply(invoiceDocDetail.getGoodsQuantity());
        String taxRate = goodInfo.getString(BasicKey.VAT_RATE);

        originalGoodsLine.put(BasicKey.GOODS_TOTAL_PRICE_TAX, goodsTotalAmountTax);
        originalGoodsLine.put(BasicKey.GOODS_TOTAL_TAX, calculateTaxAmount(goodsTotalAmountTax, taxRate));
        originalGoodsLine.put(BasicKey.GOODS_TAX_RATE, taxRate);
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

    /**
     * 获取企业ID
     */
    private String getCompanyId(PushInvoiceDocRequest request, PushInvoiceDocResponse response) {
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_NAME, request.getSellerName());
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_REG_NO, request.getSellerTaxNo());
        try {
            String req = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            String resp = httpUtil.httpPostRequest(customizeUrlConfig.getQueryCompanyInfoUrl(), req, "json");
            JSONObject companyInfoResult = JSON.parseObject(resp);
            log.info("企业信息:{}", toJSONString(companyInfoResult));
            return companyInfoResult.getJSONObject(BasicKey.INTERFACE_DATA).getString(BasicKey.COMPANY_ID);
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", toJSONString(e));
            response.setSuccess(false);
            response.setErrorMessage("获取企业信息失败");
            return null;
        }
    }

    /**
     * 构建推送请求JSON
     */
    private JSONObject buildPushRequest(PushInvoiceDocRequest request, String companyId) {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put(BasicKey.COMPANY_ID, companyId);
        jsonRequest.put(BasicKey.DOC_NO, request.getDocNo());
        jsonRequest.put(BasicKey.INVOICE_TYPE_CODE, request.getInvoiceTypeCode());
        jsonRequest.put(BasicKey.INVOICE_TYPE, request.getInvoiceType());
        jsonRequest.put(BasicKey.SELLER_NAME, request.getSellerName());
        jsonRequest.put(BasicKey.SELLER_TAX_NO, request.getSellerTaxNo());
        jsonRequest.put(BasicKey.SELLER_TAX_REG_NO, request.getSellerTaxNo());
        jsonRequest.put(BasicKey.SELLER_ADDRESS, request.getSellerAddress());
        jsonRequest.put(BasicKey.SELLER_TEL_PHONE, request.getSellerTelPhone());
        jsonRequest.put(BasicKey.SELLER_BANK_NAME, request.getSellerBankName());
        jsonRequest.put(BasicKey.SELLER_BANK_NUMBER, request.getSellerBankNumber());
        jsonRequest.put(BasicKey.DRAWER, request.getDrawer());
        jsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE_TAX, request.getInvoiceTotalPriceTax());
        jsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE, request.getInvoiceTotalPrice());
        jsonRequest.put(BasicKey.INVOICE_TOTAL_TAX, request.getInvoiceTotalTax());
        jsonRequest.put(BasicKey.LOGIN_ACCOUNT, request.getLoginAccount());
        jsonRequest.put(BasicKey.REMARKS, request.getRemarks());
        jsonRequest.put(BasicKey.BIZ_DOC_TYPE, "invoice_issue");
        return jsonRequest;
    }

    /**
     * 处理单据明细
     */
    private boolean processInvoiceDetails(List<PushInvoiceDocDetail> invoiceDocDetails,
                                          String companyId,
                                          JSONArray goodsList,
                                          JSONArray discountGoodsList,
                                          AtomicReference<BigDecimal> originalGoodsTotalPrice,
                                          AtomicReference<BigDecimal> discountGoodsTotalPrice,
                                          PushInvoiceDocResponse response) {
        for (PushInvoiceDocDetail invoiceDocDetail : invoiceDocDetails) {
            JSONObject queryProductRequest = new JSONObject();
            queryProductRequest.put(BasicKey.COMPANY_ID, companyId);
            queryProductRequest.put(BasicKey.GOODS_PERSONAL_CODE, invoiceDocDetail.getGoodsPersonalCode());
            queryProductRequest.put(BasicKey.SIZE, GOODS_SIZE);
            queryProductRequest.put(BasicKey.CURRENT, GOODS_CURRENT);
            JSONObject productResult = getGoodsInfo(queryProductRequest, response);
            if (productResult == null) {
                response.setSuccess(false);
                response.setErrorMessage("商品编码: " + invoiceDocDetail.getGoodsPersonalCode() + " 在百旺系统不存在,请维护该商品");
                return false;
            }
            JSONObject data = productResult.getJSONObject(BasicKey.INTERFACE_DATA);
            JSONArray goodsDetails = data.getJSONArray(BasicKey.RECORDS);
            if (CollUtil.isEmpty(goodsDetails)) {
                response.setSuccess(false);
                response.setErrorMessage("商品编码: " + invoiceDocDetail.getGoodsPersonalCode() + " 在百旺系统不存在,请维护该商品");
                return false;
            }
            String goodsDiscountType = invoiceDocDetail.getGoodsDiscountType();
            handlerGoodsList(goodsDiscountType, invoiceDocDetail, goodsList, goodsDetails, discountGoodsList, originalGoodsTotalPrice, discountGoodsTotalPrice);
        }
        return true;
    }

    /**
     * 计算折扣率
     */
    private BigDecimal calculateDiscountRate(PushInvoiceDocRequest request,
                                              BigDecimal originalGoodsTotalPrice,
                                              BigDecimal discountGoodsTotalPrice) {
        BigDecimal discountedAmount = request.getInvoiceTotalPriceTax().subtract(originalGoodsTotalPrice);
        return discountedAmount.divide(discountGoodsTotalPrice, 7, RoundingMode.HALF_UP);
    }

    /**
     * 处理折扣商品
     */
    private void processDiscountGoods(JSONArray discountGoodsList,
                                       BigDecimal discountRate,
                                       Map<String, PushInvoiceDocDetail> docDetailMap,
                                       JSONArray goodsList) {
        if (discountRate.compareTo(BigDecimal.ONE) < 0) {
            log.info("处理计算折扣商品");
            try {
                for (int i = 0; i < discountGoodsList.size(); i++) {
                    JSONObject goodInfo = discountGoodsList.getJSONObject(i);
                    String goodsZxbm = goodInfo.getString(BasicKey.GOODS_PERSONAL_CODE);
                    PushInvoiceDocDetail pushInvoiceDocDetail = docDetailMap.get(goodsZxbm);
                    // 创建被折扣行（fphxz=2，金额为正数）
                    JSONObject originalGoodsLine = getOriginalGoodsLine(pushInvoiceDocDetail, goodInfo);
                    setSerialNum(originalGoodsLine, goodsList);
                    goodsList.add(originalGoodsLine);
                    // 创建折扣行（fphxz=1，金额为负数）
                    JSONObject discountLine = getDiscountGoodsLine(pushInvoiceDocDetail, goodInfo, discountRate);

                    // 判断折扣金额是否为0，为0则不添加折扣行
                    BigDecimal discountAmount = discountLine.getBigDecimal(BasicKey.GOODS_TOTAL_PRICE_TAX);
                    if (discountAmount.compareTo(BigDecimal.ZERO) != 0) {
                        setSerialNum(discountLine, goodsList);
                        goodsList.add(discountLine);
                        log.info("商品编码: {}, 添加折扣行, 折扣金额: {}", goodsZxbm, discountAmount);
                    } else {
                        log.info("商品编码: {}, 折扣金额为0, 跳过添加折扣行", goodsZxbm);
                    }
                }
            } catch (Exception e) {
                log.error("折扣行处理失败:{}", toJSONString(e));
                throw new BizException("折扣行处理失败", BizErrorCode.DISCOUNT_GOODS_LIST_HANDLER_ERROR);
            }
        } else if (discountRate.compareTo(BigDecimal.ONE) == 0) {
            log.info("折扣率为1，创建原价行");
            for (int i = 0; i < discountGoodsList.size(); i++) {
                JSONObject goodInfo = discountGoodsList.getJSONObject(i);
                PushInvoiceDocDetail pushInvoiceDocDetail = docDetailMap.get(goodInfo.getString(BasicKey.GOODS_PERSONAL_CODE));
                // 折扣率为1，创建原价行
                JSONObject originalGoodsLine = getOriginalGoodsLine(pushInvoiceDocDetail, goodInfo);
                originalGoodsLine.replace(BasicKey.INVOICE_ITEM_TYPE, "0");
                setSerialNum(originalGoodsLine, goodsList);
                goodsList.add(originalGoodsLine);
            }
        } else {
            log.error("折扣率计算错误，结果大于1：{}", discountRate);
            throw new BizException("折扣率计算错误，结果大于1", BizErrorCode.DISCOUNT_RATE_ERROR);
        }
    }

    /**
     * 计算并设置总税额
     */
    private void calculateAndSetTotalTax(JSONArray goodsList,
                                          PushInvoiceDocRequest request,
                                          JSONObject jsonRequest) {
        BigDecimal totalTax = goodsList.stream()
                .map(good -> ((JSONObject) good).getBigDecimal(BasicKey.GOODS_TOTAL_TAX))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        jsonRequest.put(BasicKey.INVOICE_TOTAL_PRICE, request.getInvoiceTotalPriceTax().subtract(totalTax));
        jsonRequest.put(BasicKey.INVOICE_TOTAL_TAX, totalTax);
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
                    String waitHandlerUrl = pushDocResult.getString(BasicKey.INTERFACE_DATA);
                    String issueUrl = waitHandlerUrl.startsWith("http://") ? waitHandlerUrl.replaceFirst("http://", "https://") : waitHandlerUrl;
                    pushInvoiceDocResponse.setIssueUrl(issueUrl);
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
