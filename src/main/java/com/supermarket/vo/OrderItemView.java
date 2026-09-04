package com.supermarket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supermarket.config.MoneySerializer;

import java.math.BigDecimal;

public final class OrderItemView {
    private final String productCode;
    private final String productName;
    private final Integer quantity;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal unitPrice;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal originalAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal discountAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal payableAmount;

    public OrderItemView(String productCode, String productName, Integer quantity,
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
