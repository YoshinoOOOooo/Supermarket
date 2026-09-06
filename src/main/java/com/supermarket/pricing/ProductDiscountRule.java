package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** 商品折扣规则，按照商品编码对指定商品应用折扣率。 */
public final class ProductDiscountRule implements PricingRule {
    /** 商品折扣固定在订单级满减之前执行。 */
    private static final int ORDER = 100;
    /** 商品编码与折扣率的只读映射，例如 0.80 表示八折。 */
    private final Map<String, BigDecimal> rates;

    /** 创建商品折扣规则，并复制传入映射避免外部修改。 */
    public ProductDiscountRule(Map<String, BigDecimal> rates) {
        Objects.requireNonNull(rates, "rates");
        this.rates = Collections.unmodifiableMap(new HashMap<>(rates));
    }

    /** 返回商品折扣的执行顺序。 */
    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 遍历商品项并累计每一行的商品级优惠。 */
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
