package com.supermarket.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 规则计算完成后的不可变金额结果。 */
public final class PricingResult {
    /** 订单优惠前总金额。 */
    private final BigDecimal originalAmount;
    /** 商品级和订单级优惠总金额。 */
    private final BigDecimal discountAmount;
    /** 订单最终应付金额。 */
    private final BigDecimal payableAmount;
    /** 每个商品行的计价明细。 */
    private final List<LineResult> lineResults;

    /** 创建已完成金额舍入的计价结果。 */
    PricingResult(BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal payableAmount,
                  List<LineResult> lineResults) {
        this.originalAmount = money(originalAmount);
        this.discountAmount = money(discountAmount);
        this.payableAmount = money(payableAmount);
        this.lineResults = Collections.unmodifiableList(new ArrayList<LineResult>(lineResults));
    }

    /** 返回订单优惠前总金额。 */
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    /** 返回订单优惠总金额。 */
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    /** 返回订单最终应付金额。 */
    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    /** 返回不可修改的商品行计价结果。 */
    public List<LineResult> getLineResults() {
        return lineResults;
    }

    /** 将金额按 HALF_UP 规则保留两位小数。 */
    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** 单个商品行的不可变计价结果。 */
    public static final class LineResult {
        /** 原始计价商品项。 */
        private final PricingItem item;
        /** 商品行优惠前金额。 */
        private final BigDecimal originalAmount;
        /** 商品行优惠金额。 */
        private final BigDecimal discountAmount;
        /** 商品行优惠后应付金额。 */
        private final BigDecimal payableAmount;

        /** 创建商品行计价结果。 */
        LineResult(PricingItem item, BigDecimal originalAmount, BigDecimal discountAmount,
                   BigDecimal payableAmount) {
            this.item = item;
            this.originalAmount = money(originalAmount);
            this.discountAmount = money(discountAmount);
            this.payableAmount = money(payableAmount);
        }

        /** 返回原始计价商品项。 */
        public PricingItem getItem() {
            return item;
        }

        /** 返回商品行优惠前金额。 */
        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        /** 返回商品行优惠金额。 */
        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        /** 返回商品行优惠后应付金额。 */
        public BigDecimal getPayableAmount() {
            return payableAmount;
        }
    }
}
