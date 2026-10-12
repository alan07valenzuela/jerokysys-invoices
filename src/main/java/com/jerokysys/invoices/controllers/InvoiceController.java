package com.jerokysys.invoices.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.Invoice;
import com.jerokysys.dto.InvoiceDTO;
import com.jerokysys.invoices.services.InvoiceService;

@RestController
@RequestMapping("/invoices")
public class InvoiceController extends BaseController<Invoice, InvoiceDTO, PagedFilter, InvoiceService> {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        super(invoiceService);
        this.invoiceService = invoiceService;
    }

    @PatchMapping("/{id}/status")
    public Response<InvoiceDTO> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return Response.success(invoiceService.changeStatus(id, status));
    }

    @GetMapping("/by-student/{studentId}")
    public Response<List<InvoiceDTO>> getByStudent(
            @PathVariable Long studentId) {
        return Response.success(invoiceService.getByStudent(studentId));
    }

    @GetMapping("/by-status")
    public Response<List<InvoiceDTO>> getByStatus(
            @RequestParam String status) {
        return Response.success(invoiceService.getByStatus(status));
    }

    @GetMapping("/by-sale-condition")
    public Response<List<InvoiceDTO>> getBySaleCondition(
            @RequestParam String condition) {
        return Response.success(invoiceService.getBySaleCondition(condition));
    }

    @GetMapping("/by-number")
    public Response<List<InvoiceDTO>> getByNumber(
            @RequestParam String invoiceNumber) {
        return Response.success(invoiceService.getByInvoiceNumber(invoiceNumber));
    }

    @GetMapping("/by-timbrado")
    public Response<List<InvoiceDTO>> getByTimbrado(
            @RequestParam String timbrado) {
        return Response.success(invoiceService.getByTimbrado(timbrado));
    }

    @GetMapping("/by-tax-id")
    public Response<List<InvoiceDTO>> getByTaxId(
            @RequestParam String taxId) {
        return Response.success(invoiceService.getByTaxId(taxId));
    }

    @GetMapping("/by-date")
    public Response<List<InvoiceDTO>> getByDate(
            @RequestParam LocalDate date) {
        return Response.success(invoiceService.getByIssueDate(date));
    }

    @GetMapping("/by-date-range")
    public Response<List<InvoiceDTO>> getByDateRange(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return Response.success(invoiceService.getByIssueDateRange(from, to));
    }

    @GetMapping("/by-amount-range")
    public Response<List<InvoiceDTO>> getByAmountRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return Response.success(invoiceService.getByAmountRange(min, max));
    }

    @GetMapping("/search-by-business-name")
    public Response<List<InvoiceDTO>> searchByBusinessName(
            @RequestParam String businessName) {
        return Response.success(invoiceService.searchByBusinessName(businessName));
    }
}