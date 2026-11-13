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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/invoices")
@PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
public class InvoiceController extends BaseController<Invoice, InvoiceDTO, PagedFilter, InvoiceService> {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        super(invoiceService);
        this.invoiceService = invoiceService;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
    public Response<InvoiceDTO> create(@Valid @RequestBody InvoiceDTO dto) {
        return super.create(dto);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
    public Response<InvoiceDTO> update(@PathVariable Long id, @Valid @RequestBody InvoiceDTO dto) {
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
    public Response<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
    public Response<InvoiceDTO> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return Response.success(invoiceService.changeStatus(id, status));
    }

    @GetMapping("/by-status")
    public Response<List<InvoiceDTO>> getByStatus(
            @RequestParam String status) {
        return Response.success(invoiceService.getByStatus(status));
    }

    @GetMapping("/by-student/{studentId}")
    public Response<List<InvoiceDTO>> getByStudent(
            @PathVariable Long studentId) {
        return Response.success(invoiceService.getByStudent(studentId));
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

    @GetMapping("/by-stamp-number")
    public Response<List<InvoiceDTO>> getByStampNumber(
            @RequestParam String stampNumber) {
        return Response.success(invoiceService.getByStampNumber(stampNumber));
    }

    @GetMapping("/by-customer-ruc")
    public Response<List<InvoiceDTO>> getByCustomerRuc(
            @RequestParam String ruc) {
        return Response.success(invoiceService.getByCustomerRuc(ruc));
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

    @GetMapping("/search-by-customer-name")
    public Response<List<InvoiceDTO>> searchByCustomerName(
            @RequestParam String customerName) {
        return Response.success(invoiceService.searchByCustomerName(customerName));
    }
}
