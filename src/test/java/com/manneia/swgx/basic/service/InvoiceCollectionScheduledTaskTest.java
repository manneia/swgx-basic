package com.manneia.swgx.basic.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class InvoiceCollectionScheduledTaskTest {
    @Resource
    private InvoiceCollectionService invoiceCollectionService;

    @Test
    public void collectDailyInvoices() {
        LocalDate targetDate = LocalDate.now();
        invoiceCollectionService.collectInvoicesByDate(targetDate);
    }

    /**
     * 每日 02:10 同步已入账发票状态
     */

    @Test
    public void syncAccountedInvoicesDaily() {
        log.info("开始执行已入账发票状态同步任务");
        invoiceCollectionService.syncAccountedInvoices();
        log.info("已入账发票状态同步任务结束");
    }

    // 配置线程池大小
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    // 专用于发票处理的线程池
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    // 任务超时时间（分钟）
    private static final int TASK_TIMEOUT_MINUTES = 100;
    /**
     * 每半小时同步一次未入账发票状态
     */
    @Test
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

}