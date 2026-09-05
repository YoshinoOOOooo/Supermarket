package com.supermarket.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;

public class CheckoutItemRequest {
    @NotBlank
    private String productCode;
    @NotNull
    @Min(0)
    @Max(100000)
    private Integer quantity;

    public CheckoutItemRequest() { }
    public CheckoutItemRequest(String productCode, Integer quantity) {
        this.productCode = productCode;
        this.quantity = quantity;
    }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
