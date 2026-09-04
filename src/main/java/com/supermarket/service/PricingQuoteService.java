package com.supermarket.service;

import com.supermarket.dto.CheckoutRequest;
import com.supermarket.pricing.PricingQuote;

public interface PricingQuoteService {
    PricingQuote quote(CheckoutRequest request);
}
