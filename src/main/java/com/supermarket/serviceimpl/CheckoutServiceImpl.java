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

/** 购物车试算服务实现，将内部报价转换为公开结算结果。 */
@Service
public class CheckoutServiceImpl implements CheckoutService {
    /** 统一报价服务，负责加载商品、促销并执行计价。 */
    @Resource
    private PricingQuoteService pricingQuoteService;

    /** 计算购物车金额但不创建订单。 */
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
