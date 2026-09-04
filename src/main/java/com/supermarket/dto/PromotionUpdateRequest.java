package com.supermarket.dto;

import com.supermarket.enums.PromotionType;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromotionUpdateRequest {
    @NotBlank private String name;
    @NotNull private PromotionType type;
    private Long productId;
    @DecimalMin(value = "0.00", inclusive = false) private BigDecimal discountRate;
    @DecimalMin(value = "0.00", inclusive = false) private BigDecimal thresholdAmount;
    @DecimalMin(value = "0.00", inclusive = false) private BigDecimal reductionAmount;
    @NotNull private Integer priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public PromotionUpdateRequest() { }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PromotionType getType() { return type; }
    public void setType(PromotionType type) { this.type = type; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(BigDecimal reductionAmount) { this.reductionAmount = reductionAmount; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
