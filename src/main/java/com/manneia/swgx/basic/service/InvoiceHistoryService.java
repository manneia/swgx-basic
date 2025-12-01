package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manneia.swgx.basic.common.request.InvoiceHistoryQueryRequest;
import com.manneia.swgx.basic.common.response.InvoiceHistoryItemDTO;
import com.manneia.swgx.basic.common.response.InvoiceHistoryQueryResponse;
import com.manneia.swgx.basic.mapper.InvoiceHistoryMapper;
import com.manneia.swgx.basic.model.entity.InvoiceHistory;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 往期勾选发票信息服务
 *
 * @author lk
 */
@Slf4j
@Service
public class InvoiceHistoryService {

    @Resource
    private InvoiceHistoryMapper invoiceHistoryMapper;

    @Resource
    private BwHttpUtil bwHttpUtil;

    @Value("${invoice.history.query.api.url:}")
    private String historyQueryApiUrl;

    /**
     * 查询并保存往期勾选发票信息
     *
     * @param nsrsbh 纳税人识别号
     * @param tjyf   统计月份（格式：YYYYMM）
     */
    @Transactional(rollbackFor = Exception.class)
    public void queryAndSaveHistoryInvoices(String nsrsbh, String tjyf) {
        log.info("=== 开始查询往期勾选发票信息 ===");
        log.info("纳税人识别号: {}, 统计月份: {}", nsrsbh, tjyf);

        if (historyQueryApiUrl == null || historyQueryApiUrl.isEmpty()) {
            log.warn("invoice.history.query.api.url 未配置，跳过往期勾选发票查询");
            return;
        }

        try {
            // 构建请求参数
            InvoiceHistoryQueryRequest request = new InvoiceHistoryQueryRequest();
            request.setNsrsbh(nsrsbh);
            request.setTjyf(tjyf);
            request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));

            log.info("=== 调用往期勾选发票查询接口 ===");
            log.info("URL: {}", historyQueryApiUrl);
            log.info("请求参数: {}", JSON.toJSONString(request));

            // 调用接口
            String responseStr = bwHttpUtil.httpPostRequest(historyQueryApiUrl, JSON.toJSONString(request), "json");

            log.info("=== 往期勾选发票查询接口响应 ===");
            log.info("纳税人识别号: {}, 统计月份: {}", nsrsbh, tjyf);
            log.info("响应结果: {}", responseStr);

            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("往期勾选发票查询失败：响应为空");
                return;
            }

            // 解析响应
            InvoiceHistoryQueryResponse response = JSON.parseObject(responseStr, InvoiceHistoryQueryResponse.class);

            if (response == null || response.getCode() == null || response.getCode() != 0) {
                log.warn("往期勾选发票查询失败：{}", responseStr);
                return;
            }

            if (response.getData() == null || response.getData().getFpxxList() == null) {
                log.info("往期勾选发票查询成功，但无发票数据");
                return;
            }

            List<InvoiceHistoryItemDTO> fpxxList = response.getData().getFpxxList();
            log.info("查询到往期勾选发票数量: {}", fpxxList.size());

            // 删除该纳税人该月份的旧数据
            LambdaQueryWrapper<InvoiceHistory> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(InvoiceHistory::getNsrsbh, nsrsbh)
                    .eq(InvoiceHistory::getTjyf, tjyf);
            int deleteCount = invoiceHistoryMapper.delete(deleteWrapper);
            log.info("删除纳税人 {} 月份 {} 的旧数据 {} 条", nsrsbh, tjyf, deleteCount);

            // 保存新数据
            int saveCount = 0;
            for (InvoiceHistoryItemDTO item : fpxxList) {
                InvoiceHistory history = new InvoiceHistory();
                
                // 设置基本信息
                history.setNsrsbh(nsrsbh);
                history.setTjyf(tjyf);
                
                // 复制发票信息
                BeanUtils.copyProperties(item, history);
                
                // 设置时间
                history.setCreateTime(LocalDateTime.now());
                history.setUpdateTime(LocalDateTime.now());
                
                invoiceHistoryMapper.insert(history);
                saveCount++;
            }

            log.info("=== 往期勾选发票信息保存完成 ===");
            log.info("纳税人识别号: {}, 统计月份: {}, 保存数量: {}", nsrsbh, tjyf, saveCount);

        } catch (Exception e) {
            log.error("=== 往期勾选发票查询异常 ===");
            log.error("纳税人识别号: {}, 统计月份: {}", nsrsbh, tjyf, e);
            throw new RuntimeException("往期勾选发票查询失败", e);
        }
    }
}
