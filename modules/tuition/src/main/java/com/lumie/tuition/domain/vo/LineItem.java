package com.lumie.tuition.domain.vo;


/**
 * A single line item within a TuitionInvoice.
 * Stored as JSONB in the tuition_invoices.items column.
 * Money values are represented as plain BigDecimal (KRW, no cents).
 */
public record LineItem(
        String name,
        int quantity,
        Money unitPrice,
        Money amount
) {
    public static LineItem of(String name, int quantity, Money unitPrice) {
        return new LineItem(name, quantity, unitPrice, unitPrice.multiply(quantity));
    }
}
