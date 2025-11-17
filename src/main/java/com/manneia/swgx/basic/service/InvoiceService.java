package com.manneia.swgx.basic.service;

import com.manneia.swgx.basic.common.request.InvoiceDetailDTO;
import com.manneia.swgx.basic.common.request.InvoicePushRequest;
import com.manneia.swgx.basic.common.response.InvoiceDTO;
import com.manneia.swgx.basic.common.response.InvoiceItemDTO;
import com.manneia.swgx.basic.common.response.InvoicePushResponse;
import com.manneia.swgx.basic.mapper.PushRecordMapper;
import com.manneia.swgx.basic.model.entity.PushRecord;
import com.manneia.swgx.basic.service.support.InvoiceApiSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 发票推送服务
 *
 * @author lk
 */
@Slf4j
@Service
public class InvoiceService {

    @Resource
    private PushRecordMapper pushRecordMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private InvoiceApiSupport invoiceApiSupport;

	@Value("${invoice.push.api.url:}")
	private String pushApiUrl;

    /**
     * 需要推送的发票类型代码
     */
    private static final List<String> TARGET_FPLXDM = Collections.unmodifiableList(
            Arrays.asList("17", "85", "86", "87", "88"));

    /**
     * 处理同步返回的发票数据
     * 提取指定类型的发票，保存到数据库并推送
     *
     * @param invoiceList 发票列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void processInvoices(List<InvoiceDTO> invoiceList) {
        if (invoiceList == null || invoiceList.isEmpty()) {
            log.info("发票列表为空，跳过处理");
            return;
        }

        // 过滤出需要处理的发票类型
        List<InvoiceDTO> targetInvoices = invoiceList.stream()
                .filter(invoice -> invoice.getFplxdm() != null
                        && TARGET_FPLXDM.contains(invoice.getFplxdm()))
                .collect(Collectors.toList());

        if (targetInvoices.isEmpty()) {
            log.info("未找到需要处理的发票类型（17,85,86,87,88）");
            return;
        }

        log.info("找到 {} 张需要处理的发票", targetInvoices.size());

        for (InvoiceDTO invoiceDTO : targetInvoices) {
            try {
                // 检查是否已存在（根据发票代码和号码）
                PushRecord existingRecord = pushRecordMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PushRecord>()
                                .eq(PushRecord::getFpdm, invoiceDTO.getFpdm())
                                .eq(PushRecord::getFphm, invoiceDTO.getFphm())
                                .last("LIMIT 1")
                );

                PushRecord pushRecord;
                boolean needPush = true;
                
                if (existingRecord != null) {
                    // 更新已有记录
                    pushRecord = existingRecord;
                    // 如果已经推送成功，则跳过推送
                    if ("2".equals(pushRecord.getPushStatus()) && "1".equals(pushRecord.getPushSuccess())) {
                        needPush = false;
                        log.info("推送记录已存在且推送成功，跳过推送：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
                    } else {
                        log.info("更新已存在的推送记录：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
                    }
                } else {
                    // 创建新记录
                    pushRecord = new PushRecord();
                    pushRecord.setCreateTime(LocalDateTime.now());
                }

                // 转换并保存发票基本信息
                convertAndSaveInvoice(invoiceDTO, pushRecord);

                // 如果需要推送，则转换为推送请求并推送
                if (needPush) {
                    pushInvoice(pushRecord, invoiceDTO);
                }
            } catch (Exception e) {
                log.error("处理发票失败：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm(), e);
            }
        }
    }

    /**
     * 转换并保存发票信息
     */
    private void convertAndSaveInvoice(InvoiceDTO invoiceDTO, PushRecord pushRecord) {
        // 复制基本信息
        BeanUtils.copyProperties(invoiceDTO, pushRecord);

        // 设置默认状态
        if (pushRecord.getInvoiceStatus() == null) {
            pushRecord.setInvoiceStatus("0"); // 默认正常
        }
        if (pushRecord.getEntryStatus() == null) {
            pushRecord.setEntryStatus("0"); // 默认未入账
        }
        if (pushRecord.getPushStatus() == null) {
            pushRecord.setPushStatus("0"); // 默认未推送
        }
        if (pushRecord.getPushSuccess() == null) {
            pushRecord.setPushSuccess("0"); // 默认未成功
        }

        pushRecord.setUpdateTime(LocalDateTime.now());

        if (pushRecord.getId() == null) {
            pushRecordMapper.insert(pushRecord);
        } else {
            pushRecordMapper.updateById(pushRecord);
        }
    }

