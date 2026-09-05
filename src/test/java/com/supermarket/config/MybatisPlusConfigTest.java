package com.supermarket.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusConfigTest {
    @Test
    void registersOptimisticLockBeforeMysqlPagination() throws Exception {
        Class<?> configType = Class.forName("com.supermarket.config.MybatisPlusConfig");
        Object config = configType.getDeclaredConstructor().newInstance();
        Method factory = configType.getMethod("mybatisPlusInterceptor");
        MybatisPlusInterceptor interceptor = (MybatisPlusInterceptor) factory.invoke(config);
        List<?> interceptors = interceptor.getInterceptors();

        assertEquals(2, interceptors.size());
        assertTrue(interceptors.get(0) instanceof OptimisticLockerInnerInterceptor);
        assertTrue(interceptors.get(1) instanceof PaginationInnerInterceptor);
    }
}
