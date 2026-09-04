package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Objects;

public final class OrderThresholdReductionRule implements PricingRule {
    private static final int ORDER = 200;
    private final BigDecimal threshold;
    private final BigDecimal reduction;

    public OrderThresholdReductionRule(BigDecimal threshold, BigDecimal reduction) {
        this.threshold = Objects.requireNonNull(threshold, "threshold");
        this.reduction = Objects.requireNonNull(reduction, "reduction");
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void apply(PricingContext context) {
        Objects.requireNonNull(context, "context");
        if (context.getPayableAmount().compareTo(threshold) >= 0) {
            context.addOrderDiscount(reduction);
        }
    }
}
