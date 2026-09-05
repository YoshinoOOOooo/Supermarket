package com.supermarket.vo;

import com.supermarket.enums.PromotionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PromotionView {
    private final Long id;
    private final String code;
    private final String name;
    private final PromotionType type;
    private final Long productId;
    private final BigDecimal discountRate;
    private final BigDecimal thresholdAmount;
    private final BigDecimal reductionAmount;
    private final Integer priority;
    private final Boolean enabled;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PromotionView(Long id, String code, String name, PromotionType type, Long productId,
                         BigDecimal discountRate, BigDecimal thresholdAmount,
                         BigDecimal reductionAmount, Integer priority, Boolean enabled,
                         LocalDateTime startTime, LocalDateTime endTime,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.productId = productId;
        this.discountRate = discountRate;
        this.thresholdAmount = thresholdAmount;
        this.reductionAmount = reductionAmount;
        this.priority = priority;
        this.enabled = enabled;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public PromotionType getType() { return type; }
    public Long getProductId() { return productId; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public Integer getPriority() { return priority; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
