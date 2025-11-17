package com.manneia.swgx.basic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

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

    /**
     * 每小时执行一次，采集当天发票信息。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void collectDailyInvoices() {
        LocalDate targetDate = LocalDate.now();
        log.info("开始执行发票采集任务，日期 {}", targetDate);
        invoiceCollectionService.collectInvoicesByDate(targetDate);
        log.info("发票采集任务结束，日期 {}", targetDate);
    }

    /**
     * 每日 02:10 同步已入账发票状态
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncAccountedInvoicesDaily() {
        log.info("开始执行已入账发票状态同步任务");
        invoiceCollectionService.syncAccountedInvoices();
        log.info("已入账发票状态同步任务结束");
    }

    /**
     * 每半小时同步一次未入账发票状态
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void syncUnaccountedInvoices() {
        log.info("开始执行未入账发票状态同步任务");
        invoiceCollectionService.syncUnaccountedInvoices();
        log.info("未入账发票状态同步任务结束");
    }
}


