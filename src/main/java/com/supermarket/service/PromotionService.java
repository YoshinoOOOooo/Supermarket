package com.supermarket.service;

import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.vo.PromotionView;
import java.util.List;

public interface PromotionService {
    PromotionView create(PromotionCreateRequest request);
    PromotionView update(Long id, PromotionUpdateRequest request);
    PromotionView setEnabled(Long id, boolean enabled);
    PromotionView find(Long id);
    List<PromotionView> list();
}
