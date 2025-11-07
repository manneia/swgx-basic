package com.manneia.swgx.basic.controller;

import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.model.request.PushInvoiceDocRequest;
import com.manneia.swgx.basic.model.response.PushInvoiceDocResponse;
import com.manneia.swgx.basic.service.invoice.InvoiceDocService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public SingleResponse<PushInvoiceDocResponse> pushInvoiceDoc(@RequestBody @Valid PushInvoiceDocRequest pushInvoiceDocRequest) {
        return invoiceDocService.pushInvoiceDoc(pushInvoiceDocRequest);
    }
}
