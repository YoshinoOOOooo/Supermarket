package com.supermarket.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.supermarket.dto.CheckoutRequest;
import com.supermarket.vo.OrderView;

import java.util.UUID;

public interface OrderService {
    OrderView create(CheckoutRequest request);
    OrderView update(UUID orderNo, CheckoutRequest request);
    OrderView findByOrderNo(UUID orderNo);
    OrderView complete(UUID orderNo);
    OrderView cancel(UUID orderNo);
    IPage<OrderView> list(long page, long size);
}
