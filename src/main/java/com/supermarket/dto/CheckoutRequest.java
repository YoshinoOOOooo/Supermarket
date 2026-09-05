package com.supermarket.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

public class CheckoutRequest {
    @NotEmpty
    @Size(max = 100)
    private List<@Valid CheckoutItemRequest> items;

    public CheckoutRequest() { }
    public CheckoutRequest(List<CheckoutItemRequest> items) { this.items = items; }
    public List<CheckoutItemRequest> getItems() { return items; }
    public void setItems(List<CheckoutItemRequest> items) { this.items = items; }
}
