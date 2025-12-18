package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.InvoiceCollectionQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceFullQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceHistoryQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceSingleQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceStateChangeRequest;
import com.manneia.swgx.basic.common.response.InvoiceCollectionResponse;
import com.manneia.swgx.basic.common.response.InvoiceFullItemDTO;
import com.manneia.swgx.basic.common.response.InvoiceFullQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceHistoryItemDTO;
import com.manneia.swgx.basic.common.response.InvoiceHistoryQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceSingleQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceStateChangeResponse;
import com.manneia.swgx.basic.mapper.InvoiceCollectionRecordMapper;
import com.manneia.swgx.basic.mapper.InvoiceHistoryMapper;
import com.manneia.swgx.basic.mapper.InvoiceStatusChangeLogMapper;
import com.manneia.swgx.basic.model.entity.InvoiceCollectionRecord;
import com.manneia.swgx.basic.model.entity.InvoiceHistory;
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

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Value("${invoice.history.query.api.url:}")
    private String historyQueryApiUrl;

    @Value("${invoice.full.query.api.url:}")
    private String fullQueryApiUrl;

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

    @Resource
    private InvoiceHistoryMapper invoiceHistoryMapper;

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    // 缓存近两个月的往期勾选发票信息：纳税人识别号_月份 -> 发票列表
    private final Map<String, List<InvoiceHistoryItemDTO>> recentHistoryCache = new HashMap<>();

    // 并行处理线程池
    private final ExecutorService collectionExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()));
            
    /**
     * 按照指定日期采集所有纳税人的发票
     * 优化版：使用并行处理提高效率
     *
     * @param date 指定日期（yyyy-MM-dd）
     */
    public void collectInvoicesByDate(LocalDate date) {
        // 默认查询当天和前一天的数据
        collectInvoicesByDateRange(date.minusDays(1), date);
    }
    
    /**
     * 按照指定日期范围采集所有纳税人的发票
     * 优化版：使用并行处理提高效率
     *
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     */
    public void collectInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        long startTime = System.currentTimeMillis();
        
        if (collectionApiUrl == null || collectionApiUrl.isEmpty()) {
            log.warn("invoice.collection.api.url 未配置，跳过发票采集");
            return;
        }

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过发票采集");
            return;
        }

        String formattedEndDate = DATE_FORMATTER.format(endDate);
        String formattedStartDate = DATE_FORMATTER.format(startDate);
        
        log.info("开始并行采集 {} 个纳税人的发票数据", taxpayerList.size());
        
        // 使用并行流处理每个纳税人
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String taxNo : taxpayerList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始采集纳税人 {} 的发票数据，开始日期 {}，结束日期 {}", taxNo, formattedStartDate, formattedEndDate);
                    fetchAndStoreInvoices(taxNo, formattedStartDate, formattedEndDate);
                    log.info("纳税人 {} 发票采集完成", taxNo);
                } catch (Exception e) {
                    log.error("纳税人 {} 发票采集异常", taxNo, e);
                }
            }, collectionExecutor);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        log.info("全部纳税人发票采集完成，耗时{}ms", (endTime - startTime));
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
        request.setCjrqq(startDate); // 采集日期起：昨天
        request.setCjrqz(endDate);  // 采集日期止：今天
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
     * 优化版：批量查询全量发票，然后与数据库记录比对
     */
    public void syncUnaccountedInvoices() {
        syncInvoicesByEntryStatusOptimized(false);
    }
    
    /**
     * 同步未入账发票状态（旧版本，保留作为备用）
     */
    public void syncUnaccountedInvoicesOld() {
        syncInvoicesByEntryStatus(false);
    }

    /**
     * 同步发票状态，根据入账状态进行分组和批量处理
     * 优化版：添加批量处理和并行执行
     */
    private void syncInvoicesByEntryStatus(boolean accounted) {
        long startTime = System.currentTimeMillis();
        
        LocalDateTime halfYearAgo = LocalDate.now().minusMonths(6).atStartOfDay();
        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .ge(InvoiceCollectionRecord::getInvoiceDate, halfYearAgo)
                ;
        if (accounted) {
            // 已入账：02-已入账（企业所得税提前扣除）、03-已入账（企业所得税不扣除）
            wrapper.in(InvoiceCollectionRecord::getEntryStatus, "02", "03");
        } else {
            // 未入账：01-未入账、06-入账撤销、或为null
            wrapper.and(w -> w.in(InvoiceCollectionRecord::getEntryStatus, "01", "06")
                    .or().isNull(InvoiceCollectionRecord::getEntryStatus));
        }

        List<InvoiceCollectionRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            log.info("未找到需要同步的{}发票记录", accounted ? "已入账" : "未入账");
            return;
        }

        int batchSize = 50; // 每批处理数量
        int totalRecords = records.size();
        log.info("准备并行同步 {} 条{}发票状态", totalRecords, accounted ? "已入账" : "未入账");
        
        // 将记录分批处理，每批并行执行
        List<List<InvoiceCollectionRecord>> batches = new ArrayList<>();
        for (int i = 0; i < totalRecords; i += batchSize) {
            int end = Math.min(i + batchSize, totalRecords);
            batches.add(records.subList(i, end));
        }
        
        log.info("分为 {} 批进行同步处理", batches.size());
        
        int completedCount = 0;
        int failedCount = 0;
        
        // 按批次顺序处理（每批内并行）
        for (List<InvoiceCollectionRecord> batch : batches) {
            // 并行处理当前批次
            List<CompletableFuture<Boolean>> batchFutures = new ArrayList<>();
            
            for (InvoiceCollectionRecord record : batch) {
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        syncSingleInvoiceStatus(record);
                        return true;
                    } catch (Exception e) {
                        log.error("同步发票状态失败：{} {}", record.getInvoiceCode(), record.getInvoiceNumber(), e);
                        return false;
                    }
                }, collectionExecutor);
                
                batchFutures.add(future);
            }
            
            // 等待当前批次完成
            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                    batchFutures.toArray(new CompletableFuture[0]));
            
            try {
                // 等待当前批次完成
                allDone.join();
                
                // 统计成功和失败数
                for (CompletableFuture<Boolean> future : batchFutures) {
                    if (future.get()) {
                        completedCount++;
                    } else {
                        failedCount++;
                    }
                }
                
                log.info("已完成 {}/{} 条发票状态同步", completedCount, totalRecords);
                
            } catch (Exception e) {
                log.error("等待批量同步完成异常", e);
            }
        }
        
        long endTime = System.currentTimeMillis();
        log.info("同步{}发票状态完成，成功{}条，失败{}条，耗时{}ms", 
                accounted ? "已入账" : "未入账", completedCount, failedCount, (endTime - startTime));
    }
    
    /**
     * 同步发票状态（优化版）
     * 策略：先批量查询所有纳税人的全量发票，然后与数据库记录进行比对
     * 
     * @param accounted true-已入账，false-未入账
     */
    private void syncInvoicesByEntryStatusOptimized(boolean accounted) {
        long startTime = System.currentTimeMillis();
        
        LocalDateTime halfYearAgo = LocalDate.now().minusMonths(6).atStartOfDay();
        
        // 1. 查询数据库中需要同步的发票记录
        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .ge(InvoiceCollectionRecord::getInvoiceDate, halfYearAgo);
        if (accounted) {
            wrapper.in(InvoiceCollectionRecord::getEntryStatus, "02", "03");
        } else {
            wrapper.and(w -> w.in(InvoiceCollectionRecord::getEntryStatus, "01", "06")
                    .or().isNull(InvoiceCollectionRecord::getEntryStatus));
        }
        
        List<InvoiceCollectionRecord> dbRecords = recordMapper.selectList(wrapper);
        if (dbRecords.isEmpty()) {
            log.info("未找到需要同步的{}发票记录", accounted ? "已入账" : "未入账");
            return;
        }
        
        log.info("========== 开始优化版发票状态同步 ==========");
        log.info("数据库中待同步发票数量: {}", dbRecords.size());
        log.info("同步类型: {}", accounted ? "已入账" : "未入账");
        
        // 2. 按纳税人分组
        Map<String, List<InvoiceCollectionRecord>> recordsByTaxpayer = dbRecords.stream()
                .filter(r -> r.getBuyerTaxNo() != null)
                .collect(Collectors.groupingBy(InvoiceCollectionRecord::getBuyerTaxNo));
        
        log.info("涉及纳税人数量: {}", recordsByTaxpayer.size());
        
        // 3. 批量查询每个纳税人的全量发票
        Map<String, InvoiceFullItemDTO> fullInvoiceMap = new HashMap<>();
        int taxpayerIndex = 0;
        
        for (Map.Entry<String, List<InvoiceCollectionRecord>> entry : recordsByTaxpayer.entrySet()) {
            String taxNo = entry.getKey();
            taxpayerIndex++;
            
            log.info(">>> [{}/{}] 开始查询纳税人 {} 的全量发票", taxpayerIndex, recordsByTaxpayer.size(), taxNo);
            
            try {
                // 查询该纳税人半年内的所有发票
                List<InvoiceFullItemDTO> fullInvoices = queryFullInvoicesByTaxpayer(
                        taxNo, 
                        DATE_FORMATTER.format(halfYearAgo.toLocalDate()), 
                        DATE_FORMATTER.format(LocalDate.now())
                );
                
                log.info(">>> 纳税人 {} 查询到 {} 张发票", taxNo, fullInvoices.size());
                
                // 建立发票代码+号码 -> 发票对象的映射
                for (InvoiceFullItemDTO invoice : fullInvoices) {
                    String key = invoice.getFpdm() + "_" + invoice.getFphm();
                    fullInvoiceMap.put(key, invoice);
                }
                
            } catch (Exception e) {
                log.error(">>> 查询纳税人 {} 的全量发票失败", taxNo, e);
            }
        }
        
        log.info("全量发票查询完成，共获取 {} 张发票", fullInvoiceMap.size());
        
        // 4. 比对并更新状态
        int updatedCount = 0;
        int unchangedCount = 0;
        int notFoundCount = 0;
        
        for (InvoiceCollectionRecord dbRecord : dbRecords) {
            try {
                String key = dbRecord.getInvoiceCode() + "_" + dbRecord.getInvoiceNumber();
                InvoiceFullItemDTO fullInvoice = fullInvoiceMap.get(key);
                
                if (fullInvoice == null) {
                    notFoundCount++;
                    log.debug("发票在全量查询中未找到: {} {}", dbRecord.getInvoiceCode(), dbRecord.getInvoiceNumber());
                    continue;
                }
                
                // 检查状态是否变化
                String newInvoiceStatus = fullInvoice.getFpzt();
                String newCheckStatus = fullInvoice.getGxzt();
                String newEntryStatus = fullInvoice.getRzyt();
                String oldInvoiceStatus = dbRecord.getInvoiceStatus();
                String oldCheckStatus = dbRecord.getCheckStatus();
                String oldEntryStatus = dbRecord.getEntryStatus();
                
                boolean invoiceStatusChanged = !equals(oldInvoiceStatus, newInvoiceStatus) && newInvoiceStatus != null;
                boolean checkStatusChanged = !equals(oldCheckStatus, newCheckStatus) && newCheckStatus != null;
                boolean entryStatusChanged = !equals(oldEntryStatus, newEntryStatus) && newEntryStatus != null;
                
                if (invoiceStatusChanged || checkStatusChanged || entryStatusChanged) {
                    log.info("发票状态变化: {} {}, 发票状态: {}->{}, 勾选状态: {}->{}, 入账状态: {}->{}",
                            dbRecord.getInvoiceCode(), dbRecord.getInvoiceNumber(),
                            oldInvoiceStatus, newInvoiceStatus,
                            oldCheckStatus, newCheckStatus,
                            oldEntryStatus, newEntryStatus);
                    
                    // 调用状态变更接口
                    boolean success = callStateChangeApi(dbRecord,
                            oldInvoiceStatus, oldCheckStatus, oldEntryStatus,
                            newInvoiceStatus, newCheckStatus, newEntryStatus,
                            fullInvoice.getGxsj());
                    
                    if (success) {
                        updateRecordWithFullData(dbRecord, fullInvoice);
                        updatedCount++;
                    }
                } else {
                    unchangedCount++;
                    // 更新时间戳
                    dbRecord.setUpdateTime(LocalDateTime.now());
                    recordMapper.updateById(dbRecord);
                }
                
            } catch (Exception e) {
                log.error("处理发票状态同步异常: {} {}", dbRecord.getInvoiceCode(), dbRecord.getInvoiceNumber(), e);
            }
        }
        
        long endTime = System.currentTimeMillis();
        log.info("========== 优化版发票状态同步完成 ==========");
        log.info("总处理数: {}", dbRecords.size());
        log.info("状态已更新: {}", updatedCount);
        log.info("状态未变化: {}", unchangedCount);
        log.info("未找到发票: {}", notFoundCount);
        log.info("总耗时: {} 秒", (endTime - startTime) / 1000);
        log.info("==========================================");
    }
    
    /**
     * 查询指定纳税人在指定日期范围内的所有全量发票
     * 
     * @param taxNo 纳税人识别号
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate 结束日期（yyyy-MM-dd）
     * @return 发票列表
     */
    private List<InvoiceFullItemDTO> queryFullInvoicesByTaxpayer(String taxNo, String startDate, String endDate) {
        List<InvoiceFullItemDTO> allInvoices = new ArrayList<>();
        int pageNumber = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            InvoiceFullQueryRequest request = new InvoiceFullQueryRequest();
            request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
            request.setNsrsbh(taxNo);
            request.setKprqq(startDate);
            request.setKprqz(endDate);
            request.setPageNumber(String.valueOf(pageNumber));
            request.setPageSize(String.valueOf(PAGE_SIZE));
            
            log.debug("    查询第 {} 页，纳税人: {}", pageNumber, taxNo);
            
            String responseStr = bwHttpUtil.httpPostRequest(fullQueryApiUrl, JSON.toJSONString(request), "json");
            
            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("    全量发票查询返回为空，纳税人: {}, 页码: {}", taxNo, pageNumber);
                break;
            }
            
            InvoiceFullQueryResponse response = JSON.parseObject(responseStr, InvoiceFullQueryResponse.class);
            
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.debug("    第 {} 页无数据", pageNumber);
                break;
            }
            
            allInvoices.addAll(response.getData());
            log.debug("    第 {} 页返回 {} 条记录", pageNumber, response.getData().size());
            
            // 判断是否还有更多数据
            Integer total = response.getTotal();
            if (total == null || total <= pageNumber * PAGE_SIZE) {
                hasMore = false;
            } else {
                pageNumber++;
            }
        }
        
        return allInvoices;
    }

    /**
     * 同步单张发票状态
     * 优化版：添加逻辑判断，减少不必要的API调用
     */
    private void syncSingleInvoiceStatus(InvoiceCollectionRecord record) {
        // 校验发票记录完整性
        if (record.getInvoiceNumber() == null || record.getInvoiceCode() == null) {
            log.warn("发票记录不完整，跳过同步，发票号={}", 
                    record.getInvoiceNumber() != null ? record.getInvoiceNumber() : "null");
            return;
        }
        
        // 检查上次同步时间，避免频繁同步
        LocalDateTime lastUpdateTime = record.getUpdateTime();
        LocalDateTime now = LocalDateTime.now();
        // 如果距上次同步时间小于1小时，则跳过（非未入账发票）
        boolean isUnaccounted = "01".equals(record.getEntryStatus()) || "06".equals(record.getEntryStatus());
        if (!isUnaccounted && lastUpdateTime != null && 
                Duration.between(lastUpdateTime, now).toHours() < 1) {
            log.debug("发票近期已同步，跳过，发票号={}", record.getInvoiceNumber());
            return;
        }
        
        // 使用全量进项发票查询接口获取最新的发票状态、勾选状态和入账状态
        InvoiceFullItemDTO fullInvoice = queryFullInvoiceByRecord(record);
        if (fullInvoice == null) {
            log.warn("全量发票查询失败，跳过同步，发票号={}", record.getInvoiceNumber());
            return;
        }

        String newInvoiceStatus = fullInvoice.getFpzt();
        String newCheckStatus = fullInvoice.getGxzt();
        String newEntryStatus = fullInvoice.getRzyt();
        String oldInvoiceStatus = record.getInvoiceStatus();
        String oldCheckStatus = record.getCheckStatus();
        String oldEntryStatus = record.getEntryStatus();
        
        // 检查各状态是否发生变化
        boolean invoiceStatusChanged = !equals(oldInvoiceStatus, newInvoiceStatus) && newInvoiceStatus != null;
        boolean checkStatusChanged = !oldCheckStatus.equals(newCheckStatus)
                && newCheckStatus != null
                && !"-1".equals(oldCheckStatus);
        boolean entryStatusChanged = !equals(oldEntryStatus, newEntryStatus) && newEntryStatus != null;
        
        // 判断是否有状态变化
        if (invoiceStatusChanged || checkStatusChanged || entryStatusChanged) {
            // 记录状态变化日志
            log.info("发票状态变化，发票号={}，发票状态: {}->{}, 勾选状态: {}->{}, 入账状态: {}->{}", 
                    record.getInvoiceNumber(),
                    oldInvoiceStatus, newInvoiceStatus,
                    oldCheckStatus, newCheckStatus,
                    oldEntryStatus, newEntryStatus);
            
            try {
                boolean success = callStateChangeApi(record,
                        oldInvoiceStatus, oldCheckStatus, oldEntryStatus,
                        newInvoiceStatus, newCheckStatus, newEntryStatus,
                        fullInvoice.getGxsj());
                
                // 只有当状态变更接口调用成功时，才使用全量数据更新采集记录
                if (success) {
                    updateRecordWithFullData(record, fullInvoice);
                    log.info("发票状态同步成功，发票号={}", record.getInvoiceNumber());
                } else {
                    log.warn("发票状态变更接口调用失败，发票号={}", record.getInvoiceNumber());
                }
            } catch (Exception e) {
                log.error("调用状态变更接口异常，发票号={}", record.getInvoiceNumber(), e);
            }
        } else {
            // 即使没有状态变化，也更新更新时间以避免频繁同步
            record.setUpdateTime(now);
            recordMapper.updateById(record);
            log.debug("发票状态未变化，更新时间戳，发票号={}", record.getInvoiceNumber());
        }
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
        request.setPostState(data.getGxsj());

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
        if ("发票状态".equals("发票状态")) {
            logEntity.setPreviousInvoiceStatus(oldInvoiceStatus);
            logEntity.setCurrentInvoiceStatus(record.getInvoiceStatus());
        } else if ("勾选状态".equals("勾选状态")) {
            logEntity.setPreviousCheckStatus(oldCheckStatus);
            logEntity.setCurrentCheckStatus(record.getCheckStatus());
        } else if ("入账状态".equals("入账状态")) {
            logEntity.setPreviousEntryStatus(record.getEntryStatus());
            logEntity.setCurrentEntryStatus(record.getEntryStatus());
        }
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
        if (item.getFpdm() == null && item.getFphm() == null) {
            log.warn("发票数据缺少发票代码或号码，跳过：{}", item);
            return;
        }

        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceNumber, item.getFphm())
                .last("LIMIT 1");

        if (recordMapper.selectOne(wrapper) != null) {
            log.debug("发票已存在，跳过插入：{} {}", item.getFpdm(), item.getFphm());
            return;
        }

        log.info("开始插入新发票记录，发票代码={}，发票号码={}", item.getFpdm(), item.getFphm());

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
        
        log.info("发票记录插入成功，发票代码={}，发票号码={}，ID={},发票状态={}，勾选状态={}，入账状态={}", item.getFpdm(), item.getFphm(), record.getId(), record.getInvoiceStatus(), record.getCheckStatus(), record.getEntryStatus());
    }

    /**
     * 保存发票状态 / 勾选状态 / 入账状态变更日志
     * （仅在调用状态变更接口之后由 callStateChangeApi 调用）
     */
    private void saveStatusChangeLog(InvoiceCollectionRecord record,
                                     String oldInvoiceStatus,
                                     String oldCheckStatus,
                                     String oldEntryStatus,
                                     String newInvoiceStatus,
                                     String newCheckStatus,
                                     String newEntryStatus,
                                     String requestId,
                                     InvoiceStateChangeRequest request,
                                     InvoiceStateChangeResponse response) {
        InvoiceStatusChangeLog logEntity = new InvoiceStatusChangeLog();
        logEntity.setInvoiceCode(record.getInvoiceCode());
        logEntity.setInvoiceNumber(record.getInvoiceNumber());
        logEntity.setZpfphm(record.getZpfphm());

        logEntity.setPreviousInvoiceStatus(oldInvoiceStatus);
        logEntity.setCurrentInvoiceStatus(newInvoiceStatus);
        logEntity.setPreviousCheckStatus(oldCheckStatus);
        logEntity.setCurrentCheckStatus(newCheckStatus);
        logEntity.setPreviousEntryStatus(oldEntryStatus);
        logEntity.setCurrentEntryStatus(newEntryStatus);

        logEntity.setRequestId(requestId);
        logEntity.setRequestBody(JSON.toJSONString(request));
        if (response != null) {
            logEntity.setResponseStatus(response.getStatus());
            logEntity.setResponseMsg(response.getMsg());
            logEntity.setResponseOutJson(response.getOutJson());
        }

        logEntity.setCreatedAt(LocalDateTime.now());
        statusChangeLogMapper.insert(logEntity);
    }

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    
    /**
     * 根据采集记录调用全量进项发票查询接口
     * 优化版：添加重试机制
     */
    private InvoiceFullItemDTO queryFullInvoiceByRecord(InvoiceCollectionRecord record) {
        log.debug("=== 开始全量进项发票查询（采集记录） ===");
        log.debug("发票代码: {}, 发票号码: {}", record.getInvoiceCode(), record.getInvoiceNumber());

        if (fullQueryApiUrl == null || fullQueryApiUrl.isEmpty()) {
            log.warn("invoice.full.query.api.url 未配置，跳过全量发票查询");
            return null;
        }

        if (record.getInvoiceCode() == null || record.getInvoiceNumber() == null) {
            log.warn("发票缺少代码或号码，跳过全量发票查询");
            return null;
        }

        String nsrsbh = record.getBuyerTaxNo();
        if (nsrsbh == null || nsrsbh.isEmpty()) {
            log.warn("发票缺少购买方纳税人识别号，跳过全量发票查询：{} {}",
                    record.getInvoiceCode(), record.getInvoiceNumber());
            return null;
        }

        // 准备请求参数
        InvoiceFullQueryRequest request = new InvoiceFullQueryRequest();
        request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
        request.setNsrsbh(nsrsbh);
        request.setFpdm(record.getInvoiceCode());
        request.setFphm(record.getInvoiceNumber());
        request.setPageNumber("1");
        request.setPageSize("1");
        
        // 执行重试逻辑
        for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
            try {
                if (retryCount > 0) {
                    // 非第一次尝试，等待一段时间再重试（指数退避策略）
                    long waitTimeMillis = (long) Math.pow(2, retryCount) * 1000;
                    log.info("第{}次重试查询发票，等待{}ms后进行，发票号={}", 
                            retryCount + 1, waitTimeMillis, record.getInvoiceNumber());
                    Thread.sleep(waitTimeMillis);
                }
                
                log.info("调用全量进项发票查询接口，发票号={}", record.getInvoiceNumber());
                log.info("调用参数：{}",request.toString());
                String responseStr = bwHttpUtil.httpPostRequest(fullQueryApiUrl, JSON.toJSONString(request), "json");

                log.info("返回参数：{}", responseStr);
                if (responseStr == null || responseStr.isEmpty()) {
                    log.warn("全量发票查询响应为空，发票号={}，尝试{}/{}", 
                            record.getInvoiceNumber(), retryCount + 1, MAX_RETRY_COUNT);
                    continue; // 重试
                }

                InvoiceFullQueryResponse response = JSON.parseObject(responseStr, InvoiceFullQueryResponse.class);

                if (response == null || response.getCode() == null || response.getCode() != 0) {
                    log.warn("全量发票查询失败，发票号={}，响应码={}，尝试{}/{}", 
                            record.getInvoiceNumber(),
                            response != null ? response.getCode() : "null",
                            retryCount + 1, MAX_RETRY_COUNT);
                    
                    // 判断是否需要重试（某些错误代码可能不需要重试）
                    Integer code = response != null ? response.getCode() : null;
                    if (code != null && (code == 404 || code == 400)) {
                        // 不需要重试的错误代码
                        return null;
                    }
                    
                    continue; // 重试
                }

                if (response.getData() == null || response.getData().isEmpty()) {
                    log.warn("全量发票查询返回空数据，发票号={}", record.getInvoiceNumber());
                    return null; // 数据为空不需要重试
                }

                // 查询成功
                InvoiceFullItemDTO invoice = response.getData().get(0);
                log.debug("发票查询成功，发票号={}，勾选状态={}，入账状态={}", 
                        record.getInvoiceNumber(), invoice.getGxzt(), invoice.getRzyt());
                return invoice;
                
            } catch (Exception e) {
                log.warn("全量发票查询异常，发票号={}，尝试{}/{}", 
                        record.getInvoiceNumber(), retryCount + 1, MAX_RETRY_COUNT, e);
                
                // 如果是最后一次尝试，记录详细错误
                if (retryCount == MAX_RETRY_COUNT - 1) {
                    log.error("全量发票查询最终失败，发票号={}", record.getInvoiceNumber(), e);
                }
            }
        }
        
        // 所有重试均失败
        log.error("全量发票查询已达最大重试次数{}，仍然失败，发票号={}", 
                MAX_RETRY_COUNT, record.getInvoiceNumber());
        return null;
    }

    /**
     * 使用全量发票数据更新采集记录
     */
    private void updateRecordWithFullData(InvoiceCollectionRecord record, InvoiceFullItemDTO fullInvoice) {
        record.setInvoiceStatus(fullInvoice.getFpzt());
        record.setCheckStatus(fullInvoice.getGxzt());
        record.setEntryStatus(fullInvoice.getRzyt());

        record.setSellerName(fullInvoice.getXfmc());
        record.setSellerTaxNo(fullInvoice.getXfsh());
        record.setInvoiceDate(parseDateTime(fullInvoice.getKprq()));

        BigDecimal totalAmountTax = fullInvoice.getJshj();
        if (totalAmountTax != null) {
            record.setTotalAmountTax(totalAmountTax);
        }

        record.setUpdateTime(LocalDateTime.now());
        recordMapper.updateById(record);
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

    // 状态变更接口的最大重试次数
    private static final int STATE_CHANGE_MAX_RETRY = 3;

    /**
     * 调用发票状态变更接口（根据当前采集记录），并在调用后写入状态变更日志。
     * 优化版：添加重试机制
     * 
     * @return true 表示接口调用成功（响应不为空且 status == 0），否则为 false。
     */
    private boolean callStateChangeApi(InvoiceCollectionRecord record,
                                       String oldInvoiceStatus,
                                       String oldCheckStatus,
                                       String oldEntryStatus,
                                       String newInvoiceStatus,
                                       String newCheckStatus,
                                       String newEntryStatus,
                                       String newCheckTime) {
        if (stateChangeApiUrl == null || stateChangeApiUrl.isEmpty()) {
            log.warn("invoice.state.change.api.url 未配置，跳过状态变更推送");
            return false;
        }

        // 准备请求参数
        InvoiceStateChangeRequest request = new InvoiceStateChangeRequest();
        request.setInvoiceType(record.getInvoiceType());
        request.setInvoiceCode(record.getInvoiceCode());
        if(record.getZpfphm()!=null){
            request.setInvoiceNum(record.getZpfphm());
        }else {
            request.setInvoiceNum(record.getInvoiceNumber());
        }

        if (record.getInvoiceDate() != null) {
            request.setInvoiceDate(record.getInvoiceDate().format(DATE_FORMATTER));
        }

        request.setPurchaseTaxPayer(record.getBuyerName());
        request.setPurchaseTaxPayerNo(record.getBuyerTaxNo());
        request.setSalesTaxPayer(record.getSellerName());
        request.setSalesTaxPayerNo(record.getSellerTaxNo());
        request.setTotalAmountTax(record.getTotalAmountTax());
        // 使用最新的发票状态 / 勾选状态 / 入账状态
        request.setState(newInvoiceStatus);
        request.setDeductible(newCheckStatus);
        if("1".equals(newCheckStatus)){
            request.setDeductibleDate(newCheckTime);
        } else if ("-1".equals(newCheckStatus)) {
            request.setDeductible("3");
        }
        request.setPostState(newEntryStatus);

        InvoiceApiSupport.HeaderPackage headerPackage = invoiceApiSupport.buildHeaders();
        HttpEntity<InvoiceStateChangeRequest> entity = new HttpEntity<>(request, headerPackage.getHeaders());

        log.debug("调用发票状态变更接口，发票号={}", record.getInvoiceNumber());

        InvoiceStateChangeResponse response = null;
        String requestId = headerPackage.getRequestId();
        
        // 执行重试逻辑
        for (int retryCount = 0; retryCount < STATE_CHANGE_MAX_RETRY; retryCount++) {
            try {
                if (retryCount > 0) {
                    // 非第一次尝试，等待一段时间再重试（指数退避策略）
                    long waitTimeMillis = (long) Math.pow(2, retryCount) * 1000;
                    log.info("第{}次重试状态变更推送，等待{}ms后进行，发票号={}", 
                            retryCount + 1, waitTimeMillis, record.getInvoiceNumber());
                    Thread.sleep(waitTimeMillis);
                    
                    // 重新生成请求头，避免重复使用过期的认证信息
                    headerPackage = invoiceApiSupport.buildHeaders();
                    entity = new HttpEntity<>(request, headerPackage.getHeaders());
                    requestId = headerPackage.getRequestId();
                }

                // 执行接口调用
                response = restTemplate.postForObject(stateChangeApiUrl, entity, InvoiceStateChangeResponse.class);
                
                // 检查响应是否有效
                if (response == null) {
                    log.warn("状态变更接口响应为空，发票号={}，尝试{}/{}", 
                            record.getInvoiceNumber(), retryCount + 1, STATE_CHANGE_MAX_RETRY);
                    continue; // 重试
                }
                
                Integer status = response.getStatus();
                if (status == null || status != 0) {
                    // 判断是否是临时错误，需要重试
                    boolean shouldRetry = shouldRetryStateChange(status);
                    
                    log.warn("状态变更接口调用失败，status={}，msg={}，发票号={}，{}", 
                            status, response.getMsg(), record.getInvoiceNumber(), 
                            shouldRetry ? "将重试" : "不再重试");
                    
                    if (!shouldRetry) {
                        // 某些错误不需要重试，如参数错误、权限错误等
                        break;
                    }
                    
                    continue; // 需要重试的错误
                }
                
                // 调用成功
                log.info("状态变更接口调用成功，发票号={}", record.getInvoiceNumber());
                break;
                
            } catch (Exception e) {
                log.warn("状态变更接口调用异常，发票号={}，尝试{}/{}", 
                        record.getInvoiceNumber(), retryCount + 1, STATE_CHANGE_MAX_RETRY, e);
                
                // 如果是最后一次尝试，记录详细错误
                if (retryCount == STATE_CHANGE_MAX_RETRY - 1) {
                    log.error("状态变更接口调用最终失败，发票号={}", record.getInvoiceNumber(), e);
                }
            }
        }

        // 在接口调用完成后，记录一次综合的状态变更日志（无论成功失败都记录一条）
        saveStatusChangeLog(record,
                oldInvoiceStatus, oldCheckStatus, oldEntryStatus,
                newInvoiceStatus, newCheckStatus, newEntryStatus,
                requestId, request, response);

        // 解析响应结果，只有当 status == 0 时认为调用成功
        if (response == null) {
            return false;
        }
        
        Integer status = response.getStatus();
        return status != null && status == 0;
    }
    
    /**
     * 判断状态变更接口是否需要重试
     * @param status 状态码
     * @return 是否应该重试
     */
    private boolean shouldRetryStateChange(Integer status) {
        if (status == null) {
            return true; // 未知状态默认重试
        }
        
        // 根据状态码判断是否需要重试
        // 这里列出一些不需要重试的状态码，如参数错误、权限错误等
        // 需要根据实际API文档调整
        switch (status) {
            case 400: // 参数错误
            case 401: // 未授权
            case 403: // 禁止访问
            case 404: // 资源不存在
                return false; // 这些错误一般不需要重试
            case 429: // 请求过多
            case 500: // 服务器内部错误
            case 502: // 网关错误
            case 503: // 服务不可用
            case 504: // 网关超时
                return true;  // 这些错误一般需要重试
            default:
                return status >= 500; // 服务器类错误需要重试
        }
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
    
    /**
     * 在应用关闭时释放资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭发票采集服务线程池...");
        try {
            // 优雅关闭线程池
            collectionExecutor.shutdown();
            if (!collectionExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("线程池未在指定时间内结束，强制关闭");
                collectionExecutor.shutdownNow();
            }
            log.info("发票采集服务线程池关闭成功");
        } catch (Exception e) {
            log.error("关闭线程池异常", e);
            collectionExecutor.shutdownNow();
        }
    }
}


