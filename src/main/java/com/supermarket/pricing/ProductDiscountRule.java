package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProductDiscountRule implements PricingRule {
    private static final int ORDER = 100;
    private final Map<String, BigDecimal> rates;

    public ProductDiscountRule(Map<String, BigDecimal> rates) {
        Objects.requireNonNull(rates, "rates");
        this.rates = Collections.unmodifiableMap(new HashMap<String, BigDecimal>(rates));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void apply(PricingContext context) {
        Objects.requireNonNull(context, "context");
        for (int i = 0; i < context.getItems().size(); i++) {
            PricingItem item = context.getItems().get(i);
            BigDecimal rate = rates.get(item.getProductCode());
            if (rate != null) {
                BigDecimal discount = item.originalAmount().multiply(BigDecimal.ONE.subtract(rate));
                context.addLineDiscount(i, discount);
            }
        }
    }
}
