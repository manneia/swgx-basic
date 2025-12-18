package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.SyncRequest;
import com.manneia.swgx.basic.common.response.InvoiceDTO;
import com.manneia.swgx.basic.common.response.SyncResponse;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class ScheduledSyncTaskTest {
    @Resource
    private BwHttpUtil bwHttpUtil;

    @Resource
    private InvoiceService invoiceService;

    @Value("${invoice.sync.api.url:}")
    private String apiUrl;

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private InvoiceHistoryService invoiceHistoryService;

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Test
    void syncInvoices() {
        String req="{\n" +
                "    \"nsrsbh\": \"913101156072273832\",\n" +
                "    \"qqlx\": \"3\",\n" +
                "    \"kprqq\": \"20251216\",\n" +
                "    \"kprqz\": \"20251216\",\n" +
                "    \"czlsh\": \"43dcfc8b8d9344af855ad5eb47a5e031\",\n" +
                "    \"zzhlx\": \"0\"\n" +
                "}";

        System.out.println("请求参数为："+req);
        String responseStr = bwHttpUtil.httpPostRequest("http://api.baiwangjs.com/swgx-saas/swgx-api/interface/jxgl/fpzzhqpcx", req, "json");
        System.out.println("------------------------------------------------------------");
        System.out.println("结果为："+responseStr);
        // 使用 Fastjson 特性配置来确保正确解析
        SyncResponse response = JSON.parseObject(responseStr, SyncResponse.class, Feature.SupportNonPublicField);

        if (response != null) {
            System.out.println("fpxxList size: " + (response.getFpxxList() != null ? response.getFpxxList().size() : "null"));
            
            if (response.getFpxxList() != null && !response.getFpxxList().isEmpty()) {
                System.out.println("开始处理 " + response.getFpxxList().size() + " 条发票数据");
                invoiceService.processInvoices(response.getFpxxList());
            } else {
                System.out.println("发票列表为空，无法处理");
            }
        } else {
            System.out.println("纳税人识别号 同步调用异常 - response 为 null");
        }
    }

    @Test
    public void syncInvoices2() {
        if (apiUrl == null || apiUrl.isEmpty()) {
            log.warn("invoice.sync.api.url 未配置，跳过本次同步");
            return;
        }

        LocalDate today = LocalDate.now();

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过本次同步");
            return;
        }

        // 创建固定大小的线程池，避免创建过多线程
        int threadPoolSize = Math.min(taxpayerList.size(), 5); // 最多5个线程并行处理
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try {
            log.info("开始并行处理{}个纳税人识别号的发票同步，线程池大小：{}", taxpayerList.size(), threadPoolSize);

            // 并行处理每个纳税人识别号
            List<CompletableFuture<Void>> futures = taxpayerList.stream()
                    .map(nsrsbh -> CompletableFuture.runAsync(() -> {
                        try {
                            processTaxpayerInvoices(nsrsbh, today);
                        } catch (Exception ex) {
                            log.error("纳税人识别号 {} 同步调用异常", nsrsbh, ex);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }

        log.info("所有纳税人识别号同步任务完成");
    }

    /**
     * 处理单个纳税人识别号的发票同步
     * @param nsrsbh 纳税人识别号
     * @param today 当前日期
     */
    private void processTaxpayerInvoices(String nsrsbh, LocalDate today) {
        log.info("开始同步纳税人识别号：{}", nsrsbh);
        List<InvoiceDTO> allInvoices = new ArrayList<>();

        // 固定参数
        String qqlx = "3"; // 固定为3
        String zzhlx = ""; // 不传

        // 构建最近3天的请求
        List<SyncRequest> requests = new ArrayList<>();
        // 只查询最近3天的数据
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = today;

        // 只创建一个请求，范围为最近3天
        SyncRequest req = new SyncRequest();
        req.setNsrsbh(nsrsbh);
        req.setQqlx(qqlx);
        req.setKprqq(YYYYMMDD.format(startDate));
        req.setKprqz(YYYYMMDD.format(endDate));
        req.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
        req.setZzhlx(zzhlx);

        requests.add(req);

        // 处理最近3天的请求
        requests.parallelStream().forEach(request -> {
            try {
                log.info("调用发票同步接口，url={}，纳税人识别号={}，请求参数={}", apiUrl, nsrsbh, JSON.toJSONString(request));

                // 添加重试逻辑
                String responseStr = httpPostWithRetry(apiUrl, JSON.toJSONString(request), "json", 3);

                log.info("发票同步接口返回，url={}，纳税人识别号={}，响应长度={}", apiUrl, nsrsbh,
                        responseStr == null ? 0 : responseStr.length());

                if (responseStr == null || responseStr.isEmpty()) {
                    log.warn("纳税人识别号 {} 最近3天 {}-{} 同步返回为空", nsrsbh,
                            request.getKprqq(), request.getKprqz());
                    return;
                }

                SyncResponse response = JSON.parseObject(responseStr, SyncResponse.class);

                if (response != null) {
                    int size = response.getFpxxList() == null ? 0 : response.getFpxxList().size();
                    log.info("纳税人识别号 {} 最近3天 {}-{} 同步完成 success={} code={} msg={} fpxxListSize={}",
                            nsrsbh, request.getKprqq(), request.getKprqz(),
                            response.getSuccess(), response.getCode(), response.getMsg(), size);

                    if (size > 0) {
                        synchronized(allInvoices) {
                            allInvoices.addAll(response.getFpxxList());
                        }
                    }
                } else {
                    log.warn("纳税人识别号 {} 最近3天 {}-{} 同步解析结果为空", nsrsbh,
                            request.getKprqq(), request.getKprqz());
                }
            } catch (Exception e) {
                log.warn("纳税人 {} 请求发票数据异常", nsrsbh, e);
            }
        });

        // 所有月份查询完成后，去重并统一处理
        if (!allInvoices.isEmpty()) {
            List<InvoiceDTO> uniqueInvoices = removeDuplicateInvoices(allInvoices);
            log.info("纳税人识别号 {} 最近3天汇总发票数量：{}，去重后：{}，开始处理",
                    nsrsbh, allInvoices.size(), uniqueInvoices.size());
            invoiceService.processInvoices(uniqueInvoices);
        } else {
            log.info("纳税人识别号 {} 最近3天无需要处理的发票", nsrsbh);
        }
    }

    /**
     * 发票去重处理
     * @param invoiceList 原始发票列表
     * @return 去重后的发票列表
     */
    private List<InvoiceDTO> removeDuplicateInvoices(List<InvoiceDTO> invoiceList) {
        // 使用Map保存不重复的发票，键为发票号+开票日期
        Map<String, InvoiceDTO> uniqueInvoices = new HashMap<>();
        for (InvoiceDTO invoice : invoiceList) {
            String key = invoice.getFphm() + "_" + invoice.getKprq();
            uniqueInvoices.put(key, invoice);
        }
        return new ArrayList<>(uniqueInvoices.values());
    }

    /**
     * HTTP请求带重试机制
     * @param url 请求URL
     * @param jsonRequest JSON请求内容
     * @param requestType 请求类型
     * @param maxRetries 最大重试次数
     * @return 响应字符串
     */
    private String httpPostWithRetry(String url, String jsonRequest, String requestType, int maxRetries) {
        int retries = 0;
        Exception lastException = null;

        while (retries < maxRetries) {
            try {
                if (retries > 0) {
                    log.info("正在进行第{}次重试", retries);
                }
                return bwHttpUtil.httpPostRequest(url, jsonRequest, requestType);
            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries >= maxRetries) {
                    log.error("请求达到最大重试次数{}，放弃重试", maxRetries, e);
                    break;
                }
                log.warn("API调用失败，正在进行第{}次重试", retries, e);
                try {
                    // 指数退避策略
                    Thread.sleep(1000 * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("重试等待被中断", ie);
                    break;
                }
            }
        }

        if (lastException != null) {
            log.error("所有重试失败", lastException);
        }
        return null;
    }
}