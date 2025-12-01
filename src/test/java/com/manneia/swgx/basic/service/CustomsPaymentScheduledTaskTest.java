package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.manneia.swgx.basic.common.response.CustomsPaymentQueryResponse;
import com.manneia.swgx.basic.common.response.InvoiceDTO;
import com.manneia.swgx.basic.common.response.InvoiceItemDTO;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
class CustomsPaymentScheduledTaskTest {

    @Resource
    private BwHttpUtil bwHttpUtil;


    @Resource
    private InvoiceService invoiceService;
    @Test
    void queryCustomsPayments() {
        String req = "{\n" +
                "    \"czlsh\": \"43dcfc8b8d9344af855ad5eb47a5e031\",\n" +
                "    \"nsrsbh\": \"913101156072273832\",\n" +
                "    \"tfrqq\": \"20251113\",\n" +
                "    \"tfrqz\": \"20251113\",\n" +
                "    \"rzzt\": \"\"\n" +
                "}";

        System.out.println("请求参数为：" + req);


        String url = "http://api.baiwangjs.com/swgx-saas/swgx-api/interface/jxgl/hgjkscx";

        String responseStr = bwHttpUtil.httpPostRequest(url, req, "json");
        System.out.println("------------------------------------------------------------");
        System.out.println("结果为：" + responseStr);

        CustomsPaymentQueryResponse response = JSON.parseObject(responseStr, CustomsPaymentQueryResponse.class, Feature.SupportNonPublicField);

        if (response != null) {
            List<CustomsPaymentQueryResponse.CustomsPaymentItem> items = response.getData();
            int size = items == null ? 0 : items.size();
            log.info("纳税人 {} 海关缴款书查询完成 code={} msg={} dataSize={}",
                    "913101156072273832", response.getCode(), response.getMsg(), size);

            if (items != null && !items.isEmpty()) {
                List<InvoiceDTO> invoiceList = new ArrayList<>();
                for (CustomsPaymentQueryResponse.CustomsPaymentItem item : items) {
                    InvoiceDTO invoiceDTO = new InvoiceDTO();
                    // 基本标识信息：使用缴款书号码作为发票号占位
                    invoiceDTO.setFphm(item.getJkshm());
                    invoiceDTO.setFplxdm("17");
                    invoiceDTO.setKprq(item.getTfrq());
                    // 将纳税人信息映射为购方信息
                    invoiceDTO.setGfsh(item.getNsrsbh());
                    invoiceDTO.setGfmc(item.getNsrmc());
                    // 金额相关：税额和价税合计
                    invoiceDTO.setHjse(item.getSe());

                    // 明细映射
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

                    invoiceList.add(invoiceDTO);
                }
                // 复用现有的发票处理逻辑
                invoiceService.processInvoices(invoiceList);
            }
        } else {
            log.warn("纳税人 {} 海关缴款书查询解析结果为空", "913101156072273832");
        }
    }
}
