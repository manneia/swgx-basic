package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.CustomsPaymentQueryRequest;
import com.manneia.swgx.basic.common.request.SyncRequest;
import com.manneia.swgx.basic.common.response.CustomsPaymentQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceDTO;
import com.manneia.swgx.basic.common.response.InvoiceItemDTO;
import com.manneia.swgx.basic.common.response.SyncResponse;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 同步任务：每三小时调用一次接口，开票日期范围为本月
 *
 * @author lk
 */
@Slf4j
@Component
public class ScheduledSyncTask {

	private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

	@Value("${invoice.sync.api.url:}")
	private String apiUrl;

	@Value("${invoice.payment.api.url:}")
	private String customsApiUrl;

	@Resource
	private InvoiceService invoiceService;

	@Resource
	private InvoiceHistoryService invoiceHistoryService;

	@Resource
	private BwHttpUtil bwHttpUtil;

    /**
     * 每三小时执行一次：整点、3点、6点…
     * 使用并行处理优化，提高同步效率
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void syncInvoices() {
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
     * 海关缴款书定时查询任务：每三小时执行一次
     * 使用并行处理优化，提高查询效率
     */
    @Scheduled(cron = "0 10 */3 * * ?")
    public void syncCustomsPayments() {
        if (customsApiUrl == null || customsApiUrl.isEmpty()) {
            log.warn("customs.payment.query.api.url 未配置，跳过本次海关缴款书查询");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(3); // 最近3天

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过本次海关缴款书查询");
            return;
        }
        
        // 创建固定大小的线程池
        int threadPoolSize = Math.min(taxpayerList.size(), 5); // 最多5个线程并行处理
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            log.info("开始并行处理{}个纳税人识别号的海关缴款书查询，线程池大小：{}", taxpayerList.size(), threadPoolSize);
            
            // 并行处理每个纳税人识别号
            List<CompletableFuture<Void>> futures = taxpayerList.stream()
                .map(nsrsbh -> CompletableFuture.runAsync(() -> {
                    try {
                        processCustomsPayments(nsrsbh, start, today);
                    } catch (Exception ex) {
                        log.error("纳税人 {} 海关缴款书查询调用异常", nsrsbh, ex);
                    }
                }, executor))
                .collect(Collectors.toList());
            
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }

