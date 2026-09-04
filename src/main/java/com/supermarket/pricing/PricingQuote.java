package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PricingQuote {
    private final List<Line> lines;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal payableAmount;

    public PricingQuote(List<Line> lines, BigDecimal originalAmount,
                        BigDecimal discountAmount, BigDecimal payableAmount) {
        this.lines = Collections.unmodifiableList(new ArrayList<Line>(lines));
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }

    public List<Line> getLines() { return lines; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }

    public static final class Line {
        private final Long productId;
        private final String productCode;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal originalAmount;
        private final BigDecimal discountAmount;
        private final BigDecimal payableAmount;

        public Line(String productCode, String productName, int quantity, BigDecimal unitPrice,
                    BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount) {
            this(null, productCode, productName, quantity, unitPrice, originalAmount, discountAmount, payableAmount);
        }

        public Line(Long productId, String productCode, String productName, int quantity, BigDecimal unitPrice,
                    BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount) {
            this.productId = productId;
            this.productCode = productCode;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.originalAmount = originalAmount;
            this.discountAmount = discountAmount;
            this.payableAmount = payableAmount;
        }

        public Long getProductId() { return productId; }
        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getPayableAmount() { return payableAmount; }
    }
}
