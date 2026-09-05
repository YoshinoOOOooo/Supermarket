package com.supermarket.serviceimpl;

import com.supermarket.dto.CheckoutRequest;
import com.supermarket.pricing.PricingQuote;
import com.supermarket.service.CheckoutService;
import com.supermarket.service.PricingQuoteService;
import com.supermarket.vo.CheckoutItemView;
import com.supermarket.vo.CheckoutResultView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    @Resource
    private PricingQuoteService pricingQuoteService;

    @Override
    public CheckoutResultView calculate(CheckoutRequest request) {
        PricingQuote quote = pricingQuoteService.quote(request);
        List<CheckoutItemView> items = new ArrayList<CheckoutItemView>();
        for (PricingQuote.Line line : quote.getLines()) {
            items.add(new CheckoutItemView(line.getProductCode(), line.getProductName(), line.getQuantity(),
                    line.getUnitPrice(), line.getOriginalAmount(), line.getDiscountAmount(), line.getPayableAmount()));
        }
        return new CheckoutResultView(items, quote.getOriginalAmount(),
                quote.getDiscountAmount(), quote.getPayableAmount());
    }
}
