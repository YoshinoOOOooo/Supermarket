package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.supermarket.dto.CheckoutItemRequest;
import com.supermarket.dto.CheckoutRequest;
import com.supermarket.entity.Product;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.ProductMapper;
import com.supermarket.mapper.PromotionMapper;
import com.supermarket.pricing.PricingCalculator;
import com.supermarket.vo.CheckoutResultView;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final PromotionMapper promotionMapper = mock(PromotionMapper.class);
    private final PricingQuoteServiceImpl quoteService =
            new PricingQuoteServiceImpl(productMapper, promotionMapper, new PricingCalculator());
    private final CheckoutServiceImpl service = new CheckoutServiceImpl(quoteService);

    @BeforeAll
    static void initializeLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Promotion.class);
    }

    @Test
    void batchLoadsNormalizedProductCodesOnceWithoutWriting() {
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(1L, "APPLE", "Apple", "8.00", true),
                product(2L, "STRAWBERRY", "Strawberry", "13.00", true)));
        when(promotionMapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());

        CheckoutResultView result = service.calculate(request(item(" apple ", 2), item("Strawberry", 3)));

        assertMoney("55.00", result.getPayableAmount());
        assertEquals("APPLE", result.getItems().get(0).getProductCode());
        ArgumentCaptor<LambdaQueryWrapper<Product>> query = productQuery();
        org.junit.jupiter.api.Assertions.assertTrue(query.getValue().getSqlSegment().contains("IN"));
        org.junit.jupiter.api.Assertions.assertTrue(query.getValue().getParamNameValuePairs().containsValue("APPLE"));
        org.junit.jupiter.api.Assertions.assertTrue(query.getValue().getParamNameValuePairs().containsValue("STRAWBERRY"));
        verify(productMapper, never()).insert(any());
        verify(productMapper, never()).updateById(any());
        verify(promotionMapper, never()).insert(any());
        verify(promotionMapper, never()).updateById(any());
    }

    @Test
    void rejectsDuplicateCodesAfterNormalizationBeforeQuerying() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.calculate(request(item(" apple ", 1), item("APPLE", 2))));

        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
        verify(productMapper, never()).selectList(any());
    }

    @Test
    void reportsMissingProduct() {
        when(productMapper.selectList(any())).thenReturn(Collections.<Product>emptyList());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.calculate(request(item("MISSING", 1))));

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void reportsDisabledProduct() {
        when(productMapper.selectList(any())).thenReturn(Collections.singletonList(
                product(1L, "APPLE", "Apple", "8.00", false)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.calculate(request(item("APPLE", 1))));

        assertEquals(ErrorCode.PRODUCT_DISABLED, error.getErrorCode());
        verify(promotionMapper, never()).selectList(any());
    }

    @Test
    void returnsTwoDecimalZeroForZeroQuantities() {
        stubProducts(product(1L, "APPLE", "Apple", "8.00", true));

        CheckoutResultView result = service.calculate(request(item("APPLE", 0)));

        assertMoney("0.00", result.getOriginalAmount());
        assertMoney("0.00", result.getDiscountAmount());
        assertMoney("0.00", result.getPayableAmount());
    }

    @Test
    void pricesScenarioAWithoutPromotions() {
        stubProducts(apple(), strawberry());
        assertMoney("55.00", service.calculate(request(item("APPLE", 2), item("STRAWBERRY", 3))).getPayableAmount());
    }

    @Test
    void pricesScenarioBWithMango() {
        stubProducts(apple(), strawberry(), mango());
        assertMoney("75.00", service.calculate(request(item("APPLE", 2), item("STRAWBERRY", 3), item("MANGO", 1))).getPayableAmount());
    }

    @Test
    void pricesScenarioCWithCurrentProductDiscountOnly() {
        stubProducts(apple(), strawberry(), mango());
        LocalDateTime now = LocalDateTime.now();
        when(promotionMapper.selectList(any())).thenReturn(Arrays.asList(
                discount(2L, "0.80", true, now.minusDays(1), now.plusDays(1)),
                discount(2L, "0.10", true, now.minusDays(2), now.minusDays(1)),
                discount(2L, "0.10", false, now.minusDays(1), now.plusDays(1))));

        CheckoutResultView result = service.calculate(request(item("APPLE", 2), item("STRAWBERRY", 3), item("MANGO", 1)));

        assertMoney("67.20", result.getPayableAmount());
        ArgumentCaptor<LambdaQueryWrapper<Promotion>> query = promotionQuery();
        String sql = query.getValue().getSqlSegment();
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("enabled"));
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("start_time"));
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("end_time"));
    }

    @Test
    void pricesScenarioDWithCurrentDiscountAndThresholdReduction() {
        stubProducts(apple(), strawberry(), mango());
        LocalDateTime now = LocalDateTime.now();
        when(promotionMapper.selectList(any())).thenReturn(Arrays.asList(
                discount(2L, "0.80", true, null, null),
                threshold("100.00", "10.00", true, now.minusHours(1), now.plusHours(1))));

        CheckoutResultView result = service.calculate(request(item("APPLE", 5), item("STRAWBERRY", 5), item("MANGO", 1)));

        assertMoney("102.00", result.getPayableAmount());
    }

    @Test void rejectsDuplicateEffectivePromotionRules() {
        stubProducts(strawberry());
        when(promotionMapper.selectList(any())).thenReturn(Arrays.asList(
                discount(2L, "0.80", true, null, null), discount(2L, "0.70", true, null, null)));
        BusinessException productConflict = assertThrows(BusinessException.class,
                () -> service.calculate(request(item("STRAWBERRY", 1))));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, productConflict.getErrorCode());

        when(promotionMapper.selectList(any())).thenReturn(Arrays.asList(
                threshold("100.00", "10.00", true, null, null),
                threshold("200.00", "20.00", true, null, null)));
        BusinessException thresholdConflict = assertThrows(BusinessException.class,
                () -> service.calculate(request(item("STRAWBERRY", 1))));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, thresholdConflict.getErrorCode());
    }

    private void stubProducts(Product... products) {
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(products));
        when(promotionMapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaQueryWrapper<Product>> productQuery() {
        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        return captor;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaQueryWrapper<Promotion>> promotionQuery() {
        ArgumentCaptor<LambdaQueryWrapper<Promotion>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(promotionMapper).selectList(captor.capture());
        return captor;
    }

    private Product apple() { return product(1L, "APPLE", "Apple", "8.00", true); }
    private Product strawberry() { return product(2L, "STRAWBERRY", "Strawberry", "13.00", true); }
    private Product mango() { return product(3L, "MANGO", "Mango", "20.00", true); }

    private Product product(Long id, String code, String name, String price, boolean enabled) {
        Product product = new Product();
        product.setId(id); product.setCode(code); product.setName(name);
        product.setUnitPrice(new BigDecimal(price)); product.setEnabled(enabled);
        return product;
    }

    private Promotion discount(Long productId, String rate, boolean enabled,
                               LocalDateTime start, LocalDateTime end) {
        Promotion promotion = promotion(PromotionType.PRODUCT_DISCOUNT, enabled, start, end);
        promotion.setProductId(productId); promotion.setDiscountRate(new BigDecimal(rate));
        return promotion;
    }

    private Promotion threshold(String amount, String reduction, boolean enabled,
                                LocalDateTime start, LocalDateTime end) {
        Promotion promotion = promotion(PromotionType.ORDER_THRESHOLD_REDUCTION, enabled, start, end);
        promotion.setThresholdAmount(new BigDecimal(amount));
        promotion.setReductionAmount(new BigDecimal(reduction));
        return promotion;
    }

    private Promotion promotion(PromotionType type, boolean enabled,
                                LocalDateTime start, LocalDateTime end) {
        Promotion promotion = new Promotion();
        promotion.setType(type); promotion.setEnabled(enabled);
        promotion.setStartTime(start); promotion.setEndTime(end);
        return promotion;
    }

    private CheckoutRequest request(CheckoutItemRequest... items) {
        return new CheckoutRequest(Arrays.asList(items));
    }

    private CheckoutItemRequest item(String code, int quantity) {
        return new CheckoutItemRequest(code, quantity);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }
}
