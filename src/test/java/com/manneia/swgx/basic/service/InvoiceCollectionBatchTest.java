package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.InvoiceFullQueryRequest;
import com.manneia.swgx.basic.common.response.InvoiceFullQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceFullItemDTO;
import com.manneia.swgx.basic.mapper.InvoiceCollectionRecordMapper;
import com.manneia.swgx.basic.model.entity.InvoiceCollectionRecord;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 发票采集批量测试
 * 
 * <p>按月批量调用发票征收汇总批次查询接口，遍历所有纳税人识别号，将数据存储到 invoice_collection_record 表</p>
 *
 * @author lk
 */
@Slf4j
@SpringBootTest
public class InvoiceCollectionBatchTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 200;

    private String collectionApiUrl = "http://api.baiwangjs.com/swgx-saas/swgx-api/interface/jxgl/jxqlfpcx";


    @Resource
    private BwHttpUtil bwHttpUtil;

    @Resource
    private InvoiceCollectionRecordMapper recordMapper;

    /**
     * 测试函数：批量采集所有纳税人的发票数据
     * 
     * 默认采集最近6个月的数据
     */
    @Test
    public void testBatchCollectInvoices() {
        // 设置时间范围：最近6个月
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(5);
        
        batchCollectInvoicesByMonthRange(startMonth, endMonth);
    }

    /**
     * 测试函数：批量采集指定月份范围的发票数据
     * 
     * 示例：采集2023年1月到2023年12月的数据
     */
    @Test
    public void testBatchCollectInvoicesCustomRange() {
        YearMonth startMonth = YearMonth.of(2023, 1);
        YearMonth endMonth = YearMonth.of(2023, 12);
        
        batchCollectInvoicesByMonthRange(startMonth, endMonth);
    }

    /**
     * 按月范围批量采集所有纳税人的发票数据
     *
     * @param startMonth 开始月份（包含）
     * @param endMonth   结束月份（包含）
     */
    public void batchCollectInvoicesByMonthRange(YearMonth startMonth, YearMonth endMonth) {
        long overallStartTime = System.currentTimeMillis();
        
        // 参数验证
        if (startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("开始月份不能晚于结束月份");
        }

        if (collectionApiUrl == null || collectionApiUrl.isEmpty()) {
            log.warn("invoice.collection.api.url 未配置，跳过发票采集");
            return;
        }

        // 获取所有纳税人识别号
        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过发票采集");
            return;
        }

        // 生成月份列表
        List<YearMonth> monthList = generateMonthList(startMonth, endMonth);
        
        log.info("========== 开始批量采集发票数据 ==========");
        log.info("纳税人数量: {}", taxpayerList.size());
        log.info("月份范围: {} 至 {}", startMonth, endMonth);
        log.info("月份数量: {}", monthList.size());
        log.info("========================================");

        int totalInserted = 0;
        int totalSkipped = 0;
        int totalProcessed = 0;

        // 遍历所有纳税人
        for (int i = 0; i < taxpayerList.size(); i++) {
            String taxNo = taxpayerList.get(i);
            log.info(">>> 开始处理纳税人 [{}/{}]: {}", (i + 1), taxpayerList.size(), taxNo);
            
            // 遍历所有月份
            for (YearMonth month : monthList) {
                try {
                    log.info("  >> 采集月份: {}", month);
                    
                    // 计算该月的第一天和最后一天
                    LocalDate monthStart = month.atDay(1);
                    LocalDate monthEnd = month.atEndOfMonth();
                    
                    String startDateStr = DATE_FORMATTER.format(monthStart);
                    String endDateStr = DATE_FORMATTER.format(monthEnd);
                    
                    // 调用接口并保存数据
                    int[] result = fetchAndStoreInvoicesByMonth(taxNo, startDateStr, endDateStr);
                    int inserted = result[0];
                    int skipped = result[1];
                    
                    totalInserted += inserted;
                    totalSkipped += skipped;
                    totalProcessed += (inserted + skipped);
                    
                    log.info("  >> 月份 {} 完成，新增: {}, 跳过: {}", month, inserted, skipped);
                    
                } catch (Exception e) {
                    log.error("  >> 采集月份 {} 异常", month, e);
                }
            }
            
            log.info(">>> 纳税人 {} 处理完成\n", taxNo);
        }

        long overallEndTime = System.currentTimeMillis();
        long totalTimeSeconds = (overallEndTime - overallStartTime) / 1000;
        
        log.info("========== 批量采集完成 ==========");
        log.info("总处理记录数: {}", totalProcessed);
        log.info("新增记录数: {}", totalInserted);
        log.info("跳过记录数: {}", totalSkipped);
        log.info("总耗时: {} 秒 ({} 分钟)", totalTimeSeconds, totalTimeSeconds / 60);
        log.info("===================================");
    }

    /**
     * 获取并存储指定纳税人、指定月份的发票数据
     *
     * @param taxNo     纳税人识别号
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return int数组 [新增数量, 跳过数量]
     */
    private int[] fetchAndStoreInvoicesByMonth(String taxNo, String startDate, String endDate) {
        int insertedCount = 0;
        int skippedCount = 0;
        int pageNumber = 1;
        boolean hasMore = true;

        while (hasMore) {
            // 构建请求
            InvoiceFullQueryRequest request = buildRequest(taxNo, startDate, endDate, pageNumber, PAGE_SIZE);
            
            // 记录请求入参
            String requestJson = JSON.toJSONString(request);
            log.info("    ========== 接口调用开始 ==========");
            log.info("    > 接口URL: {}", collectionApiUrl);
            log.info("    > 纳税人识别号: {}", taxNo);
            log.info("    > 查询日期范围: {} 至 {}", startDate, endDate);
            log.info("    > 页码: {}, 每页大小: {}", pageNumber, PAGE_SIZE);
            log.info("    > 操作流水号(czlsh): {}", request.getCzlsh());
            log.info("    > 请求参数(JSON): {}", requestJson);

            // 发送请求
            long requestStartTime = System.currentTimeMillis();
            String responseStr = bwHttpUtil.httpPostRequest(collectionApiUrl, requestJson, "json");
            long requestEndTime = System.currentTimeMillis();
            long requestDuration = requestEndTime - requestStartTime;

            // 记录响应出参
            log.info("    > 接口响应耗时: {} ms", requestDuration);
            log.info("    > 响应内容(JSON): {}", responseStr);

            // 检查响应
            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("    > 接口返回为空，页码: {}", pageNumber);
                log.info("    ========== 接口调用结束（响应为空） ==========\n");
                break;
            }

            // 解析响应
            InvoiceFullQueryResponse response = JSON.parseObject(responseStr, InvoiceFullQueryResponse.class);

            if (response == null) {
                log.warn("    > 响应解析失败，页码: {}", pageNumber);
                log.info("    ========== 接口调用结束（解析失败） ==========\n");
                break;
            }

            log.info("    > 响应状态: success={}, code={}, msg={}", 
                    response.getSuccess(), response.getCode(), response.getMsg());

            if (response.getData() == null || response.getData().isEmpty()) {
                log.info("    > 第 {} 页无数据", pageNumber);
                log.info("    ========== 接口调用结束（无数据） ==========\n");
                break;
            }

            log.info("    > 分页信息: 当前页={}, 每页大小={}, 总记录数={}", 
                    response.getPageNumber(), response.getPageSize(), response.getTotal());
            log.info("    > 第 {} 页返回 {} 条发票记录", pageNumber, response.getData().size());
            log.info("    ========== 接口调用结束 ==========\n");

            // 保存数据
            for (InvoiceFullItemDTO invoice : response.getData()) {
                boolean saved = saveIfAbsent(invoice);
                if (saved) {
                    insertedCount++;
                } else {
                    skippedCount++;
                }
            }

            // 判断是否还有更多数据
            Integer total = response.getTotal();
            if (total == null || total <= pageNumber * PAGE_SIZE) {
                hasMore = false;
            } else {
                pageNumber++;
            }
        }

        return new int[]{insertedCount, skippedCount};
    }

    /**
     * 构建全量进项发票查询请求
     */
    private InvoiceFullQueryRequest buildRequest(String taxNo, String startDate, String endDate, 
                                                  int pageNumber, int pageSize) {
        InvoiceFullQueryRequest request = new InvoiceFullQueryRequest();
        request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
        request.setNsrsbh(taxNo);
        request.setKprqq(startDate);  // 开票日期起（yyyy-MM-dd）
        request.setKprqz(endDate);    // 开票日期止（yyyy-MM-dd）
        request.setPageNumber(String.valueOf(pageNumber));
        request.setPageSize(String.valueOf(pageSize));
        return request;
    }

    /**
     * 保存发票记录（如果不存在）
     *
     * @param invoice 全量进项发票DTO
     * @return true-已保存，false-已跳过
     */
    private boolean saveIfAbsent(InvoiceFullItemDTO invoice) {
        if (invoice.getFphm() == null) {
            log.warn("      ! 发票数据缺少发票号码，跳过: {}", invoice);
            return false;
        }

        // 检查是否已存在（根据发票代码和发票号码）
        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceCode, invoice.getFpdm())
                .eq(InvoiceCollectionRecord::getInvoiceNumber, invoice.getFphm())
                .last("LIMIT 1");

        if (recordMapper.selectOne(wrapper) != null) {
            log.debug("      - 发票已存在，跳过: {} {}", invoice.getFpdm(), invoice.getFphm());
            return false;
        }

        // 创建新记录
        InvoiceCollectionRecord record = new InvoiceCollectionRecord();
        record.setInvoiceType(invoice.getFplxdm());
        record.setInvoiceCode(invoice.getFpdm());
        record.setInvoiceNumber(invoice.getFphm());
        record.setInvoiceDate(parseDateTime(invoice.getKprq()));
        record.setBuyerName(invoice.getGfmc());
        record.setBuyerTaxNo(invoice.getGfsh());
        record.setSellerName(invoice.getXfmc());
        record.setSellerTaxNo(invoice.getXfsh());
        record.setTotalAmountTax(invoice.getJshj());
        record.setInvoiceStatus(invoice.getFpzt());
        record.setCheckStatus(invoice.getGxzt());  // 勾选状态
        record.setEntryStatus(invoice.getRzyt());  // 入账用途（入账状态）
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        // 插入数据库
        recordMapper.insert(record);
        
        log.info("      + 发票记录已插入: {} {}, ID: {}", invoice.getFpdm(), invoice.getFphm(), record.getId());
        return true;
    }

    /**
     * 生成月份列表
     */
    private List<YearMonth> generateMonthList(YearMonth start, YearMonth end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = start;
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    /**
     * 解析日期时间字符串
     * 支持多种格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 先尝试完整的日期时间格式
            if (dateTimeStr.length() > 10) {
                return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            } else {
                // 如果只有日期部分，转换为日期并设置时间为00:00:00
                LocalDate date = LocalDate.parse(dateTimeStr, DATE_FORMATTER);
                return date.atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("解析日期时间失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 解析日期字符串
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("解析日期失败: {}", dateStr, e);
            return null;
        }
    }


}
