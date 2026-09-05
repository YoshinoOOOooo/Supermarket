package com.supermarket.pricing;

/** 计价规则统一接口，规则按照执行顺序依次修改计价上下文。 */
public interface PricingRule {
    /** 返回规则执行顺序，数值越小越先执行。 */
    int getOrder();

    /** 将当前规则产生的优惠应用到计价上下文。 */
    void apply(PricingContext context);
}
