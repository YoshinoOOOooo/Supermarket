package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.RoundingMode;

public final class PricingContext {
    private final List<PricingItem> items;
    private final List<BigDecimal> lineDiscounts;
    private BigDecimal orderDiscount = BigDecimal.ZERO;

    PricingContext(List<PricingItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<PricingItem>(items));
        this.lineDiscounts = new ArrayList<BigDecimal>(items.size());
        for (int i = 0; i < items.size(); i++) {
            lineDiscounts.add(BigDecimal.ZERO);
        }
    }

    public List<PricingItem> getItems() {
        return items;
    }

    public BigDecimal getLineDiscount(int index) {
        return lineDiscounts.get(index);
    }

    public void addLineDiscount(int index, BigDecimal discount) {
        BigDecimal original = money(items.get(index).originalAmount());
        BigDecimal payable = money(original.subtract(lineDiscounts.get(index)).subtract(discount));
        lineDiscounts.set(index, original.subtract(payable));
    }

    public BigDecimal getOriginalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (PricingItem item : items) {
            total = total.add(money(item.originalAmount()));
        }
        return total;
    }

    public BigDecimal getDiscountAmount() {
        BigDecimal total = orderDiscount;
        for (BigDecimal discount : lineDiscounts) {
            total = total.add(discount);
        }
        return total;
    }

    public BigDecimal getPayableAmount() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            subtotal = subtotal.add(money(items.get(i).originalAmount()).subtract(lineDiscounts.get(i)));
        }
        return subtotal.subtract(orderDiscount);
    }

    public void addOrderDiscount(BigDecimal discount) {
        orderDiscount = orderDiscount.add(discount);
    }

    PricingResult toResult() {
        List<PricingResult.LineResult> lines = new ArrayList<PricingResult.LineResult>(items.size());
        for (int i = 0; i < items.size(); i++) {
            BigDecimal original = money(items.get(i).originalAmount());
            BigDecimal discount = lineDiscounts.get(i);
            lines.add(new PricingResult.LineResult(items.get(i), original, discount,
                    original.subtract(discount)));
        }
        return new PricingResult(getOriginalAmount(), getDiscountAmount(), getPayableAmount(), lines);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
