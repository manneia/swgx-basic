package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.manneia.swgx.basic.common.request.InvoiceDetailDTO;
import com.manneia.swgx.basic.common.request.InvoiceFullQueryRequest;
import com.manneia.swgx.basic.common.request.InvoiceHistoryQueryRequest;
import com.manneia.swgx.basic.common.request.InvoicePushRequest;
import com.manneia.swgx.basic.common.request.InvoiceSingleQueryRequest;
import com.manneia.swgx.basic.common.response.*;
import com.manneia.swgx.basic.mapper.InvoiceHistoryMapper;
import com.manneia.swgx.basic.mapper.InvoiceCollectionRecordMapper;
import com.manneia.swgx.basic.mapper.PushRecordMapper;
import com.manneia.swgx.basic.model.entity.InvoiceCollectionRecord;
import com.manneia.swgx.basic.model.entity.InvoiceHistory;
import com.manneia.swgx.basic.model.entity.PushRecord;
import com.manneia.swgx.basic.service.support.InvoiceApiSupport;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
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
    private InvoiceHistoryMapper invoiceHistoryMapper;

    @Resource
    private InvoiceCollectionRecordMapper invoiceCollectionRecordMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private InvoiceApiSupport invoiceApiSupport;

    @Resource
    private BwHttpUtil bwHttpUtil;

	@Value("${invoice.push.api.url:}")
	private String pushApiUrl;

	@Value("${invoice.single.query.api.url:}")
	private String singleQueryApiUrl;

	@Value("${invoice.history.query.api.url:}")
	private String historyQueryApiUrl;

	@Value("${invoice.full.query.api.url:}")
	private String fullQueryApiUrl;

	private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	// 缓存近两个月的往期勾选发票信息，格式：纳税人识别号_月份 -> 发票列表
	private final Map<String, List<InvoiceHistoryItemDTO>> recentHistoryCache = new HashMap<>();
	
	// 配置线程池大小
	private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
	
	// 专用于发票处理的线程池
	private final ExecutorService invoiceProcessorPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * 需要推送的发票类型代码
     */
    private static final List<String> TARGET_FPLXDM = Collections.unmodifiableList(
            Arrays.asList("17", "85", "86","01","04"));
