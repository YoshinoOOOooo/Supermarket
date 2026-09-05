package com.supermarket.exception;

/** API 对外返回的标准错误码。 */
public enum ErrorCode {
    /** 请求参数、格式或内容不合法。 */
    INVALID_REQUEST,
    /** 指定商品不存在。 */
    PRODUCT_NOT_FOUND,
    /** 指定商品已停用，不能参与当前操作。 */
    PRODUCT_DISABLED,
    /** 促销规则发生时间、唯一性或并发冲突。 */
    PROMOTION_CONFLICT,
    /** 通用业务资源发生唯一性或并发冲突。 */
    RESOURCE_CONFLICT,
    /** 指定订单不存在。 */
    ORDER_NOT_FOUND,
    /** 当前订单状态不允许执行目标操作。 */
    INVALID_ORDER_STATE,
    /** 未预期的服务端内部错误。 */
    INTERNAL_ERROR
}
