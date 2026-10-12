package com.jerokysys.invoices.repo;

import java.math.BigDecimal;
import java.util.List;

import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.repository.BaseRepository;

public interface InvoiceDetailRepo extends BaseRepository<InvoiceDetail, Long> {

    List<InvoiceDetail> findByInvoiceId(Long invoiceId);

    List<InvoiceDetail> findByCorrespondingMonth(String correspondingMonth);

    List<InvoiceDetail> findByVatRate(Integer vatRate);

    List<InvoiceDetail> findByDescriptionContaining(String text);

    List<InvoiceDetail> findByAmountBetween(BigDecimal min, BigDecimal max);
}