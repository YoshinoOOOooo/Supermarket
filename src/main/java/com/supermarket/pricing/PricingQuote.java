package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 应用层计价报价，包含商品持久化快照信息和汇总金额。 */
public final class PricingQuote {
    /** 商品报价明细。 */
    private final List<Line> lines;
    /** 优惠前总金额。 */
    private final BigDecimal originalAmount;
    /** 优惠总金额。 */
    private final BigDecimal discountAmount;
    /** 最终应付金额。 */
    private final BigDecimal payableAmount;

    /** 创建不可变报价结果。 */
    public PricingQuote(List<Line> lines, BigDecimal originalAmount,
                        BigDecimal discountAmount, BigDecimal payableAmount) {
        this.lines = Collections.unmodifiableList(new ArrayList<Line>(lines));
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }

    /** 返回不可修改的报价明细。 */
    public List<Line> getLines() { return lines; }
    /** 返回优惠前总金额。 */
    public BigDecimal getOriginalAmount() { return originalAmount; }
    /** 返回优惠总金额。 */
    public BigDecimal getDiscountAmount() { return discountAmount; }
    /** 返回最终应付金额。 */
    public BigDecimal getPayableAmount() { return payableAmount; }

    /** 单个商品的报价与快照信息。 */
    public static final class Line {
        /** 商品数据库主键。 */
        private final Long productId;
        /** 商品业务编码。 */
        private final String productCode;
        /** 商品名称快照。 */
        private final String productName;
        /** 购买斤数。 */
        private final int quantity;
        /** 每斤单价快照。 */
        private final BigDecimal unitPrice;
        /** 商品行优惠前金额。 */
        private final BigDecimal originalAmount;
        /** 商品行优惠金额。 */
        private final BigDecimal discountAmount;
        /** 商品行优惠后应付金额。 */
        private final BigDecimal payableAmount;

        /** 创建不带数据库商品主键的报价明细。 */
        public Line(String productCode, String productName, int quantity, BigDecimal unitPrice,
                    BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount) {
            this(null, productCode, productName, quantity, unitPrice, originalAmount, discountAmount, payableAmount);
        }

        /** 创建包含完整商品快照的报价明细。 */
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

        /** 返回商品主键。 */
        public Long getProductId() { return productId; }
        /** 返回商品业务编码。 */
        public String getProductCode() { return productCode; }
        /** 返回商品名称快照。 */
        public String getProductName() { return productName; }
        /** 返回购买斤数。 */
        public int getQuantity() { return quantity; }
        /** 返回每斤单价快照。 */
        public BigDecimal getUnitPrice() { return unitPrice; }
        /** 返回商品行优惠前金额。 */
        public BigDecimal getOriginalAmount() { return originalAmount; }
        /** 返回商品行优惠金额。 */
        public BigDecimal getDiscountAmount() { return discountAmount; }
        /** 返回商品行优惠后应付金额。 */
        public BigDecimal getPayableAmount() { return payableAmount; }
    }
}
