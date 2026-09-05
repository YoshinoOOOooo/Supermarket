package com.supermarket.mapper;

import org.apache.ibatis.annotations.Select;

public interface PromotionMutexMapper {
    @Select("SELECT mutex_key FROM promotion_mutex WHERE mutex_key = 'GLOBAL_THRESHOLD' FOR UPDATE")
    String lockGlobalThreshold();
}
