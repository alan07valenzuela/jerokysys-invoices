package com.jerokysys.invoices.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.domain.enums.VatType;

class InvoiceTotalsCalculatorTest {

    @Test
    void calculatesGrossSalesAndVatLiquidation() {
        InvoiceDetail productA = detail("100000.00", VatType.GRAVADA_10);
        InvoiceDetail rent = detail("100000.00", VatType.GRAVADA_5);
        InvoiceDetail productB = detail("30000.00", VatType.EXENTA);

        InvoiceTotalsCalculator.Totals totals = InvoiceTotalsCalculator.calculate(
                List.of(productA, rent, productB));

        assertEquals(new BigDecimal("30000.00"), totals.exempt());
        assertEquals(new BigDecimal("100000.00"), totals.sales5());
        assertEquals(new BigDecimal("100000.00"), totals.sales10());
        assertEquals(new BigDecimal("4761.90"), totals.iva5());
        assertEquals(new BigDecimal("9090.91"), totals.iva10());
        assertEquals(new BigDecimal("230000.00"), totals.total());
    }

    @Test
    void emptyDetailsProduceZeroTotals() {
        InvoiceTotalsCalculator.Totals totals = InvoiceTotalsCalculator.calculate(List.of());

        assertEquals(BigDecimal.ZERO, totals.exempt());
        assertEquals(BigDecimal.ZERO, totals.sales5());
        assertEquals(BigDecimal.ZERO, totals.sales10());
        assertEquals(new BigDecimal("0.00"), totals.iva5());
        assertEquals(new BigDecimal("0.00"), totals.iva10());
        assertEquals(BigDecimal.ZERO, totals.total());
    }

    private InvoiceDetail detail(String subtotal, VatType vatType) {
        InvoiceDetail detail = new InvoiceDetail();
        detail.setSubtotal(new BigDecimal(subtotal));
        detail.setVatType(vatType);
        return detail;
    }
}
