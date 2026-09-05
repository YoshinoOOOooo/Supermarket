package com.supermarket.enums;

/** 系统支持的促销规则类型。 */
public enum PromotionType {
    /** 针对指定商品执行折扣。 */
    PRODUCT_DISCOUNT,
    /** 订单达到金额门槛后执行固定金额减免。 */
    ORDER_THRESHOLD_REDUCTION
}
