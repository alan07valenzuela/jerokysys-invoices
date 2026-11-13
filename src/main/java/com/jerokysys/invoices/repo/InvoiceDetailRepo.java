package com.jerokysys.invoices.repo;

import java.math.BigDecimal;
import java.util.List;

import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.domain.enums.InvoiceConceptType;
import com.jerokysys.commons.domain.enums.VatType;
import com.jerokysys.commons.repository.BaseRepository;

public interface InvoiceDetailRepo extends BaseRepository<InvoiceDetail, Long> {

    List<InvoiceDetail> findByInvoiceId(Long invoiceId);

    List<InvoiceDetail> findByCorrespondingMonth(String correspondingMonth);

    List<InvoiceDetail> findByVatType(VatType vatType);

    List<InvoiceDetail> findByConceptTypeAndConceptId(InvoiceConceptType conceptType, Long conceptId);

    List<InvoiceDetail> findByDescriptionContaining(String text);

    List<InvoiceDetail> findBySubtotalBetween(BigDecimal min, BigDecimal max);
}
