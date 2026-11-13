package com.jerokysys.invoices.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.jerokysys.commons.domain.entity.InvoiceDetail;
import com.jerokysys.commons.domain.enums.VatType;

/**
 * Calcula los importes de una factura a partir de sus lineas.
 *
 * <p>Los subtotales de las lineas ya incluyen IVA. El IVA no se suma al total:
 * se liquida sobre las ventas de cada tasa.</p>
 */
final class InvoiceTotalsCalculator {

    private static final BigDecimal DIVISOR_5 = BigDecimal.valueOf(21);
    private static final BigDecimal DIVISOR_10 = BigDecimal.valueOf(11);

    private InvoiceTotalsCalculator() {
    }

    static Totals calculate(List<InvoiceDetail> details) {
        BigDecimal exempt = BigDecimal.ZERO;
        BigDecimal sales5 = BigDecimal.ZERO;
        BigDecimal sales10 = BigDecimal.ZERO;

        if (details != null) {
            for (InvoiceDetail detail : details) {
                BigDecimal subtotal = detail.getSubtotal() == null
                        ? BigDecimal.ZERO
                        : detail.getSubtotal();
                VatType vatType = detail.getVatType();
                if (vatType == null) {
                    throw new IllegalArgumentException("Every detail line needs a vatType");
                }
                switch (vatType) {
                    case EXENTA -> exempt = exempt.add(subtotal);
                    case GRAVADA_5 -> sales5 = sales5.add(subtotal);
                    case GRAVADA_10 -> sales10 = sales10.add(subtotal);
                }
            }
        }

        BigDecimal iva5 = sales5.divide(DIVISOR_5, 2, RoundingMode.HALF_UP);
        BigDecimal iva10 = sales10.divide(DIVISOR_10, 2, RoundingMode.HALF_UP);
        BigDecimal total = exempt.add(sales5).add(sales10);

        return new Totals(exempt, sales5, sales10, iva5, iva10, total);
    }

    record Totals(
            BigDecimal exempt,
            BigDecimal sales5,
            BigDecimal sales10,
            BigDecimal iva5,
            BigDecimal iva10,
            BigDecimal total) {
    }
}
