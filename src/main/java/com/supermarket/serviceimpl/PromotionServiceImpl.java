package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.entity.Promotion;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.PromotionMapper;
import com.supermarket.mapper.ProductMapper;
import com.supermarket.mapper.PromotionMutexMapper;
import com.supermarket.service.PromotionService;
import com.supermarket.vo.PromotionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import javax.annotation.Resource;

/** 促销管理服务实现，负责规则校验、冲突检测和并发串行化。 */
@Service
public class PromotionServiceImpl implements PromotionService {
    /** 促销规则持久化访问对象。 */
    @Resource
    private PromotionMapper mapper;
    /** 商品访问对象，也用于锁定商品折扣的冲突域。 */
    @Resource
    private ProductMapper productMapper;
    /** 全局满减互斥锁访问对象。 */
    @Resource
    private PromotionMutexMapper mutexMapper;

    /** 创建促销；启用规则会先锁定冲突域并检查时间范围。 */
    @Override @Transactional public PromotionView create(PromotionCreateRequest r) {
        Promotion p = new Promotion();
        p.setCode(normalizeCode(r.getCode()));
        copy(p, r.getName(), r.getType(),
                r.getProductId(), r.getDiscountRate(), r.getThresholdAmount(), r.getReductionAmount(),
                r.getPriority(), r.getStartTime(), r.getEndTime());
        try {
            p.setEnabled(r.getEnabled() == null || r.getEnabled());
            validate(p);
            if (p.getEnabled()) {
                lockConflictDomain(p);
                validateNoConflict(p);
            }
            mapper.insert(p); return view(p);
        }
        catch (DataIntegrityViolationException | CannotAcquireLockException ex) { throw conflict(); }
    }
    /** 更新促销配置；已启用规则必须重新检查冲突。 */
    @Override @Transactional public PromotionView update(Long id, PromotionUpdateRequest r) {
        Promotion p = required(id);
        copy(p, r.getName(), r.getType(), r.getProductId(), r.getDiscountRate(), r.getThresholdAmount(),
                r.getReductionAmount(), r.getPriority(), r.getStartTime(), r.getEndTime());
        try {
            validate(p);
            if (Boolean.TRUE.equals(p.getEnabled())) {
                lockConflictDomain(p); validateNoConflict(p);
            }
            mapper.updateById(p);
            return view(p); }
        catch (DataIntegrityViolationException | CannotAcquireLockException ex) { throw conflict(); }
    }
    /** 启用或停用促销；启用时执行并发安全的冲突校验。 */
    @Override @Transactional public PromotionView setEnabled(Long id, boolean enabled) {
        Promotion p = required(id);
        p.setEnabled(enabled);
        try {
            validate(p);
            if (enabled) {
                lockConflictDomain(p);
                validateNoConflict(p);
            }
            mapper.updateById(p);
            return view(p); }
        catch (DataIntegrityViolationException | CannotAcquireLockException ex) { throw conflict(); }
    }
    /** 根据主键查询促销。 */
    @Override public PromotionView find(Long id) { return view(required(id)); }
    /** 查询全部促销并转换为接口视图。 */
    @Override public List<PromotionView> list() {
        return mapper.selectList(null).stream().map(this::view).collect(Collectors.toList());
    }

    /** 校验促销时间及不同促销类型要求的参数组合。 */
    private void validate(Promotion p) {
        if (p.getStartTime() != null && p.getEndTime() != null && !p.getStartTime().isBefore(p.getEndTime()))
            invalid("Promotion start time must precede end time");

        if (p.getType() == PromotionType.PRODUCT_DISCOUNT) {
            if (p.getProductId() == null || p.getDiscountRate() == null ||
                    p.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getDiscountRate().compareTo(BigDecimal.ONE) > 0)
                invalid("Product discount requires a product and rate in (0,1]");

            p.setThresholdAmount(null); p.setReductionAmount(null);

        } else if (p.getType() == PromotionType.ORDER_THRESHOLD_REDUCTION) {
            if (p.getThresholdAmount() == null || p.getReductionAmount() == null ||
                    p.getThresholdAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getReductionAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getReductionAmount().compareTo(p.getThresholdAmount()) > 0)
                invalid("Threshold reduction requires reduction in (0, threshold]");

            p.setProductId(null); p.setDiscountRate(null);
        } else invalid("Unsupported promotion type");
    }
    /** 查询同类型、同冲突域且生效时间重叠的启用规则。 */
    private void validateNoConflict(Promotion p) {
        LambdaQueryWrapper<Promotion> q = new LambdaQueryWrapper<Promotion>().eq(Promotion::getEnabled, true)
                .eq(Promotion::getType, p.getType());

        if (p.getId() != null) q.ne(Promotion::getId, p.getId());

        if (p.getType() == PromotionType.PRODUCT_DISCOUNT) q.eq(Promotion::getProductId, p.getProductId());

        if (p.getEndTime() != null) q.and(w -> w.isNull(Promotion::getStartTime)
                .or().lt(Promotion::getStartTime, p.getEndTime()));

        if (p.getStartTime() != null) q.and(w -> w.isNull(Promotion::getEndTime)
                .or().gt(Promotion::getEndTime, p.getStartTime()));

        if (!mapper.selectList(q).isEmpty())
            throw new BusinessException(ErrorCode.PROMOTION_CONFLICT, "Promotion time range conflicts with an active rule");
    }
    /**
     * 锁定促销冲突域：商品折扣锁商品行，全局满减锁固定互斥记录。
     * 锁必须先于促销写锁获取，避免形成相反锁顺序。
     */
    private void lockConflictDomain(Promotion p) {
        if (p.getType() == PromotionType.PRODUCT_DISCOUNT) {
            if (productMapper.selectByIdForUpdate(p.getProductId()) == null) invalid("Product not found");
        } else {
            mutexMapper.lockGlobalThreshold();
        }
    }

    /** 将请求中的可编辑属性复制到促销实体。 */
    private void copy(Promotion p, String name, PromotionType type, Long productId,
                      BigDecimal rate, BigDecimal threshold, BigDecimal reduction, Integer priority,
                      java.time.LocalDateTime start, java.time.LocalDateTime end)
    {
        p.setName(name); p.setType(type); p.setProductId(productId);
        p.setDiscountRate(rate); p.setThresholdAmount(threshold); p.setReductionAmount(reduction);
        p.setPriority(priority); p.setStartTime(start); p.setEndTime(end);
    }

    /** 获取必须存在的促销实体。 */
    private Promotion required(Long id) {
        Promotion p = mapper.selectById(id);
        if (p == null) invalid("Promotion not found");
        return p;
    }

    /** 抛出普通请求参数错误。 */
    private void invalid(String message) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    /** 规范化显式业务编码，未填写时生成唯一 UUID 编码。 */
    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "PROMO_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isEmpty() || normalized.length() > 64 || !normalized.matches("[A-Z0-9_]+")) {
            invalid("Promotion code must contain letters or digits and be at most 64 characters");
        }
        return normalized;
    }

    /** 创建统一的促销配置冲突异常。 */
    private BusinessException conflict() {
        return new BusinessException(ErrorCode.PROMOTION_CONFLICT, "Promotion configuration conflicts with another rule");
    }

    /** 将促销实体转换为对外视图。 */
    private PromotionView view(Promotion p) {
        return new PromotionView(p.getId(), p.getCode(), p.getName(), p.getType(), p.getProductId(),
                p.getDiscountRate(), p.getThresholdAmount(), p.getReductionAmount(), p.getPriority(),
                p.getEnabled(), p.getStartTime(), p.getEndTime(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
