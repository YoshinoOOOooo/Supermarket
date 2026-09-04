package com.supermarket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supermarket.config.MoneySerializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CheckoutResultView {
    private final List<CheckoutItemView> items;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal originalAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal discountAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal payableAmount;

    public CheckoutResultView(List<CheckoutItemView> items, BigDecimal originalAmount,
                              BigDecimal discountAmount, BigDecimal payableAmount) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }
    public List<CheckoutItemView> getItems() { return items; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
}
