package com.supermarket.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PricingResult {
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal payableAmount;
    private final List<LineResult> lineResults;

    PricingResult(BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount,
                  List<LineResult> lineResults) {
        this.originalAmount = money(originalAmount);
        this.discountAmount = money(discountAmount);
        this.payableAmount = money(payableAmount);
        this.lineResults = Collections.unmodifiableList(new ArrayList<LineResult>(lineResults));
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public List<LineResult> getLineResults() {
        return lineResults;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static final class LineResult {
        private final PricingItem item;
        private final BigDecimal originalAmount;
        private final BigDecimal discountAmount;
        private final BigDecimal payableAmount;

        LineResult(PricingItem item, BigDecimal originalAmount, BigDecimal discountAmount,
                   BigDecimal payableAmount) {
            this.item = item;
            this.originalAmount = money(originalAmount);
            this.discountAmount = money(discountAmount);
            this.payableAmount = money(payableAmount);
        }

        public PricingItem getItem() {
            return item;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getPayableAmount() {
            return payableAmount;
        }
    }
}
