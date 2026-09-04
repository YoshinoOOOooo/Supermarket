package com.supermarket.vo;

import java.math.BigDecimal;

public final class CheckoutItemView {
    private final String productCode;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal payableAmount;

    public CheckoutItemView(String productCode, String productName, Integer quantity,
                            BigDecimal unitPrice, BigDecimal originalAmount,
                            BigDecimal discountAmount, BigDecimal payableAmount) {
        this.productCode = productCode;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
}
