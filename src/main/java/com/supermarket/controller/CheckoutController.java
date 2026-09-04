package com.supermarket.controller;

import com.supermarket.dto.CheckoutRequest;
import com.supermarket.service.CheckoutService;
import com.supermarket.vo.CheckoutResultView;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/calculate")
    public CheckoutResultView calculate(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.calculate(request);
    }
}
