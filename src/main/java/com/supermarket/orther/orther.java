package com.supermarket.orther;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class orther {

    /**
     * 计算总价，返回金额单位为元。
     * 折扣使用 0.8 表示八折，未配置折扣的水果按原价计算。
     * 先计算水果折扣，再判断满减；满减只执行一次，门槛为 0 时禁用。
     * 使用精确十进制运算，不在计算过程中舍入。
     */
    public static BigDecimal calculateTotal(
            Map<String, BigDecimal> prices,
            Map<String, Integer> quantities,
            Map<String, BigDecimal> discounts,
            BigDecimal threshold,
            BigDecimal reduction) {
        if (prices == null || quantities == null || discounts == null
                || threshold == null || reduction == null) {
            throw new IllegalArgumentException("价格、斤数、折扣和满减参数不能为 null");
        }
        if (threshold.signum() < 0 || reduction.signum() < 0) {
            throw new IllegalArgumentException("满减门槛和金额不能为负数");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String fruit = entry.getKey();
            Integer quantity = entry.getValue();
            BigDecimal price = prices.get(fruit);
            BigDecimal discount = discounts.getOrDefault(fruit, BigDecimal.ONE);

            if (fruit == null || fruit.trim().isEmpty()) {
                throw new IllegalArgumentException("水果名称不能为空");
            }
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("购买斤数必须为非负整数：" + fruit);
            }
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException("水果价格缺失或为负数：" + fruit);
            }
            if (discount == null || discount.signum() < 0
                    || discount.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("折扣必须在 0 到 1 之间：" + fruit);
            }

            total = total.add(price.multiply(BigDecimal.valueOf(quantity))
                    .multiply(discount));
        }

        if (threshold.signum() > 0 && total.compareTo(threshold) >= 0) {
            total = total.subtract(reduction);
        }
        return total.max(BigDecimal.ZERO);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Map<String, BigDecimal> prices = new HashMap<>();
        Map<String, Integer> quantities = new HashMap<>();
        Map<String, BigDecimal> discounts = new HashMap<>();

        System.out.print("请输入水果种类数量：");
        int count = scanner.nextInt();

        for (int i = 0; i < count; i++) {
            System.out.println("请输入第 " + (i + 1)
                    + " 种水果：名称 单价 斤数 折扣（1 表示原价，0.8 表示八折）");

            String fruit = scanner.next();
            BigDecimal price = new BigDecimal(scanner.next());
            int quantity = scanner.nextInt();
            BigDecimal discount = new BigDecimal(scanner.next());

            prices.put(fruit, price);
            quantities.put(fruit, quantity);
            discounts.put(fruit, discount);
        }

        System.out.print("请输入满减门槛（0 表示不启用）：");
        BigDecimal threshold = new BigDecimal(scanner.next());

        System.out.print("请输入满减金额（不启用时输入 0）：");
        BigDecimal reduction = new BigDecimal(scanner.next());

        BigDecimal total = calculateTotal(
                prices, quantities, discounts, threshold, reduction);

        if(total.equals(BigDecimal.ZERO))throw new IllegalArgumentException("请输入有效物品数量");

        System.out.println("应付总价：" + total.toPlainString() + " 元");
    }
}
