package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.RoundingMode;

/** 规则执行期间使用的可变计价上下文，集中维护优惠累计与金额舍入。 */
public final class PricingContext {
    /** 本次参与计价的不可变商品列表。 */
    private final List<PricingItem> items;
    /** 与商品列表下标对应的商品级优惠金额。 */
    private final List<BigDecimal> lineDiscounts;
    /** 整张订单累计的订单级优惠金额。 */
    private BigDecimal orderDiscount = BigDecimal.ZERO;

    /** 根据商品项初始化计价上下文和零值行优惠。 */
    PricingContext(List<PricingItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<PricingItem>(items));
        this.lineDiscounts = new ArrayList<BigDecimal>(items.size());
        for (int i = 0; i < items.size(); i++) {
            lineDiscounts.add(BigDecimal.ZERO);
        }
    }

    /** 返回只读商品项列表。 */
    public List<PricingItem> getItems() {
        return items;
    }

    /** 返回指定商品行已经累计的优惠金额。 */
    public BigDecimal getLineDiscount(int index) {
        return lineDiscounts.get(index);
    }

    /** 为指定商品行增加优惠，并按两位小数统一舍入。 */
    public void addLineDiscount(int index, BigDecimal discount) {
        BigDecimal original = money(items.get(index).originalAmount());
        BigDecimal payable = money(original.subtract(lineDiscounts.get(index)).subtract(discount));
        lineDiscounts.set(index, original.subtract(payable));
    }

    /** 汇总所有商品行的优惠前金额。 */
    public BigDecimal getOriginalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (PricingItem item : items) {
            total = total.add(money(item.originalAmount()));
        }
        return total;
    }

    /** 汇总商品级优惠与订单级优惠。 */
    public BigDecimal getDiscountAmount() {
        BigDecimal total = orderDiscount;
        for (BigDecimal discount : lineDiscounts) {
            total = total.add(discount);
        }
        return total;
    }

    /** 计算扣除全部优惠后的订单应付金额。 */
    public BigDecimal getPayableAmount() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            subtotal = subtotal.add(money(items.get(i).originalAmount()).subtract(lineDiscounts.get(i)));
        }
        return subtotal.subtract(orderDiscount);
    }

    /** 累加整张订单级别的优惠。 */
    public void addOrderDiscount(BigDecimal discount) {
        orderDiscount = orderDiscount.add(discount);
    }

    /** 将可变上下文转换为不可变计价结果。 */
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

    /** 将金额按 HALF_UP 规则保留两位小数。 */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
