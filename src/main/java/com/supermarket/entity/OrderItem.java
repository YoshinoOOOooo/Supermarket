package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 订单明细实体，保存下单或修改订单时的商品与价格快照。 */
@TableName("order_item")
public class OrderItem {
    /** 订单明细主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属订单的数据库主键。 */
    private Long orderId;
    /** 商品数据库主键。 */
    private Long productId;
    /** 下单时的商品业务编码快照。 */
    private String productCode;
    /** 下单时的商品名称快照。 */
    private String productName;
    /** 购买斤数。 */
    private Integer quantity;
    /** 下单时的每斤单价快照，单位为元。 */
    private BigDecimal unitPrice;
    /** 本明细优惠前金额，单位为元。 */
    private BigDecimal originalAmount;
    /** 本明细商品级优惠金额，单位为元。 */
    private BigDecimal discountAmount;
    /** 本明细商品级优惠后的应付金额，单位为元。 */
    private BigDecimal payableAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
}