//    private static final List<String> TARGET_YT = Collections.unmodifiableList(
//            Arrays.asList("83971601","55197407","46272783","39233465","223320251001071749-L01","25372000000178854079")
//    );

    /**
     * 处理同步返回的发票数据
     * 提取指定类型的发票，保存到数据库并推送
     * 优化版：使用并行处理
     *
     * @param invoiceList 发票列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void processInvoices(List<InvoiceDTO> invoiceList) {
        long startTime = System.currentTimeMillis();
        if (invoiceList == null || invoiceList.isEmpty()) {
            log.info("发票列表为空，跳过处理");
            return;
        }
        
        log.info("开始处理{}张发票", invoiceList.size());

        // 批量去重：使用Map数据结构去除重复发票
        Map<String, InvoiceDTO> uniqueInvoices = new HashMap<>();
        for (InvoiceDTO invoice : invoiceList) {
            if (invoice.getFphm() == null) continue;
            String key = invoice.getFphm();
            uniqueInvoices.put(key, invoice);
        }
        log.info("发票去重后仅有{}张", uniqueInvoices.size());
        
        // 过滤出需要处理的发票类型
        List<InvoiceDTO> targetInvoices = uniqueInvoices.values().stream()
                .filter(invoice -> invoice.getFplxdm() != null && TARGET_FPLXDM.contains(invoice.getFplxdm()))
                .collect(Collectors.toList());

        if (targetInvoices.isEmpty()) {
            log.info("未找到需要处理的发票");
            return;
        }

        log.info("找到 {} 张需要处理的发票，开始并行处理", targetInvoices.size());

        // 并行处理发票
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // 预查询所有已存在的记录，避免单条查询
        List<String> allInvoiceNumbers = targetInvoices.stream()
                .map(InvoiceDTO::getFphm)
                .collect(Collectors.toList());
        
        // 批量查询已存在的记录
        Map<String, PushRecord> existingRecords = new HashMap<>();
        if (!allInvoiceNumbers.isEmpty()) {
            // 分批查询，避免 IN 子句过长
            List<List<String>> batches = getBatches(allInvoiceNumbers, 500);
            for (List<String> batch : batches) {
                List<PushRecord> records = pushRecordMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PushRecord>()
                                .in(PushRecord::getFphm, batch)
                );
                for (PushRecord record : records) {
                    existingRecords.put(record.getFphm(), record);
                }
            }
            log.info("预查询到{}条已存在的记录", existingRecords.size());
        }
        
        // 创建批量处理任务
        for (InvoiceDTO invoiceDTO : targetInvoices) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 根据预查询结果判断是否已存在
                    PushRecord existingRecord = existingRecords.get(invoiceDTO.getFphm());
                    PushRecord pushRecord;
                    boolean needPush = true;
                    
                    if (existingRecord != null) {
                        // 更新已有记录
                        pushRecord = existingRecord;
                        // 如果已经推送成功，则跳过推送
                        if ("2".equals(pushRecord.getPushStatus()) && "1".equals(pushRecord.getPushSuccess())) {
                            needPush = false;
                            log.debug("推送记录已存在且推送成功，跳过推送：{}", invoiceDTO.getFphm());
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
                    log.error("处理发票失败：{}", invoiceDTO.getFphm(), e);
                }
            }, invoiceProcessorPool);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        try {
            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allDone.join();
        } catch (Exception e) {
            log.error("并行处理发票异常", e);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("发票处理完成，共处理{}张发票，耗时{}ms", targetInvoices.size(), (endTime - startTime));
    }
    
    /**
     * 将列表分批处理
     * @param list 原始列表
     * @param batchSize 批大小
     * @return 分批后的列表集合
     */
    private <T> List<List<T>> getBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    /**
     * 转换并保存发票信息
     */
    private void convertAndSaveInvoice(InvoiceDTO invoiceDTO, PushRecord pushRecord) {
        // 复制基本信息（字段名匹配的会自动复制）
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
     * 将当前准备推送的发票保存到发票采集记录表（基础信息），如果不存在则插入
     */
    private void saveInvoiceCollectionRecordIfAbsent(InvoiceDTO invoiceDTO, InvoicePushRequest pushRequest) {
        if (invoiceDTO == null || invoiceDTO.getFpdm() == null || invoiceDTO.getFphm() == null) {
            return;
        }

        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceCode, invoiceDTO.getFpdm())
                .eq(InvoiceCollectionRecord::getInvoiceNumber, invoiceDTO.getFphm())
                .last("LIMIT 1");

        if (invoiceCollectionRecordMapper.selectOne(wrapper) != null) {
            return;
        }

        InvoiceCollectionRecord record = new InvoiceCollectionRecord();
        record.setInvoiceType(invoiceDTO.getFplxdm());
        record.setInvoiceCode(invoiceDTO.getFpdm());
        record.setInvoiceNumber(invoiceDTO.getFphm());
        record.setZpfphm(invoiceDTO.getZpfphm());

        String kprq = invoiceDTO.getKprq();
        if (kprq != null && !kprq.isEmpty()) {
            try {
                if (kprq.length() == 10) {
                    record.setInvoiceDate(LocalDate.parse(kprq, DATE_FORMATTER).atStartOfDay());
                } else {
                    record.setInvoiceDate(LocalDateTime.parse(kprq, DATE_TIME_FORMATTER));
                }
            } catch (Exception e) {
                // ignore parse error, keep invoiceDate null
            }
        }

        record.setBuyerName(invoiceDTO.getGfmc());
        record.setBuyerTaxNo(invoiceDTO.getGfsh());
        record.setSellerName(invoiceDTO.getXfmc());
        record.setSellerTaxNo(invoiceDTO.getXfsh());
        record.setTotalAmountTax(invoiceDTO.getJshj());

        // 默认记账状态：未入账
        record.setEntryStatus("01");

        if (pushRequest != null) {
            // 使用最新的发票状态、勾选状态和勾选时间
            record.setInvoiceStatus(pushRequest.getState());
            record.setCheckStatus(pushRequest.getDeductible());
            String deductibleDate = pushRequest.getDeductibleDate();
            record.setCheckDate(LocalDate.parse(deductibleDate, DATE_FORMATTER));

        } else {
            record.setInvoiceStatus(invoiceDTO.getFpzt());
        }
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        invoiceCollectionRecordMapper.insert(record);
    }

    /**
     * 使用全量查询结果将发票保存到发票采集记录表（基础信息），如果不存在则插入
     */
    private void saveInvoiceCollectionRecordIfAbsent(InvoiceDTO invoiceDTO, InvoiceFullItemDTO fullInvoice) {
        if (invoiceDTO == null || invoiceDTO.getFpdm() == null || invoiceDTO.getFphm() == null
                || fullInvoice == null) {
            return;
        }

        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceCode, invoiceDTO.getFpdm())
                .eq(InvoiceCollectionRecord::getInvoiceNumber, invoiceDTO.getFphm())
                .last("LIMIT 1");

        if (invoiceCollectionRecordMapper.selectOne(wrapper) != null) {
            return;
        }

        InvoiceCollectionRecord record = new InvoiceCollectionRecord();
        record.setInvoiceType(fullInvoice.getFplxdm() != null ? fullInvoice.getFplxdm() : invoiceDTO.getFplxdm());
        record.setInvoiceCode(fullInvoice.getFpdm());
        record.setInvoiceNumber(fullInvoice.getFphm());
        record.setZpfphm(invoiceDTO.getZpfphm());

        String kprq = fullInvoice.getKprq();
        if (kprq != null && !kprq.isEmpty()) {
            try {
                if (kprq.length() == 10) {
                    record.setInvoiceDate(LocalDate.parse(kprq, DATE_FORMATTER).atStartOfDay());
                } else {
                    record.setInvoiceDate(LocalDateTime.parse(kprq, DATE_TIME_FORMATTER));
                }
            } catch (Exception e) {
                // ignore parse error, keep invoiceDate null
            }
        }

        record.setBuyerName(fullInvoice.getGfmc());
        record.setBuyerTaxNo(fullInvoice.getGfsh());
        record.setSellerName(fullInvoice.getXfmc());
        record.setSellerTaxNo(fullInvoice.getXfsh());
        record.setTotalAmountTax(fullInvoice.getJshj());

        // 记账状态来自全量查询的 rzyt
        record.setEntryStatus(fullInvoice.getRzyt());

        // 发票状态、勾选状态和勾选时间来自全量查询
        record.setInvoiceStatus(fullInvoice.getFpzt());
        record.setCheckStatus(fullInvoice.getGxzt());

        String gxsj = fullInvoice.getGxsj();
        record.setCheckDate(LocalDate.parse(gxsj, DATE_FORMATTER));
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        invoiceCollectionRecordMapper.insert(record);
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

            log.info("开始推送发票信息：{}",JSON.toJSONString(invoiceDTO) );

            // 转换为推送请求（内部会查询最新的勾选状态并写入pushRequest）
            InvoicePushRequest pushRequest = convertToPushRequest(invoiceDTO);

            // 构建请求头
            InvoiceApiSupport.HeaderPackage headerPackage = invoiceApiSupport.buildHeaders();
            HttpEntity<InvoicePushRequest> entity = new HttpEntity<>(pushRequest, headerPackage.getHeaders());

            // 记录推送日志
            log.info("=================================================");
            log.info("开始推送发票：{} {}", pushRecord.getFpdm(), pushRecord.getFphm());
            log.info("推送URL: {}", pushApiUrl);
            log.info("请求头: {}", headerPackage.getHeaders());
            log.info("请求参数: {}", JSON.toJSONString(pushRequest));
            log.info("=================================================");

            InvoicePushResponse response = restTemplate.postForObject(
                    pushApiUrl, entity, InvoicePushResponse.class);
            
            // 记录响应日志
            log.info("=================================================");
            log.info("推送响应: {}", JSON.toJSONString(response));
            log.info("=================================================");

            // 解析响应并更新状态
            if (response != null && "0".equals(response.getStatus())) {
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
                        ? (response.getMsg() != null ? response.getMsg() : response.getOutJson())
                        : "推送接口返回为空";
                pushRecord.setPushErrorMsg(errorMsg);
                log.warn("发票推送失败：{} {}，错误信息：{}", pushRecord.getFpdm(), pushRecord.getFphm(), errorMsg);
            }
        } catch (Exception e) {
            // 推送异常
            pushRecord.setPushStatus("2");
            pushRecord.setPushSuccess("0");
            pushRecord.setPushErrorMsg(e.getMessage());
            log.error("=================================================");
            log.error("发票推送异常：{} {}", pushRecord.getFpdm(), pushRecord.getFphm());
            log.error("异常信息: {}", e.getMessage());
            log.error("异常堆栈: ", e);
            log.error("=================================================");
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
        if(invoiceDTO.getZpfphm()!= null){
            pushRequest.setInvoiceNum(invoiceDTO.getZpfphm());
        }else {
            pushRequest.setInvoiceNum(invoiceDTO.getFphm());
        }
        pushRequest.setInvoiceDate(invoiceDTO.getKprq());
        pushRequest.setCheckCode(invoiceDTO.getJym());

        pushRequest.setState(invoiceDTO.getFpzt());

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
        // 使用三元表达式确保不为空
        pushRequest.setTotalAmount(
                invoiceDTO.getHjje() != null ? invoiceDTO.getHjje() : BigDecimal.ZERO
        );

        pushRequest.setTotalAmountTax(
                invoiceDTO.getJshj() != null ? invoiceDTO.getJshj() : BigDecimal.ZERO
        );

        // 根据金额正负值设置发票状态：正数为1，负数为0
        if (invoiceDTO.getHjje() != null) {
            if (invoiceDTO.getHjje().compareTo(BigDecimal.ZERO) >= 0) {
                pushRequest.setInvoiceStatus("1"); // 正数或零
                log.debug("发票金额为正数，设置 invoiceStatus=1");
            } else {
                pushRequest.setInvoiceStatus("3"); // 负数
                log.debug("发票金额为负数，设置 invoiceStatus=0");
            }
        } else {
            pushRequest.setInvoiceStatus("1"); // 默认为1
            log.debug("发票金额为空，默认设置 invoiceStatus=1");
        }

        // 其他信息
        pushRequest.setMemo(invoiceDTO.getBz());
        pushRequest.setDrawer(invoiceDTO.getKpr());
        pushRequest.setChecker(invoiceDTO.getFhr());
        pushRequest.setPayee(invoiceDTO.getSkr());
        pushRequest.setInputSource("10");
        pushRequest.setIsElectronic("0");

        // 查询并设置勾选状态和勾选时间
        queryAndSetDeductibleStatus(invoiceDTO, pushRequest);

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
                    // 使用工具方法统一处理转换逻辑
                    detail.setPrice(convertToBigDecimal(item.getDj()));
                    detail.setQuantity(convertToBigDecimal(item.getSpsl(), BigDecimal.ONE)); // 数量默认为1
                    detail.setAmount(convertToBigDecimal(item.getJe()));
                    detail.setTax(convertToBigDecimal(item.getSe()));
                    detail.setRate(safeParseTaxRate(item.getSl()).toString());


                    // 计算含税金额（带空值保护）
                    BigDecimal amount = (BigDecimal) ObjectUtils.defaultIfNull(detail.getAmount(), BigDecimal.ZERO);
                    BigDecimal tax = (BigDecimal) ObjectUtils.defaultIfNull(detail.getTax(), BigDecimal.ZERO);
                    detail.setAmountTax(amount.add(tax));

                } catch (Exception e) {
                    log.warn("转换明细金额字段失败：{}", e.getMessage());

                }


                detailList.add(detail);
            }
            pushRequest.setDetail(detailList);
        }

        return pushRequest;
    }

    /**
     * 检查并设置发票是否已勾选
     * <p>逻辑：</p>
     * <ol>
     *     <li>先根据纳税人识别号、发票代码、发票号码查询 invoice_history 表</li>
     *     <li>如果有数据，直接设置勾选状态和勾选时间</li>
     *     <li>如果没有数据，查询近两个月的往期勾选发票信息（调用API）</li>
     *     <li>判断这张发票在不在近两个月中，如果在设置勾选状态和时间，不在就设置勾选状态为非勾选0</li>
     * </ol>
     *
     * @param invoiceDTO  发票数据
     * @param pushRequest 推送请求对象
     */
    private void checkDeductibleStatus(InvoiceDTO invoiceDTO, InvoicePushRequest pushRequest) {
        log.info("=== 开始查询发票勾选状态 ===");
        log.info("发票代码: {}, 发票号码: {}, 购买方税号: {}", 
                invoiceDTO.getFpdm(), invoiceDTO.getFphm(), invoiceDTO.getGfsh());

        String nsrsbh = invoiceDTO.getGfsh(); // 使用购买方税号
        String fpdm = invoiceDTO.getFpdm();
        String fphm = invoiceDTO.getFphm();

        if (nsrsbh == null || nsrsbh.isEmpty()  || fphm == null) {
            log.warn("发票信息不完整，无法查询勾选状态，设置为未勾选");
            pushRequest.setDeductible("0"); // 未勾选
            return;
        }

        // 1. 先查询 invoice_history 表
        LambdaQueryWrapper<InvoiceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvoiceHistory::getNsrsbh, nsrsbh)
                .eq(InvoiceHistory::getFpdm, fpdm)
                .eq(InvoiceHistory::getFphm, fphm)
                .orderByDesc(InvoiceHistory::getGxrq)
                .last("LIMIT 1");

        InvoiceHistory historyRecord = invoiceHistoryMapper.selectOne(wrapper);

        if (historyRecord != null) {
            log.info("在 invoice_history 表中找到勾选记录");
            log.info("勾选日期: {}, 发票状态: {}", historyRecord.getGxrq(), historyRecord.getFpzt());
            pushRequest.setDeductible("1"); // 已勾选
            pushRequest.setDeductibleDate(historyRecord.getGxrq());
            log.info("设置为已勾选，勾选时间: {}", historyRecord.getGxrq());
            return;
        }

        log.info("在 invoice_history 表中未找到记录，查询近两个月往期勾选发票信息");

        // 2. 查询近两个月的往期勾选发票信息
        List<InvoiceHistoryItemDTO> recentInvoices = queryRecentTwoMonthsHistory(nsrsbh);

        if (recentInvoices == null || recentInvoices.isEmpty()) {
            log.warn("查询近两个月往期勾选发票信息失败或无数据，设置为未勾选");
            pushRequest.setDeductible("0"); // 未勾选
            return;
        }

        // 3. 判断这张发票是否在近两个月中
        Optional<InvoiceHistoryItemDTO> foundInvoice = recentInvoices.stream()
                .filter(item -> fpdm.equals(item.getFpdm()) && fphm.equals(item.getFphm()))
                .findFirst();

        if (foundInvoice.isPresent()) {
            InvoiceHistoryItemDTO item = foundInvoice.get();
            log.info("在近两个月往期勾选发票中找到该发票");
            log.info("勾选日期: {}, 发票状态: {}", item.getGxrq(), item.getFpzt());
            pushRequest.setDeductible("1"); // 已勾选
            pushRequest.setDeductibleDate(item.getGxrq());
        } else {
            log.info("在近两个月往期勾选发票中未找到该发票，设置为未勾选");
            pushRequest.setDeductible("0"); // 未勾选
        }

        log.info("=== 发票勾选状态查询完成 ===");
    }

    /**
     * 查询近两个月的往期勾选发票信息
     * <p>使用缓存避免多次调用API</p>
     *
     * @param nsrsbh 纳税人识别号
     * @return 近两个月的发票列表
     */
    private List<InvoiceHistoryItemDTO> queryRecentTwoMonthsHistory(String nsrsbh) {
        log.info("=== 查询近两个月往期勾选发票信息 ===");
        log.info("纳税人识别号: {}", nsrsbh);

        if (historyQueryApiUrl == null || historyQueryApiUrl.isEmpty()) {
            log.warn("invoice.history.query.api.url 未配置，无法查询往期勾选发票");
            return Collections.emptyList();
        }

        // 计算近两个月的月份
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate twoMonthsAgo = today.minusMonths(2).withDayOfMonth(1);
        String lastMonthStr = YYYYMM.format(lastMonth);
        String twoMonthsAgoStr = YYYYMM.format(twoMonthsAgo);

        log.info("查询月份: {} 和 {}", twoMonthsAgoStr, lastMonthStr);

        List<InvoiceHistoryItemDTO> allInvoices = new ArrayList<>();

        // 查询两个月的数据
        for (String tjyf : Arrays.asList(twoMonthsAgoStr, lastMonthStr)) {
            String cacheKey = nsrsbh + "_" + tjyf;

            // 检查缓存
            if (recentHistoryCache.containsKey(cacheKey)) {
                log.info("使用缓存数据: {}", cacheKey);
                allInvoices.addAll(recentHistoryCache.get(cacheKey));
                continue;
            }

            // 调用API查询
            log.info("调用API查询月份: {}", tjyf);
            List<InvoiceHistoryItemDTO> monthInvoices = queryHistoryByMonth(nsrsbh, tjyf);
            
            if (monthInvoices != null && !monthInvoices.isEmpty()) {
                // 缓存结果
                recentHistoryCache.put(cacheKey, monthInvoices);
                allInvoices.addAll(monthInvoices);
                log.info("查询到 {} 条发票，已缓存", monthInvoices.size());
            } else {
                log.info("月份 {} 无发票数据", tjyf);
            }
        }

        log.info("近两个月共查询到 {} 条发票记录", allInvoices.size());
        return allInvoices;
    }

    /**
     * 查询指定月份的往期勾选发票信息
     *
     * @param nsrsbh 纳税人识别号
     * @param tjyf   统计月份（格式：YYYYMM）
     * @return 发票列表
     */
    private List<InvoiceHistoryItemDTO> queryHistoryByMonth(String nsrsbh, String tjyf) {
        try {
            // 构建请求参数
            InvoiceHistoryQueryRequest request = new InvoiceHistoryQueryRequest();
            request.setNsrsbh(nsrsbh);
            request.setTjyf(tjyf);
            request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));

            log.info("调用往期勾选发票查询接口");
            log.info("URL: {}", historyQueryApiUrl);
            log.info("请求参数: {}", JSON.toJSONString(request));

            String responseStr = bwHttpUtil.httpPostRequest(historyQueryApiUrl, JSON.toJSONString(request), "json");

            log.info("往期勾选发票查询接口响应: {}", responseStr);

            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("往期勾选发票查询失败：响应为空");
                return Collections.emptyList();
            }

            InvoiceHistoryQueryResponse response = JSON.parseObject(responseStr, InvoiceHistoryQueryResponse.class);

            if (response == null || response.getCode() == null || response.getCode() != 0) {
                log.warn("往期勾选发票查询失败：{}", responseStr);
                return Collections.emptyList();
            }

            if (response.getData() == null || response.getData().getFpxxList() == null) {
                log.info("往期勾选发票查询成功，但无发票数据");
                return Collections.emptyList();
            }

            log.info("往期勾选发票查询成功，发票数量: {}", response.getData().getFpxxList().size());
            return response.getData().getFpxxList();
        } catch (Exception e) {
            log.error("查询往期勾选发票异常：纳税人识别号={}, 月份={}", nsrsbh, tjyf, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询单张发票的详细信息
     *
     * @param invoiceDTO 发票数据
     * @return 单票查询结果
     */
    private InvoiceSingleQueryResponse.InvoiceSingleData querySingleInvoice(InvoiceDTO invoiceDTO) {
        log.info("=== 开始单票查询 ===");
        log.info("输入参数 invoiceDTO: {}", JSON.toJSONString(invoiceDTO));
        log.info("单票查询URL: {}", singleQueryApiUrl);

        if (singleQueryApiUrl == null || singleQueryApiUrl.isEmpty()) {
            log.warn("invoice.single.query.api.url 未配置，跳过单票查询");
            return null;
        }

        if (invoiceDTO.getFpdm() == null || invoiceDTO.getFphm() == null) {
            log.warn("发票缺少代码或号码，跳过单票查询");
            return null;
        }

        // 使用购买方识别号
        String nsrsbh = invoiceDTO.getGfsh();
        log.info("使用购买方识别号: {}", nsrsbh);

        if (nsrsbh == null || nsrsbh.isEmpty()) {
            log.warn("发票缺少购买方纳税人识别号，跳过单票查询：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
            return null;
        }

        try {
            // 构建请求参数
            InvoiceSingleQueryRequest request = new InvoiceSingleQueryRequest();
            request.setFpdm(invoiceDTO.getFpdm());
            request.setFphm(invoiceDTO.getFphm());
            request.setNsrsbh(nsrsbh);
            request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));

            log.info("=== 调用单票查询接口 ===");
            log.info("URL: {}", singleQueryApiUrl);
            log.info("请求参数: {}", JSON.toJSONString(request));

            String responseStr = bwHttpUtil.httpPostRequest(singleQueryApiUrl, JSON.toJSONString(request), "json");

            log.info("=== 单票查询接口响应 ===");
            log.info("发票代码: {}, 发票号码: {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
            log.info("响应结果: {}", responseStr);

            if (responseStr == null || responseStr.isEmpty()) {
                log.warn("单票查询失败：{} {}，响应为空", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
                return null;
            }

            InvoiceSingleQueryResponse response = JSON.parseObject(responseStr, InvoiceSingleQueryResponse.class);

            if (response == null || response.getCode() == null || response.getCode() != 0) {
                log.warn("单票查询失败：{} {}，响应：{}", invoiceDTO.getFpdm(), invoiceDTO.getFphm(), responseStr);
                return null;
            }

            if (response.getData() == null) {
                log.warn("单票查询返回空数据：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm());
                return null;
            }

            log.info("=== 单票查询成功 ===");
            log.info("返回数据: {}", JSON.toJSONString(response.getData()));
            return response.getData();
        } catch (Exception e) {
            log.error("=== 单票查询异常 ===");
            log.error("单票查询异常：{} {}", invoiceDTO.getFpdm(), invoiceDTO.getFphm(), e);
            return null;
        }
    }

    /**
     * 通过全量进项发票查询接口查询并设置勾选状态和勾选时间
     *
     * @param invoiceDTO  发票数据
     * @param pushRequest 推送请求
     */
    private void queryAndSetDeductibleStatus(InvoiceDTO invoiceDTO, InvoicePushRequest pushRequest) {
        // 使用购买方识别号
        String nsrsbh = invoiceDTO.getGfsh();
        String fpdm = invoiceDTO.getFpdm();
        String fphm = invoiceDTO.getFphm();
        String zpfphm = invoiceDTO.getZpfphm();

        if (nsrsbh == null || nsrsbh.isEmpty() || fphm == null) {
            log.info("发票信息不完整，无法查询勾选状态，设置为未勾选");
            pushRequest.setDeductible("0");
            return;
        }

        // 调用查询接口
        log.info("查询发票勾选状态，发票号: {}", fphm);
        InvoiceFullItemDTO fullInvoice = queryFullInvoice(nsrsbh, fpdm, fphm, zpfphm);

        if (fullInvoice == null) {
            log.info("全量发票查询未找到该发票，设置为未勾选");
            pushRequest.setDeductible("0");
            return;
        }

        // 从查询结果中获取勾选状态和勾选时间
        String gxzt = fullInvoice.getGxzt();
        String gxsj = fullInvoice.getGxsj();
        String fpzt = fullInvoice.getFpzt();
        pushRequest.setState(fpzt);

        log.info("查询到发票勾选状态: {}, 勾选时间: {}", gxzt, gxsj);

        // 设置勾选状态：1-已勾选，0-未勾选
        if ("1".equals(gxzt)) {
            pushRequest.setDeductible("1");
            pushRequest.setDeductibleDate(gxsj);
            log.info("设置为已勾选，勾选时间: {}", gxsj);
        } else {
            pushRequest.setDeductible("0");
            log.info("勾选状态为 {}，设置为未勾选", gxzt);
        }
    }

    /**
     * 调用全量进项发票查询接口
     *
     * @param nsrsbh 纳税人识别号
     * @param fpdm   发票代码
     * @param fphm   发票号码
     * @param zpfphm 专票发票号码
     * @return 发票信息，查询失败返回null
     */
    private InvoiceFullItemDTO queryFullInvoice(String nsrsbh, String fpdm, String fphm, String zpfphm) {
        if (fullQueryApiUrl == null || fullQueryApiUrl.isEmpty()) {
            log.info("invoice.full.query.api.url 未配置，跳过全量发票查询");
            return null;
        }
        
        try {
            // 构建请求参数
            InvoiceFullQueryRequest request = new InvoiceFullQueryRequest();
            request.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
            request.setNsrsbh(nsrsbh);
            request.setFpdm(fpdm);
            request.setFphm(fphm);
            request.setPageNumber("1");
            request.setPageSize("1");

            log.info("调用发票查询接口，发票号={}", fphm);
            String responseStr = bwHttpUtil.httpPostRequest(fullQueryApiUrl, JSON.toJSONString(request), "json");

            if (responseStr == null || responseStr.isEmpty()) {
                log.info("查询响应为空, 发票号={}", fphm);
                return null;
            }

            InvoiceFullQueryResponse response = JSON.parseObject(responseStr, InvoiceFullQueryResponse.class);

            if (response == null || response.getCode() == null || response.getCode() != 0) {
                log.info("查询失败, 发票号={}, 响应码={}", fphm,
                        response != null ? response.getCode() : "null");
                return null;
            }

            if (response.getData() == null || response.getData().isEmpty()) {
                log.info("查询返回空数据, 发票号={}", fphm);
                return null;
            }

            // 返回第一条结果
            InvoiceFullItemDTO invoice = response.getData().get(0);
            log.info("查询成功, 发票号={}, 勾选状态={}", fphm, invoice.getGxzt());
            
            // 保存到采集记录表
            saveInvoiceCollectionRecordIfAbsent(invoice, zpfphm);
            
            return invoice;
        } catch (Exception e) {
            log.error("发票查询异常, 发票号={}", fphm, e);
            return null;
        }
    }

    /**
     * 使用全量接口返回结果将发票保存到发票采集记录表（基础信息），如果不存在则插入
     */
    private void saveInvoiceCollectionRecordIfAbsent(InvoiceFullItemDTO fullInvoice, String zpfphm) {
        if (fullInvoice == null ||  fullInvoice.getFphm() == null) {
            return;
        }
        
        // 查询发票是否已存在
        LambdaQueryWrapper<InvoiceCollectionRecord> wrapper = new LambdaQueryWrapper<InvoiceCollectionRecord>()
                .eq(InvoiceCollectionRecord::getInvoiceNumber, fullInvoice.getFphm())
                .last("LIMIT 1");

        if (invoiceCollectionRecordMapper.selectOne(wrapper) != null) {
            return;
        }

        // 创建记录对象
        InvoiceCollectionRecord record = new InvoiceCollectionRecord();
        record.setInvoiceType(fullInvoice.getFplxdm());
        record.setInvoiceCode(fullInvoice.getFpdm());
        record.setInvoiceNumber(fullInvoice.getFphm());
        record.setZpfphm(zpfphm);

        String kprq = fullInvoice.getKprq();
        if (kprq != null && !kprq.isEmpty()) {
            try {
                if (kprq.length() == 10) {
                    record.setInvoiceDate(LocalDate.parse(kprq, DATE_FORMATTER).atStartOfDay());
                } else {
                    record.setInvoiceDate(LocalDateTime.parse(kprq, DATE_TIME_FORMATTER));
                }
            } catch (Exception e) {
                // ignore parse error, keep invoiceDate null
            }
        }

        record.setBuyerName(fullInvoice.getGfmc());
        record.setBuyerTaxNo(fullInvoice.getGfsh());
        record.setSellerName(fullInvoice.getXfmc());
        record.setSellerTaxNo(fullInvoice.getXfsh());
        record.setTotalAmountTax(fullInvoice.getJshj());

        // 记账状态来自全量查询的 rzyt
        record.setEntryStatus(fullInvoice.getRzyt());

        // 发票状态、勾选状态和勾选时间来自全量查询
        record.setInvoiceStatus(fullInvoice.getFpzt());
        record.setCheckStatus(fullInvoice.getGxzt());

        String gxsj = fullInvoice.getGxsj();
        if(gxsj!= null){
            record.setCheckDate(LocalDate.parse(gxsj, DATE_FORMATTER));
        }

        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        
        log.info("保存发票采集记录, 发票号={}", fullInvoice.getFphm());
        invoiceCollectionRecordMapper.insert(record);
    }


    private BigDecimal convertToBigDecimal(String value) {
        return convertToBigDecimal(value, BigDecimal.ZERO);
    }

    // 字符串转BigDecimal（带自定义默认值）
    private BigDecimal convertToBigDecimal(String value, BigDecimal defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }

        try {
            // 处理特殊字符（如逗号分隔的千分位）
            String cleanValue = value.replace(",", "")
                    .replace(" ", "");
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("数值转换失败: '{}', 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 智能转换税率字符串为BigDecimal
     * @param rateStr 税率字符串（支持小数或百分比）
     * @return 转换后的税率值，无效输入返回BigDecimal.ZERO
     */
    public static BigDecimal parseTaxRate(String rateStr) {
        if (StringUtils.isBlank(rateStr)) {
            return BigDecimal.ZERO;
        }

        String cleanRate = rateStr.trim().replaceAll("\\s+", "");

        try {
            // 百分比格式处理
            if (cleanRate.endsWith("%")) {
                String numberPart = cleanRate.substring(0, cleanRate.length() - 1);
                BigDecimal value = new BigDecimal(numberPart);
                return value.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            // 小数格式处理
            else {
                BigDecimal value = new BigDecimal(cleanRate);

                // 智能修正：如果输入是整数且范围在1-100之间，当作百分比处理
                if (value.scale() <= 0 &&
                        value.compareTo(BigDecimal.ONE) > 0 &&
                        value.compareTo(BigDecimal.valueOf(100)) <= 0) {
                    return value.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                return value;
            }
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 安全转换并验证税率
     */
    public static BigDecimal safeParseTaxRate(String rateStr) {
        BigDecimal rate = parseTaxRate(rateStr);

        // 验证合理范围 (0-1.0)
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            // 特殊处理：增值税普通发票可能超过100%（如免税）
            return rate.compareTo(BigDecimal.valueOf(100)) <= 0 ?
                    rate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;
        }
        return rate;
    }

}

