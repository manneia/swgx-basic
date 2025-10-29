package com.manneia.swgx.basic.controller;

import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.model.request.PushInvoiceDocRequest;
import com.manneia.swgx.basic.model.response.PushInvoiceDocResponse;
import com.manneia.swgx.basic.service.invoice.InvoiceDocService;
import com.manneia.swgx.basic.vo.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @author lkx
 * @description com.manneia.swgx.basic.controller
 * @created 2025-10-29 16:15:42
 */
@RestController
@RequestMapping("invoiceDoc")
public class InvoiceDocController {

    @Resource
    private InvoiceDocService invoiceDocService;

    @PostMapping(value = "push")
    public Result<SingleResponse<PushInvoiceDocResponse>> pushInvoiceDoc(@RequestBody @Valid PushInvoiceDocRequest pushInvoiceDocRequest) {
        return Result.success(invoiceDocService.pushInvoiceDoc(pushInvoiceDocRequest));
    }
}
