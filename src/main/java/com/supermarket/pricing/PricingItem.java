package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Objects;

public final class PricingItem {
    private final String productCode;
    private final int quantity;
    private final BigDecimal unitPrice;

    public PricingItem(String productCode, int quantity, BigDecimal unitPrice) {
        this.productCode = Objects.requireNonNull(productCode, "productCode");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    BigDecimal originalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
