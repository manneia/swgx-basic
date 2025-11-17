package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.InvoiceCollectionQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceSingleQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceStateChangeRequest;
import com.manneia.swgx.basic.common.response.InvoiceCollectionResponse;
import com.manneia.swgx.basic.common.response.InvoiceSingleQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceStateChangeResponse;
import com.manneia.swgx.basic.mapper.InvoiceCollectionRecordMapper;
import com.manneia.swgx.basic.mapper.InvoiceStatusChangeLogMapper;
import com.manneia.swgx.basic.model.entity.InvoiceCollectionRecord;
import com.manneia.swgx.basic.model.entity.InvoiceStatusChangeLog;
import com.manneia.swgx.basic.service.support.InvoiceApiSupport;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 发票采集服务
 *
 * <p>定时拉取发票采集接口数据，并保存基础信息。</p>
 *
 * @author lk
 */
@Slf4j
@Service
public class InvoiceCollectionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int PAGE_SIZE = 200;

    @Value("${invoice.collection.api.url:}")
    private String collectionApiUrl;

    @Value("${invoice.single.query.api.url:}")
    private String singleQueryApiUrl;

    @Value("${invoice.state.change.api.url:}")
    private String stateChangeApiUrl;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private BwHttpUtil bwHttpUtil;

    @Resource
    private InvoiceCollectionRecordMapper recordMapper;

    @Resource
    private InvoiceStatusChangeLogMapper statusChangeLogMapper;

    @Resource
    private InvoiceApiSupport invoiceApiSupport;

    /**
     * 按照指定日期采集所有纳税人的发票
     *
     * @param date 指定日期（yyyy-MM-dd）
     */
    public void collectInvoicesByDate(LocalDate date) {
        if (collectionApiUrl == null || collectionApiUrl.isEmpty()) {
            log.warn("invoice.collection.api.url 未配置，跳过发票采集");
            return;
        }

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过发票采集");
            return;
        }

        String endDate = DATE_FORMATTER.format(date);
        String startDate = DATE_FORMATTER.format(date.minusDays(1));

        for (String taxNo : taxpayerList) {
            try {
                log.info("开始采集纳税人 {} 的发票数据，开始日期 {}，结束日期 {}", taxNo, startDate, endDate);
                fetchAndStoreInvoices(taxNo, startDate, endDate);
            } catch (Exception e) {
                log.error("纳税人 {} 发票采集异常", taxNo, e);
                // 不中断其他纳税人
            }
        }
    }

    private void fetchAndStoreInvoices(String taxNo, String startDate, String endDate) {
        int pageNumber = 1;
        boolean hasMore = true;

        while (hasMore) {
            InvoiceCollectionQueryRequest request = buildRequest(taxNo, startDate, endDate, pageNumber, PAGE_SIZE);
            log.info("调用发票采集接口，url={}，纳税人={}，请求参数={}", collectionApiUrl, taxNo, JSON.toJSONString(request));

            String responseStr = bwHttpUtil.httpPostRequest(collectionApiUrl, JSON.toJSONString(request), "json");

            log.info("发票采集接口返回，url={}，纳税人={}，响应={}", collectionApiUrl, taxNo, responseStr);

            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("发票采集接口返回为空，纳税人={}，页码={}", taxNo, pageNumber);
                break;
            }

            InvoiceCollectionResponse response = JSON.parseObject(responseStr, InvoiceCollectionResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.info("纳税人 {} 第 {} 页无发票数据", taxNo, pageNumber);
                break;
            }

            log.info("纳税人 {} 第 {} 页返回 {} 条发票数据", taxNo, pageNumber, response.getData().size());
            response.getData().forEach(this::saveIfAbsent);

            long total = response.getTotal() == null ? 0 : response.getTotal();
            int currentCount = pageNumber * PAGE_SIZE;
            if (total <= currentCount) {
                hasMore = false;
            } else {
                pageNumber++;
            }
        }
    }

    private InvoiceCollectionQueryRequest buildRequest(String taxNo, String startDate, String endDate, int pageNumber, int pageSize) {
        InvoiceCollectionQueryRequest request = new InvoiceCollectionQueryRequest();
        request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
        request.setGssh(taxNo);
        request.setKprqq(startDate); // 开票日期起：昨天
        request.setKprqz(endDate);  // 开票日期止：今天
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);
        return request;
    }

    /**
     * 同步已入账发票状态（每日一次）
     */
    public void syncAccountedInvoices() {
        syncInvoicesByEntryStatus(true);
    }

    /**
     * 同步未入账发票状态（半小时一次）
     */
    public void syncUnaccountedInvoices() {
        syncInvoicesByEntryStatus(false);
    }

    private void syncInvoicesByEntryStatus(boolean accounted) {
        LocalDateTime halfYearAgo = LocalDate.now().minusMonths(6).atStartOfDay();
        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .ge(InvoiceCollectionRecord::getInvoiceDate, halfYearAgo);
        if (accounted) {
            wrapper.eq(InvoiceCollectionRecord::getEntryStatus, "1");
        } else {
            wrapper.and(w -> w.ne(InvoiceCollectionRecord::getEntryStatus, "1")
                    .or().isNull(InvoiceCollectionRecord::getEntryStatus));
        }

        List<InvoiceCollectionRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            log.info("未找到需要同步的{}发票记录", accounted ? "已入账" : "未入账");
            return;
        }

        log.info("准备同步 {} 条{}发票状态", records.size(), accounted ? "已入账" : "未入账");
        for (InvoiceCollectionRecord record : records) {
            try {
                syncSingleInvoiceStatus(record);
            } catch (Exception e) {
                log.error("同步发票状态失败：{} {}", record.getInvoiceCode(), record.getInvoiceNumber(), e);
            }
        }
    }

    private void syncSingleInvoiceStatus(InvoiceCollectionRecord record) {
        InvoiceSingleQueryResponse.InvoiceSingleData data = querySingleInvoice(record);
        if (data == null) {
            return;
        }

        String newInvoiceStatus = data.getFpzt();
        String newCheckStatus = data.getSfgx();
        if (equals(record.getInvoiceStatus(), newInvoiceStatus)
                && equals(record.getCheckStatus(), newCheckStatus)) {
            log.debug("发票状态未变化：{} {}", record.getInvoiceCode(), record.getInvoiceNumber());
            return;
        }

        String oldInvoiceStatus = record.getInvoiceStatus();
        String oldCheckStatus = record.getCheckStatus();

        updateRecordWithSingleData(record, data);
        callStateChangeAndLog(record, data, oldInvoiceStatus, oldCheckStatus);
    }

    private InvoiceSingleQueryResponse.InvoiceSingleData querySingleInvoice(InvoiceCollectionRecord record) {
        if (singleQueryApiUrl == null || singleQueryApiUrl.isEmpty()) {
            log.warn("invoice.single.query.api.url 未配置，跳过单票查询");
            return null;
        }
        if (record.getInvoiceCode() == null || record.getInvoiceNumber() == null) {
            log.warn("发票缺少代码或号码，跳过单票查询");
            return null;
        }

        String nsrsbh = record.getSellerTaxNo() != null ? record.getSellerTaxNo() : record.getBuyerTaxNo();
        if (nsrsbh == null || nsrsbh.isEmpty()) {
            log.warn("发票缺少纳税人识别号，跳过单票查询：{} {}", record.getInvoiceCode(), record.getInvoiceNumber());
            return null;
        }

        InvoiceSingleQueryRequest request = new InvoiceSingleQueryRequest();
        request.setFpdm(record.getInvoiceCode());
        request.setFphm(record.getInvoiceNumber());
        request.setNsrsbh(nsrsbh);
        request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));

        log.info("调用单票查询接口，url={}，请求参数={}", singleQueryApiUrl, JSON.toJSONString(request));

        String responseStr = bwHttpUtil.httpPostRequest(singleQueryApiUrl, JSON.toJSONString(request), "json");

        log.info("单票查询接口返回，url={}，发票代码={}，发票号码={}，响应={}",
                singleQueryApiUrl, record.getInvoiceCode(), record.getInvoiceNumber(), responseStr);

        if (responseStr == null || responseStr.isEmpty()) {
            log.warn("单票查询失败：{} {}，响应为空", record.getInvoiceCode(), record.getInvoiceNumber());
            return null;
        }

        InvoiceSingleQueryResponse response = JSON.parseObject(responseStr, InvoiceSingleQueryResponse.class);

        if (response == null || response.getCode() == null || response.getCode() != 0) {
            log.warn("单票查询失败：{} {}，响应：{}", record.getInvoiceCode(), record.getInvoiceNumber(), responseStr);
            return null;
        }
        if (response.getData() == null) {
            log.warn("单票查询返回空数据：{} {}", record.getInvoiceCode(), record.getInvoiceNumber());
            return null;
        }
        return response.getData();
    }

    private void updateRecordWithSingleData(InvoiceCollectionRecord record,
                                            InvoiceSingleQueryResponse.InvoiceSingleData data) {
        record.setInvoiceStatus(data.getFpzt());
        record.setCheckStatus(data.getSfgx());
        record.setEntryStatus(data.getSfqr());
        record.setSellerName(data.getXfmc());
        record.setSellerTaxNo(data.getXfsh());
        record.setInvoiceDate(parseDateTime(data.getKprq()));
        BigDecimal total = calculateTotalAmountTax(data);
        if (total != null) {
            record.setTotalAmountTax(total);
        }
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    private void callStateChangeAndLog(InvoiceCollectionRecord record,
                                       InvoiceSingleQueryResponse.InvoiceSingleData data,
                                       String oldInvoiceStatus,
                                       String oldCheckStatus) {
        if (stateChangeApiUrl == null || stateChangeApiUrl.isEmpty()) {
            log.warn("invoice.state.change.api.url 未配置，跳过状态变更推送");
            return;
        }

        InvoiceStateChangeRequest request = new InvoiceStateChangeRequest();
        request.setInvoiceType(data.getFplx());
        request.setInvoiceCode(data.getFpdm());
        request.setInvoiceNum(data.getFphm());
        request.setInvoiceDate(data.getKprq());
        request.setPurchaseTaxPayer(record.getBuyerName());
        request.setPurchaseTaxPayerNo(record.getBuyerTaxNo());
        request.setSalesTaxPayer(data.getXfmc());
        request.setSalesTaxPayerNo(data.getXfsh());
        BigDecimal totalAmountTax = calculateTotalAmountTax(data);
        request.setTotalAmountTax(totalAmountTax != null ? totalAmountTax : record.getTotalAmountTax());
        request.setState(data.getFpzt());
        request.setDeductible(data.getSfgx());
        request.setDeductibleDate(data.getQrsj());

        InvoiceApiSupport.HeaderPackage headerPackage = invoiceApiSupport.buildHeaders();
        HttpEntity<InvoiceStateChangeRequest> entity = new HttpEntity<>(request, headerPackage.getHeaders());
        log.info("调用发票状态变更接口，url={}，请求参数={}", stateChangeApiUrl, JSON.toJSONString(request));

        InvoiceStateChangeResponse response = restTemplate.postForObject(
                stateChangeApiUrl, entity, InvoiceStateChangeResponse.class);

        log.info("发票状态变更接口返回，url={}，发票代码={}，发票号码={}，响应={}",
                stateChangeApiUrl, record.getInvoiceCode(), record.getInvoiceNumber(), JSON.toJSONString(response));

        InvoiceStatusChangeLog logEntity = new InvoiceStatusChangeLog();
        logEntity.setInvoiceCode(record.getInvoiceCode());
        logEntity.setInvoiceNumber(record.getInvoiceNumber());
        logEntity.setPreviousInvoiceStatus(oldInvoiceStatus);
        logEntity.setPreviousCheckStatus(oldCheckStatus);
        logEntity.setCurrentInvoiceStatus(record.getInvoiceStatus());
        logEntity.setCurrentCheckStatus(record.getCheckStatus());
        logEntity.setRequestId(headerPackage.getRequestId());
        logEntity.setRequestBody(JSON.toJSONString(request));
        if (response != null) {
            logEntity.setResponseStatus(response.getStatus());
            logEntity.setResponseMsg(response.getMsg());
            logEntity.setResponseOutJson(response.getOutJson());
        }
        logEntity.setCreatedAt(LocalDateTime.now());
        statusChangeLogMapper.insert(logEntity);
    }

    private void saveIfAbsent(InvoiceCollectionResponse.InvoiceCollectionItem item) {
        if (item.getFpdm() == null || item.getFphm() == null) {
            log.warn("发票数据缺少发票代码或号码，跳过：{}", item);
            return;
        }

        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceCode, item.getFpdm())
                .eq(InvoiceCollectionRecord::getInvoiceNumber, item.getFphm())
                .last("LIMIT 1");

        if (recordMapper.selectOne(wrapper) != null) {
            log.debug("发票已存在，跳过插入：{} {}", item.getFpdm(), item.getFphm());
            return;
        }

        InvoiceCollectionRecord record = new InvoiceCollectionRecord();
        record.setInvoiceType(item.getFplxdm());
        record.setInvoiceCode(item.getFpdm());
        record.setInvoiceNumber(item.getFphm());
        record.setInvoiceDate(parseDateTime(item.getKprq()));
        record.setBuyerName(item.getGfmc());
        record.setBuyerTaxNo(item.getGfnsrsbh());
        record.setSellerName(item.getXfmc());
        record.setSellerTaxNo(item.getXfnsrsbh());
        record.setTotalAmountTax(parseBigDecimal(item.getJshj()));
        record.setInvoiceStatus(item.getFpzt());
        record.setCheckStatus(item.getGxztDm());
        record.setEntryStatus(item.getRzztDm());
        record.setCheckDate(parseDate(item.getGxczrq()));
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        recordMapper.insert(record);
    }

    private BigDecimal calculateTotalAmountTax(InvoiceSingleQueryResponse.InvoiceSingleData data) {
        if (data == null) {
            return null;
        }
        BigDecimal amount = data.getJe();
        BigDecimal tax = data.getSe();
        if (amount == null && tax == null) {
            return null;
        }
        amount = amount == null ? BigDecimal.ZERO : amount;
        tax = tax == null ? BigDecimal.ZERO : tax;
        return amount.add(tax);
    }

    private boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("解析日期时间失败：{}", value, e);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("解析日期失败：{}", value, e);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            log.warn("解析金额失败：{}", value, e);
            return null;
        }
    }
}


