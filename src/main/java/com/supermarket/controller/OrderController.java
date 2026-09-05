package com.supermarket.controller;

import com.supermarket.dto.CheckoutRequest;
import com.supermarket.service.OrderService;
import com.supermarket.vo.OrderView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.annotation.Resource;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Resource
    private OrderService orderService;

    @PostMapping
    public OrderView create(@Valid @RequestBody CheckoutRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{orderNo}")
    public OrderView find(@PathVariable UUID orderNo) {
        return orderService.findByOrderNo(orderNo);
    }

    @PutMapping("/{orderNo}")
    public OrderView update(@PathVariable UUID orderNo, @Valid @RequestBody CheckoutRequest request) {
        return orderService.update(orderNo, request);
    }
}
