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
import java.util.List;
import java.util.UUID;

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

	@Value("${invoice.payment.query.api.url:}")
	private String customsApiUrl;

	@Resource
	private InvoiceService invoiceService;

	@Resource
	private InvoiceHistoryService invoiceHistoryService;

	@Resource
	private BwHttpUtil bwHttpUtil;

    /**
     * 每三小时执行一次：整点、3点、6点…
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void syncInvoices() {
        if (apiUrl == null || apiUrl.isEmpty()) {
            log.warn("invoice.sync.api.url 未配置，跳过本次同步");
            return;
        }

        // 固定参数
        String qqlx = "3"; // 固定为3
        String zzhlx = ""; // 不传
        LocalDate today = LocalDate.now();

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList == null || taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过本次同步");
            return;
        }

        // 遍历每个纳税人识别号执行同步
        for (String nsrsbh : taxpayerList) {
            try {
                log.info("开始同步纳税人识别号：{}", nsrsbh);
                List<InvoiceDTO> allInvoices = new ArrayList<>();

                // 最近半年，按月分6次查询
                for (int i = 0; i < 6; i++) {
                    LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    if (monthEnd.isAfter(today)) {
                        monthEnd = today;
                    }

                    SyncRequest req = new SyncRequest();
                    req.setNsrsbh(nsrsbh);
                    req.setQqlx(qqlx);
                    req.setKprqq(YYYYMMDD.format(monthStart));
                    req.setKprqz(YYYYMMDD.format(monthEnd));
                    req.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
                    req.setZzhlx(zzhlx);

                    log.info("调用发票同步接口，url={}，纳税人识别号={}，请求参数={}", apiUrl, nsrsbh, JSON.toJSONString(req));

                    String responseStr = bwHttpUtil.httpPostRequest(apiUrl, JSON.toJSONString(req), "json");

                    log.info("发票同步接口返回，url={}，纳税人识别号={}，响应={}", apiUrl, nsrsbh, responseStr);

                    if (responseStr == null || responseStr.isEmpty()) {
                        log.warn("纳税人识别号 {} 月度 {}-{} 同步返回为空", nsrsbh,
                                YYYYMMDD.format(monthStart), YYYYMMDD.format(monthEnd));
                        continue;
                    }

                    SyncResponse response = JSON.parseObject(responseStr, SyncResponse.class);

                    if (response != null) {
                        int size = response.getFpxxList() == null ? 0 : response.getFpxxList().size();
                        log.info("纳税人识别号 {} 月度 {}-{} 同步完成 success={} code={} msg={} fpxxListSize={}",
                                nsrsbh, YYYYMMDD.format(monthStart), YYYYMMDD.format(monthEnd),
                                response.getSuccess(), response.getCode(), response.getMsg(), size);

                        if (size > 0) {
                            allInvoices.addAll(response.getFpxxList());
                        }
                    } else {
                        log.warn("纳税人识别号 {} 月度 {}-{} 同步解析结果为空", nsrsbh,
                                YYYYMMDD.format(monthStart), YYYYMMDD.format(monthEnd));
                    }
                }

                // 所有月份查询完成后再统一处理
                if (!allInvoices.isEmpty()) {
                    log.info("纳税人识别号 {} 最近半年汇总发票数量：{}，开始处理", nsrsbh, allInvoices.size());
                    invoiceService.processInvoices(allInvoices);
                } else {
                    log.info("纳税人识别号 {} 最近半年无需要处理的发票", nsrsbh);
                }
            } catch (Exception ex) {
                log.error("纳税人识别号 {} 同步调用异常", nsrsbh, ex);
                // 继续执行下一个，不中断整个任务
            }
        }

        log.info("所有纳税人识别号同步任务完成");
    }

    /**
     * 海关缴款书定时查询任务：每三小时执行一次
     */
    @Scheduled(cron = "0 10 */3 * * ?")
    public void syncCustomsPayments() {
        if (customsApiUrl == null || customsApiUrl.isEmpty()) {
            log.warn("customs.payment.query.api.url 未配置，跳过本次海关缴款书查询");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(5).withDayOfMonth(1); // 半年前的第一天

        List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
        if (taxpayerList == null || taxpayerList.isEmpty()) {
            log.warn("纳税人识别号列表为空，跳过本次海关缴款书查询");
            return;
        }

        for (String nsrsbh : taxpayerList) {
            try {
                log.info("开始查询纳税人 {} 的海关缴款书", nsrsbh);

                CustomsPaymentQueryRequest req = new CustomsPaymentQueryRequest();
                req.setNsrsbh(nsrsbh);
                req.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
                req.setTfrqq(YYYYMMDD.format(start));
                req.setTfrqz(YYYYMMDD.format(today));

                String reqJson = JSON.toJSONString(req);
                log.info("调用海关缴款书查询接口，url={}，纳税人识别号={}，请求参数={}", customsApiUrl, nsrsbh, reqJson);

                String responseStr = bwHttpUtil.httpPostRequest(customsApiUrl, reqJson, "json");

                log.info("海关缴款书查询接口返回，url={}，纳税人识别号={}，响应={}", customsApiUrl, nsrsbh, responseStr);

                if (responseStr == null || responseStr.isEmpty()) {
                    log.warn("纳税人识别号 {} 海关缴款书查询返回为空", nsrsbh);
                    continue;
                }

                CustomsPaymentQueryResponse response = JSON.parseObject(responseStr, CustomsPaymentQueryResponse.class);

                if (response != null) {
                    List<CustomsPaymentQueryResponse.CustomsPaymentItem> items = response.getData();
                    int size = items == null ? 0 : items.size();
                    log.info("纳税人 {} 海关缴款书查询完成 code={} msg={} dataSize={}",
                            nsrsbh, response.getCode(), response.getMsg(), size);

                    if (items != null && !items.isEmpty()) {
                        List<InvoiceDTO> invoiceList = new ArrayList<>();
                        for (CustomsPaymentQueryResponse.CustomsPaymentItem item : items) {
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
					log.warn("纳税人 {} 海关缴款书查询解析结果为空", nsrsbh);
				}
			} catch (Exception ex) {
				log.error("纳税人 {} 海关缴款书查询调用异常", nsrsbh, ex);
			}
		}

		log.info("所有纳税人识别号海关缴款书查询任务完成");
	}

	/**
	 * 每月1号凌晨执行一次：查询往期勾选发票信息
	 * 查询范围：两个月前的发票数据
	 */
//	@Scheduled(cron = "0 0 3 1 * ?")
	public void syncHistoryInvoices() {
		log.info("=== 开始往期勾选发票信息同步任务 ===");

		List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
		if (taxpayerList == null || taxpayerList.isEmpty()) {
			log.warn("纳税人识别号列表为空，跳过往期勾选发票同步");
			return;
		}

		// 计算查询的月份：两个月前
		LocalDate today = LocalDate.now();
		LocalDate twoMonthsAgo = today.minusMonths(2).withDayOfMonth(1); // 两个月前的1号
		String tjyf = YYYYMM.format(twoMonthsAgo);

		log.info("查询月份：{}", tjyf);

		// 遍历每个纳税人识别号
		for (String nsrsbh : taxpayerList) {
			try {
				log.info("查询纳税人 {} 月份 {} 的往期勾选发票", nsrsbh, tjyf);
				invoiceHistoryService.queryAndSaveHistoryInvoices(nsrsbh, tjyf);
				log.info("纳税人 {} 的往期勾选发票信息查询完成", nsrsbh);
			} catch (Exception e) {
				log.error("查询纳税人 {} 月份 {} 的往期勾选发票异常", nsrsbh, tjyf, e);
				// 继续处理下一个纳税人，不中断整个任务
			}
		}

		log.info("=== 往期勾选发票信息同步任务完成 ===");
	}
}


