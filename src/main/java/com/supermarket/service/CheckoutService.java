package com.supermarket.service;

import com.supermarket.dto.CheckoutRequest;
import com.supermarket.vo.CheckoutResultView;

public interface CheckoutService {
    CheckoutResultView calculate(CheckoutRequest request);
}
