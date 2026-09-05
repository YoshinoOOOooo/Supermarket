package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.PromotionMapper;
import com.supermarket.mapper.ProductMapper;
import com.supermarket.mapper.PromotionMutexMapper;
import com.supermarket.entity.Product;
import com.supermarket.vo.PromotionView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {
    private final PromotionMapper mapper = mock(PromotionMapper.class);
    private final ProductMapper products = mock(ProductMapper.class);
    private final PromotionMutexMapper mutex = mock(PromotionMutexMapper.class);
    private final PromotionServiceImpl service = promotionService(mapper, products, mutex);
    @Captor private ArgumentCaptor<LambdaQueryWrapper<Promotion>> promotionWrapperCaptor;
    private final AtomicReference<Promotion> insertedPromotion = new AtomicReference<Promotion>();

    @BeforeAll static void initializeLambdaMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Promotion.class);
    }

    @BeforeEach void allowExistingProduct() {
        lenient().when(products.selectByIdForUpdate(anyLong())).thenReturn(new Product());
        lenient().doAnswer(invocation -> {
            Promotion promotion = invocation.getArgument(0);
            if (promotion.getId() == null) promotion.setId(100L);
            insertedPromotion.set(promotion);
            return 1;
        }).when(mapper).insert(any(Promotion.class));
        lenient().when(mapper.selectById(100L)).thenAnswer(invocation -> insertedPromotion.get());
        lenient().when(mapper.updateById(any(Promotion.class))).thenReturn(1);
    }

    @Test void productDiscountRequiresProductAndRate() {
        PromotionCreateRequest request = base(PromotionType.PRODUCT_DISCOUNT);
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(BusinessException.class, () -> service.create(request)).getErrorCode());
    }
    @Test void discountRateAboveOneIsRejected() {
        PromotionCreateRequest request = base(PromotionType.PRODUCT_DISCOUNT); request.setProductId(1L); request.setDiscountRate(new BigDecimal("1.01"));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void zeroDiscountRateIsRejected() {
        PromotionCreateRequest request = discount(); request.setDiscountRate(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void negativeDiscountRateIsRejected() {
        PromotionCreateRequest request = discount(); request.setDiscountRate(new BigDecimal("-0.01"));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void missingThresholdIsRejected() {
        PromotionCreateRequest request = threshold(); request.setThresholdAmount(null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void missingReductionIsRejected() {
        PromotionCreateRequest request = threshold(); request.setReductionAmount(null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void zeroThresholdIsRejected() {
        PromotionCreateRequest request = threshold(); request.setThresholdAmount(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void negativeThresholdIsRejected() {
        PromotionCreateRequest request = threshold(); request.setThresholdAmount(new BigDecimal("-1"));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void zeroReductionIsRejected() {
        PromotionCreateRequest request = threshold(); request.setReductionAmount(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void negativeReductionIsRejected() {
        PromotionCreateRequest request = threshold(); request.setReductionAmount(new BigDecimal("-1"));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void thresholdReductionRequiresPositiveAmountsAndReductionNotAboveThreshold() {
        PromotionCreateRequest request = base(PromotionType.ORDER_THRESHOLD_REDUCTION); request.setThresholdAmount(BigDecimal.TEN); request.setReductionAmount(new BigDecimal("11"));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void startMustPrecedeEnd() {
        PromotionCreateRequest request = discount(); request.setStartTime(LocalDateTime.of(2026, 2, 2, 0, 0)); request.setEndTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertThrows(BusinessException.class, () -> service.create(request));
    }
    @Test void activeOverlappingProductDiscountConflicts() {
        PromotionCreateRequest request = discount();
        request.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        request.setEndTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, error.getErrorCode()); verify(mapper, never()).insert(any());
        verify(mapper).selectList(promotionWrapperCaptor.capture());
        assertWrapper(promotionWrapperCaptor.getValue(), false, true);
    }
    @Test void activeOverlappingThresholdRuleConflicts() {
        PromotionCreateRequest request = base(PromotionType.ORDER_THRESHOLD_REDUCTION); request.setThresholdAmount(BigDecimal.TEN); request.setReductionAmount(BigDecimal.ONE);
        request.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0)); request.setEndTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class, () -> service.create(request)).getErrorCode());
        verify(mapper).selectList(promotionWrapperCaptor.capture());
        assertWrapper(promotionWrapperCaptor.getValue(), false, false);
    }
    @Test void disabledRuleDoesNotCheckConflictAndCanBeCreated() {
        PromotionCreateRequest request = discount(); request.setEnabled(false); service.create(request);
        verify(mapper, never()).selectList(any()); verify(mapper).insert(any(Promotion.class));
    }
    @Test void createReturnsReloadedDatabaseTimestamps() {
        Promotion persisted = promotion(8L, false);
        persisted.setCreatedAt(LocalDateTime.of(2026, 9, 6, 12, 0));
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        doAnswer(invocation -> { ((Promotion) invocation.getArgument(0)).setId(8L); return 1; })
                .when(mapper).insert(any(Promotion.class));
        when(mapper.selectById(8L)).thenReturn(persisted);

        assertEquals(persisted.getCreatedAt(), service.create(discount()).getCreatedAt());
    }
    @Test void updateReportsConcurrentModificationWhenVersionLoses() {
        Promotion existing = promotion(5L, true);
        when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        when(mapper.updateById(existing)).thenReturn(0);
        PromotionUpdateRequest request = new PromotionUpdateRequest();
        request.setName("Changed"); request.setType(PromotionType.PRODUCT_DISCOUNT);
        request.setProductId(1L); request.setDiscountRate(new BigDecimal("0.75")); request.setPriority(2);

        assertEquals(ErrorCode.PROMOTION_CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(5L, request)).getErrorCode());
    }
    @Test void updatingAnEnabledRuleRechecksConflicts() {
        Promotion existing = promotion(5L, true); when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        PromotionUpdateRequest request = new PromotionUpdateRequest(); request.setName("Changed"); request.setType(PromotionType.PRODUCT_DISCOUNT); request.setProductId(1L); request.setDiscountRate(new BigDecimal("0.75")); request.setPriority(2);
        request.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0)); request.setEndTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class, () -> service.update(5L, request)).getErrorCode());
        verify(mapper, never()).updateById(any());
        verify(mapper).selectList(promotionWrapperCaptor.capture());
        assertWrapper(promotionWrapperCaptor.getValue(), true, true);
    }
    @Test void nullablePromotionFieldsAreWrittenWhenTheirValueIsNull() {
        assertAlwaysUpdate("productId");
        assertAlwaysUpdate("discountRate");
        assertAlwaysUpdate("thresholdAmount");
        assertAlwaysUpdate("reductionAmount");
        assertAlwaysUpdate("startTime");
        assertAlwaysUpdate("endTime");
    }
    @Test void updateReturnsTheReloadedDatabaseState() {
        Promotion existing = promotion(5L, true);
        existing.setEndTime(LocalDateTime.of(2026, 9, 6, 10, 0));
        existing.setUpdatedAt(LocalDateTime.of(2026, 9, 6, 9, 0));

        Promotion refreshed = promotion(5L, true);
        refreshed.setEndTime(null);
        refreshed.setUpdatedAt(LocalDateTime.of(2026, 9, 6, 11, 0));
        when(mapper.selectById(5L)).thenReturn(existing, refreshed);
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());

        PromotionUpdateRequest request = new PromotionUpdateRequest();
        request.setName("Changed");
        request.setType(PromotionType.PRODUCT_DISCOUNT);
        request.setProductId(1L);
        request.setDiscountRate(new BigDecimal("0.75"));
        request.setPriority(2);
        request.setEndTime(null);

        PromotionView result = service.update(5L, request);

        assertNull(result.getEndTime());
        assertEquals(LocalDateTime.of(2026, 9, 6, 11, 0), result.getUpdatedAt());
    }
    @Test void enablingARuleRechecksConflicts() {
        Promotion existing = promotion(5L, false); when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        assertThrows(BusinessException.class, () -> service.setEnabled(5L, true));
        verify(mapper, never()).updateById(any());
    }
    @Test void locksProductBeforeCheckingDiscountConflict() {
        ProductMapper products = mock(ProductMapper.class); PromotionMutexMapper mutex = mock(PromotionMutexMapper.class);
        PromotionServiceImpl lockingService = promotionService(mapper, products, mutex);
        when(products.selectByIdForUpdate(1L)).thenReturn(new Product());
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        lockingService.create(discount());
        org.mockito.InOrder order = inOrder(products, mapper);
        order.verify(products).selectByIdForUpdate(1L); order.verify(mapper).selectList(any()); order.verify(mapper).insert(any());
    }
    @Test void locksGlobalMutexBeforeCheckingThresholdConflict() {
        ProductMapper products = mock(ProductMapper.class); PromotionMutexMapper mutex = mock(PromotionMutexMapper.class);
        PromotionServiceImpl lockingService = promotionService(mapper, products, mutex);
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        lockingService.create(threshold());
        org.mockito.InOrder order = inOrder(mutex, mapper);
        order.verify(mutex).lockGlobalThreshold(); order.verify(mapper).selectList(any()); order.verify(mapper).insert(any());
    }
    @Test void mapsPromotionWriteIntegrityFailureToPromotionConflict() {
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        when(mapper.insert(any())).thenThrow(new DataIntegrityViolationException("duplicate code"));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(discount())).getErrorCode());
    }
    @Test void missingCodeForChineseNameGeneratesNonEmptyUuidCode() {
        PromotionCreateRequest request = discount(); request.setName("草莓八折"); request.setCode("  ");
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        service.create(request);
        ArgumentCaptor<Promotion> saved = ArgumentCaptor.forClass(Promotion.class); verify(mapper).insert(saved.capture());
        assertTrue(saved.getValue().getCode().matches("PROMO_[A-F0-9]{32}"));
        assertTrue(saved.getValue().getCode().length() <= 64);
    }
    @Test void explicitSymbolOnlyCodeIsRejectedBeforePersistence() {
        PromotionCreateRequest request = discount(); request.setCode(" -- !!! ");
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(BusinessException.class,
                () -> service.create(request)).getErrorCode());
        verify(mapper, never()).insert(any());
    }
    @Test void explicitCodeIsNormalizedAndDatabaseCollisionIsConflict() {
        PromotionCreateRequest request = discount(); request.setCode(" summer-sale ");
        when(mapper.selectList(any())).thenReturn(Collections.<Promotion>emptyList());
        when(mapper.insert(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(request)).getErrorCode());
        ArgumentCaptor<Promotion> saved = ArgumentCaptor.forClass(Promotion.class); verify(mapper).insert(saved.capture());
        assertEquals("SUMMER_SALE", saved.getValue().getCode());
    }
    @Test void explicitCodeLongerThanSchemaLimitIsRejected() {
        PromotionCreateRequest request = discount(); request.setCode(String.join("", Collections.nCopies(65, "A")));
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(BusinessException.class,
                () -> service.create(request)).getErrorCode());
        verify(mapper, never()).insert(any());
    }
    private PromotionCreateRequest discount() { PromotionCreateRequest r = base(PromotionType.PRODUCT_DISCOUNT); r.setProductId(1L); r.setDiscountRate(new BigDecimal("0.80")); return r; }
    private PromotionCreateRequest threshold() { PromotionCreateRequest r = base(PromotionType.ORDER_THRESHOLD_REDUCTION); r.setThresholdAmount(BigDecimal.TEN); r.setReductionAmount(BigDecimal.ONE); return r; }

    private PromotionServiceImpl promotionService(PromotionMapper promotions, ProductMapper productMapper,
                                                  PromotionMutexMapper mutexMapper) {
        PromotionServiceImpl target = new PromotionServiceImpl();
        ReflectionTestUtils.setField(target, "mapper", promotions);
        ReflectionTestUtils.setField(target, "productMapper", productMapper);
        ReflectionTestUtils.setField(target, "mutexMapper", mutexMapper);
        return target;
    }
    private PromotionCreateRequest base(PromotionType type) { PromotionCreateRequest r = new PromotionCreateRequest(); r.setName("Rule"); r.setType(type); r.setPriority(1); r.setEnabled(true); return r; }
    private Promotion promotion(Long id, boolean enabled) { Promotion p = new Promotion(); p.setId(id); p.setName("Rule"); p.setType(PromotionType.PRODUCT_DISCOUNT); p.setProductId(1L); p.setDiscountRate(new BigDecimal("0.80")); p.setPriority(1); p.setEnabled(enabled); return p; }
    private void assertAlwaysUpdate(String property) {
        assertEquals(FieldStrategy.ALWAYS, TableInfoHelper.getTableInfo(Promotion.class).getFieldList().stream()
                .filter(field -> property.equals(field.getProperty())).findFirst().get().getUpdateStrategy());
    }
    private void assertWrapper(LambdaQueryWrapper<Promotion> wrapper, boolean excludesSelf, boolean productScoped) {
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("enabled"));
        assertTrue(sql.contains("type"));
        assertEquals(productScoped, sql.contains("product_id"));
        assertEquals(excludesSelf, sql.contains("id <>"));
        assertTrue(sql.contains("end_time"));
        assertTrue(sql.contains("start_time"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 1, 1, 0, 0)));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 2, 1, 0, 0)));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Boolean.TRUE));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(productScoped ? PromotionType.PRODUCT_DISCOUNT : PromotionType.ORDER_THRESHOLD_REDUCTION));
        if (productScoped) assertTrue(wrapper.getParamNameValuePairs().containsValue(1L));
        if (excludesSelf) assertTrue(wrapper.getParamNameValuePairs().containsValue(5L));
    }
}
