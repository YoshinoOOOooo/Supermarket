package com.supermarket.pricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PricingCalculator {

    public PricingResult calculate(List<PricingItem> items, List<PricingRule> rules) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(rules, "rules");
        PricingContext context = new PricingContext(items);
        List<PricingRule> orderedRules = new ArrayList<PricingRule>(rules);
        Collections.sort(orderedRules, new Comparator<PricingRule>() {
            @Override
            public int compare(PricingRule left, PricingRule right) {
                return Integer.compare(left.getOrder(), right.getOrder());
            }
        });
        for (PricingRule rule : orderedRules) {
            rule.apply(context);
        }
        if (context.getPayableAmount().signum() < 0) {
            throw new IllegalStateException("payable amount must not be negative");
        }
        return context.toResult();
    }
}
