package com.manneia.swgx.basic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 发票采集定时任务
 *
 * <ul>
 *     <li>每小时采集一次当天发票</li>
 *     <li>每日同步一次已入账发票状态</li>
 *     <li>每半小时同步一次未入账发票状态</li>
 * </ul>
 *
 * @author lk
 */
@Slf4j
@Component
public class InvoiceCollectionScheduledTask {

    @Resource
    private InvoiceCollectionService invoiceCollectionService;
    
    // 配置线程池大小
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    
    // 专用于发票处理的线程池
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    
    // 任务超时时间（分钟）
    private static final int TASK_TIMEOUT_MINUTES = 10;

    /**
     * 每3小时的第20分钟执行一次，采集最近两天的发票信息。
     * 优化版：使用并行处理和超时控制
     */
    @Scheduled(cron = "0 20 */3 * * ?")
    public void collectDailyInvoices() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(2); // 最近两天
        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行发票采集任务，日期范围 {} 至 {}", startDate, endDate);
        
        try {
            // 异步执行任务并添加超时控制
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 传入开始日期和结束日期
                    invoiceCollectionService.collectInvoicesByDateRange(startDate, endDate);
                } catch (Exception e) {
                    log.error("发票采集任务异常，日期范围 {} 至 {}", startDate, endDate, e);
                    throw new RuntimeException(e);
                }
            }, taskExecutor);
            
            // 等待任务完成，最多等待指定时间
            future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            log.info("发票采集任务成功完成，日期范围 {} 至 {}，耗时{}s", startDate, endDate, duration.getSeconds());
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("发票采集任务超时，已经运行超过{}分钟", TASK_TIMEOUT_MINUTES);
        } catch (Exception e) {
            log.error("发票采集任务异常", e);
        }
    }

    /**
     * 每日 02:10 同步已入账发票状态
     * 优化版：使用并行处理和超时控制
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncAccountedInvoicesDaily() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行已入账发票状态同步任务");
        
        try {
            // 异步执行任务并添加超时控制
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    invoiceCollectionService.syncAccountedInvoices();
                } catch (Exception e) {
                    log.error("已入账发票状态同步任务异常", e);
                    throw new RuntimeException(e);
                }
            }, taskExecutor);
            
            // 等待任务完成，最多等待指定时间
            future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            log.info("已入账发票状态同步任务成功完成，耗时{}s", duration.getSeconds());
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("已入账发票状态同步任务超时，已经运行超过{}分钟", TASK_TIMEOUT_MINUTES);
        } catch (Exception e) {
            log.error("已入账发票状态同步任务异常", e);
        }
    }

    /**
     * 每半小时同步一次未入账发票状态
     * 优化版：使用并行处理和超时控制
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void syncUnaccountedInvoices() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行未入账发票状态同步任务");
        
        try {
            // 异步执行任务并添加超时控制
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    invoiceCollectionService.syncUnaccountedInvoices();
                } catch (Exception e) {
                    log.error("未入账发票状态同步任务异常", e);
                    throw new RuntimeException(e);
                }
            }, taskExecutor);
            
            // 等待任务完成，最多等待指定时间
            future.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            log.info("未入账发票状态同步任务成功完成，耗时{}s", duration.getSeconds());
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("未入账发票状态同步任务超时，已经运行超过{}分钟", TASK_TIMEOUT_MINUTES);
        } catch (Exception e) {
            log.error("未入账发票状态同步任务异常", e);
        }
    }
    
    /**
     * 在应用关闭时释放资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭发票采集任务线程池...");
        try {
            // 优雅关闭线程池
            taskExecutor.shutdown();
            if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("线程池未在指定时间内结束，强制关闭");
                taskExecutor.shutdownNow();
            }
            log.info("发票采集任务线程池关闭成功");
        } catch (Exception e) {
            log.error("关闭线程池异常", e);
            taskExecutor.shutdownNow();
        }
    }
}