    /**
     * 推送发票
     */
    private void pushInvoice(PushRecord pushRecord, InvoiceDTO invoiceDTO) {
        if (pushApiUrl == null || pushApiUrl.isEmpty()) {
            log.warn("推送接口地址未配置，跳过推送");
            return;
        }

        try {
            // 更新推送状态为推送中
            pushRecord.setPushStatus("1");
            pushRecord.setPushTime(LocalDateTime.now());
            pushRecordMapper.updateById(pushRecord);

            // 转换为推送请求
            InvoicePushRequest pushRequest = convertToPushRequest(invoiceDTO);

            // 构建请求头
            InvoiceApiSupport.HeaderPackage headerPackage = invoiceApiSupport.buildHeaders();
            HttpEntity<InvoicePushRequest> entity = new HttpEntity<>(pushRequest, headerPackage.getHeaders());

            InvoicePushResponse response = restTemplate.postForObject(
                    pushApiUrl, entity, InvoicePushResponse.class);

            // 解析响应并更新状态
            if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
                // 推送成功
                pushRecord.setPushStatus("2"); // 已推送
                pushRecord.setPushSuccess("1"); // 成功
                pushRecord.setPushErrorMsg(null);
                log.info("发票推送成功：{} {}", pushRecord.getFpdm(), pushRecord.getFphm());
            } else {
                // 推送失败
                pushRecord.setPushStatus("2"); // 已推送（尝试过）
                pushRecord.setPushSuccess("0"); // 失败
                String errorMsg = response != null
                        ? (response.getMsg() != null ? response.getMsg() : response.getErrorMsg())
                        : "推送接口返回为空";
                pushRecord.setPushErrorMsg(errorMsg);
                log.warn("发票推送失败：{} {}，错误信息：{}", pushRecord.getFpdm(), pushRecord.getFphm(), errorMsg);
            }
        } catch (Exception e) {
            // 推送异常
            pushRecord.setPushStatus("2");
            pushRecord.setPushSuccess("0");
            pushRecord.setPushErrorMsg(e.getMessage());
            log.error("发票推送异常：{} {}", pushRecord.getFpdm(), pushRecord.getFphm(), e);
        } finally {
            pushRecord.setUpdateTime(LocalDateTime.now());
            pushRecordMapper.updateById(pushRecord);
        }
    }

    /**
     * 将InvoiceDTO转换为InvoicePushRequest
     */
    private InvoicePushRequest convertToPushRequest(InvoiceDTO invoiceDTO) {
        InvoicePushRequest pushRequest = new InvoicePushRequest();

        // 基本信息
        pushRequest.setInvoiceType(invoiceDTO.getFplxdm());
        pushRequest.setInvoiceCode(invoiceDTO.getFpdm());
        pushRequest.setInvoiceNum(invoiceDTO.getFphm());
        pushRequest.setInvoiceDate(invoiceDTO.getKprq());
        pushRequest.setCheckCode(invoiceDTO.getJym());

        // 购买方信息
        pushRequest.setPurchaseTaxPayer(invoiceDTO.getGfmc());
        pushRequest.setPurchaseTaxPayerNo(invoiceDTO.getGfsh());
        pushRequest.setPurchaseAddrTel(invoiceDTO.getGfdzdh());
        pushRequest.setPurchaseBankAccount(invoiceDTO.getGfyhzh());

        // 销售方信息
        pushRequest.setSalesTaxPayer(invoiceDTO.getXfmc());
        pushRequest.setSalesTaxPayerNo(invoiceDTO.getXfsh());
        pushRequest.setSalesAddrTel(invoiceDTO.getXfdzdh());
        pushRequest.setSalesBankAccount(invoiceDTO.getXfyhzh());

        // 金额信息
        pushRequest.setTotalTax(invoiceDTO.getHjse());
        pushRequest.setTotalAmount(invoiceDTO.getHjje());
        pushRequest.setTotalAmountTax(invoiceDTO.getJshj());

        // 其他信息
        pushRequest.setMemo(invoiceDTO.getBz());
        pushRequest.setDrawer(invoiceDTO.getKpr());
        pushRequest.setChecker(invoiceDTO.getFhr());
        pushRequest.setPayee(invoiceDTO.getSkr());

        // 转换明细列表
        if (invoiceDTO.getFpmxList() != null && !invoiceDTO.getFpmxList().isEmpty()) {
            List<InvoiceDetailDTO> detailList = new ArrayList<>();
            for (InvoiceItemDTO item : invoiceDTO.getFpmxList()) {
                InvoiceDetailDTO detail = new InvoiceDetailDTO();
                detail.setRowNo(item.getXh());
                detail.setName(item.getSpmc());
                detail.setModel(item.getGgxh());
                detail.setUom(item.getDw());

                // 转换金额字段（字符串转BigDecimal）
                try {
                    if (item.getDj() != null && !item.getDj().isEmpty()) {
                        detail.setPrice(new BigDecimal(item.getDj()));
                    }
                    if (item.getSpsl() != null && !item.getSpsl().isEmpty()) {
                        detail.setQuantity(new BigDecimal(item.getSpsl()));
                    }
                    if (item.getJe() != null && !item.getJe().isEmpty()) {
                        detail.setAmount(new BigDecimal(item.getJe()));
                    }
                    if (item.getSe() != null && !item.getSe().isEmpty()) {
                        detail.setTax(new BigDecimal(item.getSe()));
                    }
                    if (item.getSl() != null && !item.getSl().isEmpty()) {
                        detail.setRate(new BigDecimal(item.getSl()));
                    }
                    // 含税金额 = 金额 + 税额
                    if (detail.getAmount() != null && detail.getTax() != null) {
                        detail.setAmountTax(detail.getAmount().add(detail.getTax()));
                    }
                } catch (Exception e) {
                    log.warn("转换明细金额字段失败：{}", e.getMessage());
                }

                detailList.add(detail);
            }
            pushRequest.setDetail(detailList);
        }

        return pushRequest;
    }

}

