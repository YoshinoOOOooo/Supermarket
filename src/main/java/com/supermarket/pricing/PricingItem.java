package com.supermarket.pricing;

import java.math.BigDecimal;
import java.util.Objects;

/** 参与计价的单个商品输入，只包含计价所需的最小信息。 */
public final class PricingItem {
    /** 商品业务编码。 */
    private final String productCode;
    /** 购买斤数，必须大于等于零。 */
    private final int quantity;
    /** 每斤单价，单位为元。 */
    private final BigDecimal unitPrice;

    /** 创建不可变计价商品项。 */
    public PricingItem(String productCode, int quantity, BigDecimal unitPrice) {
        this.productCode = Objects.requireNonNull(productCode, "productCode");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    }

    /** 返回商品业务编码。 */
    public String getProductCode() {
        return productCode;
    }

    /** 返回购买斤数。 */
    public int getQuantity() {
        return quantity;
    }

    /** 返回每斤单价。 */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** 计算该商品行未优惠的金额。 */
    BigDecimal originalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
