package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.response.SyncResponse;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@SpringBootTest
class ScheduledSyncTaskTest {
    @Resource
    private BwHttpUtil bwHttpUtil;

    @Resource
    private InvoiceService invoiceService;


    @Resource
    private InvoiceHistoryService invoiceHistoryService;

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Test
    void syncInvoices() {
        String req="{\n" +
                "    \"nsrsbh\": \"913101156072273832\",\n" +
                "    \"qqlx\": \"3\",\n" +
                "    \"kprqq\": \"20250702\",\n" +
                "    \"kprqz\": \"20250702\",\n" +
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

//    @Test
//    void syncInvoices2() {
//        System.out.println("=== 开始往期勾选发票信息同步任务 ===");
//
//        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
//        if (taxpayerList == null || taxpayerList.isEmpty()) {
//            System.out.println("纳税人识别号列表为空，跳过往期勾选发票同步");
//            return;
//        }
//
//        // 计算查询的月份：两个月前
//        LocalDate today = LocalDate.now();
//        LocalDate twoMonthsAgo = today.minusMonths(2).withDayOfMonth(1);
//        String tjyf = YYYYMM.format(twoMonthsAgo);
//
//        System.out.println("查询月份：" + tjyf);
//
//        // 测试单个纳税人
//        String nsrsbh = "913101156072273832";
//        try {
//            System.out.println("查询纳税人 " + nsrsbh + " 月份 " + tjyf + " 的往期勾选发票");
//            invoiceHistoryService.queryAndSaveHistoryInvoices(nsrsbh, tjyf);
//            System.out.println("纳税人 " + nsrsbh + " 的往期勾选发票信息查询完成");
//        } catch (Exception e) {
//            System.out.println("查询失败：" + e.getMessage());
//            e.printStackTrace();
//        }
//
//        System.out.println("=== 往期勾选发票信息同步任务完成 ===");
//    }

}