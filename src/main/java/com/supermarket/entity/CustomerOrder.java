package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 顾客订单主表实体，保存订单状态及金额汇总快照。 */
@TableName("customer_order")
public class CustomerOrder {
    /** 订单数据库主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对外使用的唯一订单编号。 */
    private String orderNo;
    /** 订单当前状态。 */
    private OrderStatus status;
    /** 优惠前商品总金额，单位为元。 */
    private BigDecimal originalAmount;
    /** 订单全部优惠金额，单位为元。 */
    private BigDecimal discountAmount;
    /** 顾客最终应付金额，单位为元。 */
    private BigDecimal payableAmount;
    /** 订单创建时间。 */
    private LocalDateTime createdAt;
    /** 订单最后更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
