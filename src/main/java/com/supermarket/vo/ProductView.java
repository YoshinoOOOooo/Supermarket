package com.supermarket.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductView {
    private final Long id;
    private final String code;
    private final String name;
    private final BigDecimal unitPrice;
    private final Boolean enabled;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductView(Long id, String code, String name, BigDecimal unitPrice, Boolean enabled,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.unitPrice = unitPrice;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
