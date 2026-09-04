package com.supermarket.serviceimpl;

import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.PromotionMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromotionServiceImplTest {
    private final PromotionMapper mapper = mock(PromotionMapper.class);
    private final PromotionServiceImpl service = new PromotionServiceImpl(mapper);

    @Test void productDiscountRequiresProductAndRate() {
        PromotionCreateRequest request = base(PromotionType.PRODUCT_DISCOUNT);
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(BusinessException.class, () -> service.create(request)).getErrorCode());
    }
    @Test void discountRateMustBeGreaterThanZeroAndAtMostOne() {
        PromotionCreateRequest request = base(PromotionType.PRODUCT_DISCOUNT); request.setProductId(1L); request.setDiscountRate(new BigDecimal("1.01"));
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
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        BusinessException error = assertThrows(BusinessException.class, () -> service.create(discount()));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, error.getErrorCode()); verify(mapper, never()).insert(any());
    }
    @Test void activeOverlappingThresholdRuleConflicts() {
        PromotionCreateRequest request = base(PromotionType.ORDER_THRESHOLD_REDUCTION); request.setThresholdAmount(BigDecimal.TEN); request.setReductionAmount(BigDecimal.ONE);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class, () -> service.create(request)).getErrorCode());
    }
    @Test void disabledRuleDoesNotCheckConflictAndCanBeCreated() {
        PromotionCreateRequest request = discount(); request.setEnabled(false); service.create(request);
        verify(mapper, never()).selectList(any()); verify(mapper).insert(any(Promotion.class));
    }
    @Test void updatingAnEnabledRuleRechecksConflicts() {
        Promotion existing = promotion(5L, true); when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        PromotionUpdateRequest request = new PromotionUpdateRequest(); request.setName("Changed"); request.setType(PromotionType.PRODUCT_DISCOUNT); request.setProductId(1L); request.setDiscountRate(new BigDecimal("0.75")); request.setPriority(2);
        assertEquals(ErrorCode.PROMOTION_CONFLICT, assertThrows(BusinessException.class, () -> service.update(5L, request)).getErrorCode());
        verify(mapper, never()).updateById(any());
    }
    @Test void enablingARuleRechecksConflicts() {
        Promotion existing = promotion(5L, false); when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Promotion()));
        assertThrows(BusinessException.class, () -> service.setEnabled(5L, true));
        verify(mapper, never()).updateById(any());
    }
    private PromotionCreateRequest discount() { PromotionCreateRequest r = base(PromotionType.PRODUCT_DISCOUNT); r.setProductId(1L); r.setDiscountRate(new BigDecimal("0.80")); return r; }
    private PromotionCreateRequest base(PromotionType type) { PromotionCreateRequest r = new PromotionCreateRequest(); r.setName("Rule"); r.setType(type); r.setPriority(1); r.setEnabled(true); return r; }
    private Promotion promotion(Long id, boolean enabled) { Promotion p = new Promotion(); p.setId(id); p.setName("Rule"); p.setType(PromotionType.PRODUCT_DISCOUNT); p.setProductId(1L); p.setDiscountRate(new BigDecimal("0.80")); p.setPriority(1); p.setEnabled(enabled); return p; }
}
