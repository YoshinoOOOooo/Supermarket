package com.supermarket.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.supermarket.service.OrderService;
import com.supermarket.vo.OrderView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {
    private final OrderService orderService;

    public OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public IPage<OrderView> list(@RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "20") long size) {
        return orderService.list(page, size);
    }

    @PostMapping("/{orderNo}/complete")
    public OrderView complete(@PathVariable UUID orderNo) {
        return orderService.complete(orderNo);
    }

    @PostMapping("/{orderNo}/cancel")
    public OrderView cancel(@PathVariable UUID orderNo) {
        return orderService.cancel(orderNo);
    }
}
