package com.jerokysys.invoices.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.Response;
import com.jerokysys.dto.InvoiceDetailDTO;
import com.jerokysys.invoices.services.InvoiceDetailService;

/**
 * Las lineas de detalle son de solo lectura: se escriben dentro de la factura.
 * Por eso este controller NO extiende BaseController, que expondría POST, PUT y
 * DELETE sobre rutas que el contrato no define.
 */
@RestController
@RequestMapping("/invoice-details")
public class InvoiceDetailController {

    private final InvoiceDetailService invoiceDetailService;

    public InvoiceDetailController(InvoiceDetailService invoiceDetailService) {
        this.invoiceDetailService = invoiceDetailService;
    }

    @GetMapping
    public Response<PageResult<InvoiceDetailDTO>> getByFilter(@ModelAttribute PagedFilter filter) {
        return Response.success(invoiceDetailService.getByFilter(filter));
    }

    @GetMapping("/{id}")
    public Response<InvoiceDetailDTO> getById(@PathVariable Long id) {
        return Response.success(invoiceDetailService.getById(id));
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public Response<List<InvoiceDetailDTO>> getByInvoice(@PathVariable Long invoiceId) {
        return Response.success(invoiceDetailService.getByInvoice(invoiceId));
    }

    @GetMapping("/by-month")
    public Response<List<InvoiceDetailDTO>> getByMonth(@RequestParam String month) {
        return Response.success(invoiceDetailService.getByCorrespondingMonth(month));
    }

    @GetMapping("/by-vat-rate")
    public Response<List<InvoiceDetailDTO>> getByVatRate(@RequestParam Integer vatRate) {
        return Response.success(invoiceDetailService.getByVatRate(vatRate));
    }

    @GetMapping("/by-concept")
    public Response<List<InvoiceDetailDTO>> getByConcept(
            @RequestParam String conceptType,
            @RequestParam Long conceptId) {
        return Response.success(invoiceDetailService.getByConcept(conceptType, conceptId));
    }

    @GetMapping("/by-amount-range")
    public Response<List<InvoiceDetailDTO>> getByAmountRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return Response.success(invoiceDetailService.getByAmountRange(min, max));
    }

    @GetMapping("/search-by-description")
    public Response<List<InvoiceDetailDTO>> searchByDescription(@RequestParam String description) {
        return Response.success(invoiceDetailService.searchByDescription(description));
    }
}