        log.info("所有纳税人识别号海关缴款书查询任务完成");
    }
    
    /**
     * 处理单个纳税人的海关缴款书查询
     * @param nsrsbh 纳税人识别号
     * @param start 开始日期
     * @param today 当前日期
     */
    private void processCustomsPayments(String nsrsbh, LocalDate start, LocalDate today) {
        try {
            log.info("开始查询纳税人 {} 的海关缴款书", nsrsbh);

            CustomsPaymentQueryRequest req = new CustomsPaymentQueryRequest();
            req.setNsrsbh(nsrsbh);
            req.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
            req.setTfrqq(YYYYMMDD.format(start));
            req.setTfrqz(YYYYMMDD.format(today));

            String reqJson = JSON.toJSONString(req);
            log.info("调用海关缴款书查询接口，url={}，纳税人识别号={}，请求参数={}", customsApiUrl, nsrsbh, reqJson);

            // 添加重试逻辑
            String responseStr = httpPostWithRetry(customsApiUrl, reqJson, "json", 3);

            log.info("海关缴款书查询接口返回，url={}，纳税人识别号={}，响应长度={}", customsApiUrl, nsrsbh, 
                    responseStr == null ? 0 : responseStr.length());

            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("纳税人识别号 {} 海关缴款书查询返回为空", nsrsbh);
                return;
            }

            CustomsPaymentQueryResponse response = JSON.parseObject(responseStr, CustomsPaymentQueryResponse.class);

            if (response != null) {
                List<CustomsPaymentQueryResponse.CustomsPaymentItem> items = response.getData();
                int size = items == null ? 0 : items.size();
                log.info("纳税人 {} 海关缴款书查询完成 code={} msg={} dataSize={}",
                        nsrsbh, response.getCode(), response.getMsg(), size);

                if (items != null && !items.isEmpty()) {
                    // 使用并行流优化数据转换过程
                    List<InvoiceDTO> invoiceList = items.parallelStream().map(item -> {
                        InvoiceDTO invoiceDTO = new InvoiceDTO();
                        // 基本标识信息：使用缴款书号码作为发票号占位
                        invoiceDTO.setFphm(item.getJkshm());
                        invoiceDTO.setKprq(item.getTfrq());
                        invoiceDTO.setFplxdm("17");
                        // 将纳税人信息映射为购方信息
                        invoiceDTO.setGfsh(item.getNsrsbh());
                        invoiceDTO.setGfmc(item.getNsrmc());
                        // 金额相关：税额和价税合计
                        invoiceDTO.setHjse(item.getSe());
                        // 这里将有效税额作为价税合计或合计金额的占位
                        BigDecimal yxse = item.getYxse();
                        if (yxse != null) {
                            invoiceDTO.setJshj(yxse);
                        }

                        // 明细映射 - 修复重复代码的问题
                        if (item.getJksmxList() != null && !item.getJksmxList().isEmpty()) {
                            List<InvoiceItemDTO> detailList = new ArrayList<>();
                            for (CustomsPaymentQueryResponse.CustomsPaymentDetail detail : item.getJksmxList()) {
                                InvoiceItemDTO invoiceItem = new InvoiceItemDTO();
                                invoiceItem.setSpmc(detail.getHwmc());
                                invoiceItem.setSpbm(detail.getSh());
                                invoiceItem.setDw(detail.getDw());
                                invoiceItem.setSpsl(detail.getSpsl());
                                invoiceItem.setSl(detail.getSl());
                                if (detail.getSkje() != null) {
                                    invoiceItem.setSe(detail.getSkje().toPlainString());
                                }
                                if (detail.getWsjg() != null) {
                                    invoiceItem.setJe(detail.getWsjg().toPlainString());
                                }
                                detailList.add(invoiceItem);
                            }
                            invoiceDTO.setFpmxList(detailList);
                        }
                        return invoiceDTO;
                    }).collect(Collectors.toList());

                    // 批量处理发票
                    if (!invoiceList.isEmpty()) {
                        log.info("纳税人 {} 海关缴款书转换为发票数据，共 {} 条", nsrsbh, invoiceList.size());
                        invoiceService.processInvoices(invoiceList);
                    }
                }
            } else {
                log.warn("纳税人 {} 海关缴款书查询解析结果为空", nsrsbh);
            }
        } catch (Exception ex) {
            log.error("纳税人 {} 海关缴款书查询处理异常", nsrsbh, ex);
        }
    }

	/**
	 * 每月1号凌晨执行一次：查询往期勾选发票信息
	 * 查询范围：两个月前的发票数据
	 */
//	@Scheduled(cron = "0 0 3 1 * ?")
	public void syncHistoryInvoices() {
		log.info("=== 开始往期勾选发票信息同步任务 ===");

		List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
		if (taxpayerList.isEmpty()) {
			log.warn("纳税人识别号列表为空，跳过往期勾选发票同步");
			return;
		}

		// 计算查询的月份：两个月前
		LocalDate today = LocalDate.now();
		LocalDate twoMonthsAgo = today.minusMonths(2).withDayOfMonth(1); // 两个月前的1号
		String tjyf = YYYYMM.format(twoMonthsAgo);

		log.info("查询月份：{}", tjyf);
		
		// 创建固定大小的线程池
        int threadPoolSize = Math.min(taxpayerList.size(), 5); // 最多5个线程并行处理
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            log.info("开始并行处理{}个纳税人识别号的往期勾选发票查询", taxpayerList.size());
            
            // 并行处理每个纳税人识别号
            List<CompletableFuture<Void>> futures = taxpayerList.stream()
                .map(nsrsbh -> CompletableFuture.runAsync(() -> {
                    try {
                        log.info("查询纳税人 {} 月份 {} 的往期勾选发票", nsrsbh, tjyf);
                        invoiceHistoryService.queryAndSaveHistoryInvoices(nsrsbh, tjyf);
                        log.info("纳税人 {} 的往期勾选发票信息查询完成", nsrsbh);
                    } catch (Exception e) {
                        log.error("查询纳税人 {} 月份 {} 的往期勾选发票异常", nsrsbh, tjyf, e);
                    }
                }, executor))
                .collect(Collectors.toList());
            
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }

		log.info("=== 往期勾选发票信息同步任务完成 ===");
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


