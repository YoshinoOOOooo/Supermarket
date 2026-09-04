package com.supermarket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supermarket.config.MoneySerializer;
import com.supermarket.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OrderView {
    private final String orderNo;
    private final OrderStatus status;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal originalAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal discountAmount;
    @JsonSerialize(using = MoneySerializer.class)
    private final BigDecimal payableAmount;
    private final List<OrderItemView> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderView(String orderNo, OrderStatus status, BigDecimal originalAmount,
                     BigDecimal discountAmount, BigDecimal payableAmount,
                     List<OrderItemView> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public String getOrderNo() { return orderNo; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public List<OrderItemView> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
