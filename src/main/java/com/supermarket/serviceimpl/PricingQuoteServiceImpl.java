package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.dto.CheckoutItemRequest;
import com.supermarket.dto.CheckoutRequest;
import com.supermarket.entity.Product;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.ProductMapper;
import com.supermarket.mapper.PromotionMapper;
import com.supermarket.pricing.OrderThresholdReductionRule;
import com.supermarket.pricing.PricingCalculator;
import com.supermarket.pricing.PricingItem;
import com.supermarket.pricing.PricingQuote;
import com.supermarket.pricing.PricingResult;
import com.supermarket.pricing.PricingRule;
import com.supermarket.pricing.ProductDiscountRule;
import com.supermarket.service.PricingQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PricingQuoteServiceImpl implements PricingQuoteService {
    private final ProductMapper productMapper;
    private final PromotionMapper promotionMapper;
    private final PricingCalculator pricingCalculator;

    @Autowired
    public PricingQuoteServiceImpl(ProductMapper productMapper, PromotionMapper promotionMapper) {
        this(productMapper, promotionMapper, new PricingCalculator());
    }

    public PricingQuoteServiceImpl(ProductMapper productMapper, PromotionMapper promotionMapper,
                                   PricingCalculator pricingCalculator) {
        this.productMapper = productMapper;
        this.promotionMapper = promotionMapper;
        this.pricingCalculator = pricingCalculator;
    }

    @Override
    public PricingQuote quote(CheckoutRequest request) {
        LinkedHashMap<String, Integer> quantities = normalizedQuantities(request);
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getCode, quantities.keySet()));
        Map<String, Product> productsByCode = indexProducts(products);
        validateProducts(quantities, productsByCode);

        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionMapper.selectList(activePromotionQuery(now));
        List<PricingItem> items = new ArrayList<PricingItem>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = productsByCode.get(entry.getKey());
            items.add(new PricingItem(entry.getKey(), entry.getValue(), product.getUnitPrice()));
        }

        PricingResult result = pricingCalculator.calculate(items,
                rules(currentPromotions(promotions, now), productsByCode));
        return toQuote(result, productsByCode);
    }

    private LinkedHashMap<String, Integer> normalizedQuantities(CheckoutRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw invalid("Checkout items must not be empty");
        }
        LinkedHashMap<String, Integer> quantities = new LinkedHashMap<String, Integer>();
        for (CheckoutItemRequest item : request.getItems()) {
            if (item == null || item.getProductCode() == null || item.getProductCode().trim().isEmpty()
                    || item.getQuantity() == null || item.getQuantity() < 0) {
                throw invalid("Checkout item is invalid");
            }
            String code = item.getProductCode().trim().toUpperCase(Locale.ROOT);
            if (quantities.put(code, item.getQuantity()) != null) {
                throw invalid("Duplicate product code: " + code);
            }
        }
        return quantities;
    }

    private Map<String, Product> indexProducts(List<Product> products) {
        Map<String, Product> indexed = new HashMap<String, Product>();
        if (products != null) {
            for (Product product : products) {
                if (product != null && product.getCode() != null) {
                    indexed.put(product.getCode().trim().toUpperCase(Locale.ROOT), product);
                }
            }
        }
        return indexed;
    }

    private void validateProducts(Map<String, Integer> quantities, Map<String, Product> products) {
        for (String code : quantities.keySet()) {
            Product product = products.get(code);
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + code);
            }
            if (!Boolean.TRUE.equals(product.getEnabled())) {
                throw new BusinessException(ErrorCode.PRODUCT_DISABLED, "Product is disabled: " + code);
            }
        }
    }

    private LambdaQueryWrapper<Promotion> activePromotionQuery(LocalDateTime now) {
        return new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getEnabled, true)
                .and(query -> query.isNull(Promotion::getStartTime).or().le(Promotion::getStartTime, now))
                .and(query -> query.isNull(Promotion::getEndTime).or().gt(Promotion::getEndTime, now))
                .orderByAsc(Promotion::getPriority);
    }

    private List<Promotion> currentPromotions(List<Promotion> promotions, LocalDateTime now) {
        List<Promotion> current = new ArrayList<Promotion>();
        if (promotions == null) return current;
        for (Promotion promotion : promotions) {
            if (promotion != null && Boolean.TRUE.equals(promotion.getEnabled())
                    && (promotion.getStartTime() == null || !promotion.getStartTime().isAfter(now))
                    && (promotion.getEndTime() == null || promotion.getEndTime().isAfter(now))) {
                current.add(promotion);
            }
        }
        return current;
    }

    private List<PricingRule> rules(List<Promotion> promotions, Map<String, Product> productsByCode) {
        Map<Long, String> codesById = new HashMap<Long, String>();
        for (Map.Entry<String, Product> entry : productsByCode.entrySet()) {
            codesById.put(entry.getValue().getId(), entry.getKey());
        }
        Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();
        boolean thresholdSeen = false;
        List<PricingRule> rules = new ArrayList<PricingRule>();
        for (Promotion promotion : promotions) {
            if (promotion.getType() == PromotionType.PRODUCT_DISCOUNT) {
                String code = codesById.get(promotion.getProductId());
                if (code != null && promotion.getDiscountRate() != null
                        && rates.put(code, promotion.getDiscountRate()) != null) {
                    throw new BusinessException(ErrorCode.PROMOTION_CONFLICT, "Multiple active product discounts");
                }
            } else if (promotion.getType() == PromotionType.ORDER_THRESHOLD_REDUCTION
                    && promotion.getThresholdAmount() != null && promotion.getReductionAmount() != null) {
                if (thresholdSeen) throw new BusinessException(ErrorCode.PROMOTION_CONFLICT,
                        "Multiple active threshold reductions");
                thresholdSeen = true;
                rules.add(new OrderThresholdReductionRule(
                        promotion.getThresholdAmount(), promotion.getReductionAmount()));
            }
        }
        if (!rates.isEmpty()) rules.add(new ProductDiscountRule(rates));
        return rules;
    }

    private PricingQuote toQuote(PricingResult result, Map<String, Product> products) {
        List<PricingQuote.Line> lines = new ArrayList<PricingQuote.Line>();
        for (PricingResult.LineResult line : result.getLineResults()) {
            PricingItem item = line.getItem();
            Product product = products.get(item.getProductCode());
            lines.add(new PricingQuote.Line(product.getId(), item.getProductCode(), product.getName(), item.getQuantity(),
                    item.getUnitPrice(), line.getOriginalAmount(), line.getDiscountAmount(),
                    line.getPayableAmount()));
        }
        return new PricingQuote(lines, result.getOriginalAmount(), result.getDiscountAmount(), result.getPayableAmount());
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
