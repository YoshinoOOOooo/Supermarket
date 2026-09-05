package com.supermarket.enums;

/** 订单生命周期状态。 */
public enum OrderStatus {
    /** 未支付，可修改、完成或取消。 */
    UNPAID,
    /** 已完成，属于终态。 */
    COMPLETED,
    /** 已取消，属于终态。 */
    CANCELLED
}
