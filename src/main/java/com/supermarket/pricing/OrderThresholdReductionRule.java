package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Objects;

/** 订单满减规则，在折后小计达到门槛时减免固定金额。 */
public final class OrderThresholdReductionRule implements PricingRule {
    /** 满减在商品折扣之后执行。 */
    private static final int ORDER = 200;
    /** 触发满减的折后金额门槛。 */
    private final BigDecimal threshold;
    /** 达到门槛后减免的固定金额。 */
    private final BigDecimal reduction;

    /** 创建订单满减规则。 */
    public OrderThresholdReductionRule(BigDecimal threshold, BigDecimal reduction) {
        this.threshold = Objects.requireNonNull(threshold, "threshold");
        this.reduction = Objects.requireNonNull(reduction, "reduction");
    }

    /** 返回订单满减的执行顺序。 */
    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 根据当前折后应付金额判断并应用订单级减免。 */
    @Override
    public void apply(PricingContext context) {
        Objects.requireNonNull(context, "context");
        if (context.getPayableAmount().compareTo(threshold) >= 0) {
            context.addOrderDiscount(reduction);
        }
    }
}
