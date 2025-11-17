package com.manneia.swgx.basic.service;

import com.alibaba.fastjson.JSON;
import com.manneia.swgx.basic.common.constant.TaxpayerRegistry;
import com.manneia.swgx.basic.common.request.SyncRequest;
import com.manneia.swgx.basic.common.response.SyncResponse;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

	@Value("${invoice.sync.api.url:}")
	private String apiUrl;

	@Resource
	private InvoiceService invoiceService;

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

		// 计算本月日期范围：从本月1号到今天
		LocalDate today = LocalDate.now();
		LocalDate start = today.withDayOfMonth(1); // 本月1号

		// 固定参数
		String qqlx = "3"; // 固定为3
		String zzhlx = ""; // 不传

		List<String> taxpayerList = TaxpayerRegistry.getTaxpayerList();
		if (taxpayerList == null || taxpayerList.isEmpty()) {
			log.warn("纳税人识别号列表为空，跳过本次同步");
			return;
		}

		// 遍历每个纳税人识别号执行同步
		for (String nsrsbh : taxpayerList) {
			try {
				log.info("开始同步纳税人识别号：{}", nsrsbh);

				SyncRequest req = new SyncRequest();
				req.setNsrsbh(nsrsbh);
				req.setQqlx(qqlx);
				req.setKprqq(YYYYMMDD.format(start));
				req.setKprqz(YYYYMMDD.format(today));
				req.setCzlsh(UUID.randomUUID().toString().replace("-", ""));
				req.setZzhlx(zzhlx);

				log.info("调用发票同步接口，url={}，纳税人识别号={}，请求参数={}", apiUrl, nsrsbh, JSON.toJSONString(req));

				String responseStr = bwHttpUtil.httpPostRequest(apiUrl, JSON.toJSONString(req), "json");

				log.info("发票同步接口返回，url={}，纳税人识别号={}，响应={}", apiUrl, nsrsbh, responseStr);

				if (responseStr == null || responseStr.isEmpty()) {
					log.warn("纳税人识别号 {} 同步返回为空", nsrsbh);
					continue;
				}

				SyncResponse response = JSON.parseObject(responseStr, SyncResponse.class);

				if (response != null) {
					log.info("纳税人识别号 {} 同步完成 success={} code={} msg={} dataSize={}",
							nsrsbh, response.getSuccess(), response.getCode(), response.getMsg(),
							response.getData() == null ? 0 : response.getData().size());

					// 处理返回的发票数据：提取指定类型、保存、推送
					if (response.getData() != null && !response.getData().isEmpty()) {
						invoiceService.processInvoices(response.getData());
					}
				} else {
					log.warn("纳税人识别号 {} 同步解析结果为空", nsrsbh);
				}
			} catch (Exception ex) {
				log.error("纳税人识别号 {} 同步调用异常", nsrsbh, ex);
				// 继续执行下一个，不中断整个任务
			}
		}

		log.info("所有纳税人识别号同步任务完成");
	}
}


