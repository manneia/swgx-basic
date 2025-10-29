package com.manneia.swgx.basic.service.invoice;

import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.model.request.PushInvoiceDocRequest;
import com.manneia.swgx.basic.model.response.PushInvoiceDocResponse;

/**
 * @author lkx
 * @description 发票单据服务
 * @created 2025-10-29 16:34:15
 */
public interface InvoiceDocService {

    /**
     * 推送发票单据并生成开票链接
     *
     * @param pushInvoiceDocRequest 请求参数
     *
     * @return 结果
     */
    SingleResponse<PushInvoiceDocResponse> pushInvoiceDoc(PushInvoiceDocRequest pushInvoiceDocRequest);
}
