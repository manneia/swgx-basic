package com.manneia.swgx.basic.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.LocalDate;

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

    /**
     * 每半小时同步一次未入账发票状态
     */
    @Test
    public void syncUnaccountedInvoices() {
        log.info("开始执行未入账发票状态同步任务");
        invoiceCollectionService.syncUnaccountedInvoices();
        log.info("未入账发票状态同步任务结束");
    }

}