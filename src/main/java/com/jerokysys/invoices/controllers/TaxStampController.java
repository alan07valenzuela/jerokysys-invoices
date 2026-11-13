package com.jerokysys.invoices.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.TaxStamp;
import com.jerokysys.dto.TaxStampDTO;
import com.jerokysys.invoices.services.TaxStampService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tax-stamps")
@PreAuthorize("hasAnyRole('ADMIN','SECRETARY')")
public class TaxStampController extends BaseController<TaxStamp, TaxStampDTO, PagedFilter, TaxStampService> {

    private final TaxStampService taxStampService;

    public TaxStampController(TaxStampService taxStampService) {
        super(taxStampService);
        this.taxStampService = taxStampService;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Response<TaxStampDTO> create(@Valid @RequestBody TaxStampDTO dto) {
        return super.create(dto);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<TaxStampDTO> update(@PathVariable Long id, @Valid @RequestBody TaxStampDTO dto) {
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @GetMapping("/valid")
    public Response<List<TaxStampDTO>> getValid(
            @RequestParam(required = false) LocalDate date) {
        return Response.success(taxStampService.getValid(date));
    }
}
