package com.jerokysys.invoices.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.jerokysys.commons.domain.entity.Invoice;
import com.jerokysys.commons.domain.enums.InvoiceStatus;
import com.jerokysys.commons.domain.enums.SaleCondition;
import com.jerokysys.commons.repository.BaseRepository;

public interface InvoiceRepo extends BaseRepository<Invoice, Long> {

    List<Invoice> findByStudentId(Long studentId);

    List<Invoice> findByPaymentStatus(InvoiceStatus paymentStatus);

    List<Invoice> findBySaleCondition(SaleCondition saleCondition);

    List<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByStampNumber(String stampNumber);

    List<Invoice> findByCustomerRuc(String customerRuc);

    List<Invoice> findByIssueDate(LocalDate date);

    List<Invoice> findByIssueDateBetween(LocalDate from, LocalDate to);

    List<Invoice> findByTotalAmountBetween(BigDecimal min, BigDecimal max);

    List<Invoice> findByCustomerNameContainingIgnoreCase(String customerName);

    boolean existsByTaxStampIdAndSequenceNumber(Long taxStampId, Integer sequenceNumber);
}
