package com.supermarket.pricing;

public interface PricingRule {
    int getOrder();

    void apply(PricingContext context);
}
