package com.supermarket.pricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 计价计算器，负责排序并依次执行所有促销规则。 */
public final class PricingCalculator {

    /**
     * 根据商品项和促销规则计算金额结果。
     * 商品级优惠先于订单级优惠执行，并拒绝产生负数应付金额。
     */
    public PricingResult calculate(List<PricingItem> items, List<PricingRule> rules) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(rules, "rules");
        PricingContext context = new PricingContext(items);
        List<PricingRule> orderedRules = new ArrayList<PricingRule>(rules);
        Collections.sort(orderedRules, Comparator.comparingInt(PricingRule::getOrder));
        for (PricingRule rule : orderedRules) {
            rule.apply(context);
        }
        if (context.getPayableAmount().signum() < 0) {
            throw new IllegalStateException("payable amount must not be negative");
        }
        return context.toResult();
    }
}
