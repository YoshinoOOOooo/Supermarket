package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.enums.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 促销规则持久化实体，可表示商品折扣或订单满减。 */
@TableName("promotion")
public class Promotion {
    /** 促销规则主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 促销规则的唯一业务编码。 */
    private String code;
    /** 促销规则展示名称。 */
    private String name;
    /** 促销类型。 */
    private PromotionType type;
    /** 适用商品主键；订单级满减规则不使用该字段。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long productId;
    /** 商品折扣率，例如 0.80 表示八折。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal discountRate;
    /** 触发订单满减的金额门槛，单位为元。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal thresholdAmount;
    /** 达到门槛后减免的金额，单位为元。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal reductionAmount;
    /** 规则执行优先级，数值越小越先执行。 */
    private Integer priority;
    /** 促销规则是否启用。 */
    private Boolean enabled;
    /** 促销生效时间；为空表示不限制开始时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime startTime;
    /** 促销失效时间；为空表示不限制结束时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime endTime;
    /** 促销记录创建时间。 */
    private LocalDateTime createdAt;
    /** 促销记录最后更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PromotionType getType() { return type; }
    public void setType(PromotionType type) { this.type = type; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(BigDecimal reductionAmount) { this.reductionAmount = reductionAmount; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
