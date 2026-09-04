package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.PromotionMapper;
import com.supermarket.service.PromotionService;
import com.supermarket.vo.PromotionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {
    private final PromotionMapper mapper;
    public PromotionServiceImpl(PromotionMapper mapper) { this.mapper = mapper; }

    @Override @Transactional public PromotionView create(PromotionCreateRequest r) {
        Promotion p = new Promotion(); copy(p, r.getName(), r.getType(), r.getProductId(), r.getDiscountRate(), r.getThresholdAmount(), r.getReductionAmount(), r.getPriority(), r.getStartTime(), r.getEndTime());
        p.setEnabled(r.getEnabled() == null || r.getEnabled()); validate(p); if (p.getEnabled()) validateNoConflict(p); mapper.insert(p); return view(p);
    }
    @Override @Transactional public PromotionView update(Long id, PromotionUpdateRequest r) {
        Promotion p = required(id); copy(p, r.getName(), r.getType(), r.getProductId(), r.getDiscountRate(), r.getThresholdAmount(), r.getReductionAmount(), r.getPriority(), r.getStartTime(), r.getEndTime());
        validate(p); if (Boolean.TRUE.equals(p.getEnabled())) validateNoConflict(p); mapper.updateById(p); return view(p);
    }
    @Override @Transactional public PromotionView setEnabled(Long id, boolean enabled) {
        Promotion p = required(id); p.setEnabled(enabled); validate(p); if (enabled) validateNoConflict(p); mapper.updateById(p); return view(p);
    }
    @Override public PromotionView find(Long id) { return view(required(id)); }
    @Override public List<PromotionView> list() { return mapper.selectList(null).stream().map(this::view).collect(Collectors.toList()); }

    private void validate(Promotion p) {
        if (p.getStartTime() != null && p.getEndTime() != null && !p.getStartTime().isBefore(p.getEndTime())) invalid("Promotion start time must precede end time");
        if (p.getType() == PromotionType.PRODUCT_DISCOUNT) {
            if (p.getProductId() == null || p.getDiscountRate() == null || p.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || p.getDiscountRate().compareTo(BigDecimal.ONE) > 0) invalid("Product discount requires a product and rate in (0,1]");
            p.setThresholdAmount(null); p.setReductionAmount(null);
        } else if (p.getType() == PromotionType.ORDER_THRESHOLD_REDUCTION) {
            if (p.getThresholdAmount() == null || p.getReductionAmount() == null || p.getThresholdAmount().compareTo(BigDecimal.ZERO) <= 0 || p.getReductionAmount().compareTo(BigDecimal.ZERO) <= 0 || p.getReductionAmount().compareTo(p.getThresholdAmount()) > 0) invalid("Threshold reduction requires reduction in (0, threshold]");
            p.setProductId(null); p.setDiscountRate(null);
        } else invalid("Unsupported promotion type");
    }
    private void validateNoConflict(Promotion p) {
        LambdaQueryWrapper<Promotion> q = new LambdaQueryWrapper<Promotion>().eq(Promotion::getEnabled, true).eq(Promotion::getType, p.getType());
        if (p.getId() != null) q.ne(Promotion::getId, p.getId());
        if (p.getType() == PromotionType.PRODUCT_DISCOUNT) q.eq(Promotion::getProductId, p.getProductId());
        if (p.getEndTime() != null) q.and(w -> w.isNull(Promotion::getStartTime).or().lt(Promotion::getStartTime, p.getEndTime()));
        if (p.getStartTime() != null) q.and(w -> w.isNull(Promotion::getEndTime).or().gt(Promotion::getEndTime, p.getStartTime()));
        if (!mapper.selectList(q).isEmpty()) throw new BusinessException(ErrorCode.PROMOTION_CONFLICT, "Promotion time range conflicts with an active rule");
    }
    private void copy(Promotion p, String name, PromotionType type, Long productId, BigDecimal rate, BigDecimal threshold, BigDecimal reduction, Integer priority, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        p.setName(name); p.setType(type); p.setProductId(productId); p.setDiscountRate(rate); p.setThresholdAmount(threshold); p.setReductionAmount(reduction); p.setPriority(priority); p.setStartTime(start); p.setEndTime(end);
    }
    private Promotion required(Long id) { Promotion p = mapper.selectById(id); if (p == null) invalid("Promotion not found"); return p; }
    private void invalid(String message) { throw new BusinessException(ErrorCode.INVALID_REQUEST, message); }
    private PromotionView view(Promotion p) { return new PromotionView(p.getId(), p.getName(), p.getType(), p.getProductId(), p.getDiscountRate(), p.getThresholdAmount(), p.getReductionAmount(), p.getPriority(), p.getEnabled(), p.getStartTime(), p.getEndTime(), p.getCreatedAt(), p.getUpdatedAt()); }
}
