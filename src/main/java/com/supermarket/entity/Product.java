package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品持久化实体，对应商品基础信息表。 */
@TableName("product")
public class Product {
    /** 商品主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品业务编码，例如 APPLE。 */
    private String code;
    /** 商品展示名称。 */
    private String name;
    /** 商品每斤单价，单位为元。 */
    private BigDecimal unitPrice;
    /** 商品是否启用；停用商品不能参与结算。 */
    private Boolean enabled;
    /** 商品记录创建时间。 */
    private LocalDateTime createdAt;
    /** 商品记录最后更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
