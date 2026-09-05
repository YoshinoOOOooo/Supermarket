package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.supermarket.dto.ProductCreateRequest;
import com.supermarket.dto.ProductUpdateRequest;
import com.supermarket.entity.Product;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    private final ProductMapper mapper = mock(ProductMapper.class);
    private final ProductServiceImpl service = productService();

    private ProductServiceImpl productService() {
        ProductServiceImpl target = new ProductServiceImpl();
        ReflectionTestUtils.setField(target, "mapper", mapper);
        return target;
    }
    @Captor private ArgumentCaptor<LambdaQueryWrapper<Product>> productWrapperCaptor;
    private final AtomicReference<Product> insertedProduct = new AtomicReference<Product>();

    @BeforeAll static void initializeLambdaMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Product.class);
    }

    @BeforeEach void emulateSuccessfulWrites() {
        lenient().doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getId() == null) product.setId(100L);
            insertedProduct.set(product);
            return 1;
        }).when(mapper).insert(any(Product.class));
        lenient().when(mapper.selectById(100L)).thenAnswer(invocation -> insertedProduct.get());
        lenient().when(mapper.updateById(any(Product.class))).thenReturn(1);
    }

    @Test void createNormalizesCodeAndPersistsEnabledProduct() {
        when(mapper.selectCount(any())).thenReturn(0L);
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCode("  apple-01 "); request.setName("Apple"); request.setUnitPrice(new BigDecimal("3.50"));
        service.create(request);
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(mapper).insert(saved.capture());
        verify(mapper).selectCount(productWrapperCaptor.capture());
        assertEquals("APPLE-01", saved.getValue().getCode());
        assertTrue(saved.getValue().getEnabled());
        assertTrue(productWrapperCaptor.getValue().getSqlSegment().contains("code"));
        assertTrue(productWrapperCaptor.getValue().getParamNameValuePairs().containsValue("APPLE-01"));
    }

    @Test void createRejectsDuplicateNormalizedCode() {
        when(mapper.selectCount(any())).thenReturn(1L);
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCode(" apple-01 "); request.setName("Apple"); request.setUnitPrice(BigDecimal.ONE);
        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals(ErrorCode.RESOURCE_CONFLICT, error.getErrorCode());
        verify(mapper).selectCount(productWrapperCaptor.capture());
        assertTrue(productWrapperCaptor.getValue().getSqlSegment().contains("code"));
        assertTrue(productWrapperCaptor.getValue().getParamNameValuePairs().containsValue("APPLE-01"));
        verify(mapper, never()).insert(any());
    }

    @Test void updateChangesOnlyMutableFields() {
        Product product = product(7L, "APPLE-01", "Old", BigDecimal.ONE, true);
        when(mapper.selectById(7L)).thenReturn(product);
        ProductUpdateRequest request = new ProductUpdateRequest(); request.setName("New"); request.setUnitPrice(new BigDecimal("2.25"));
        service.update(7L, request);
        assertEquals("APPLE-01", product.getCode()); assertEquals("New", product.getName()); assertEquals(new BigDecimal("2.25"), product.getUnitPrice());
        verify(mapper).updateById(product);
    }

    @Test void updateReturnsReloadedDatabaseTimestamps() {
        Product existing = product(7L, "APPLE", "Old", BigDecimal.ONE, true);
        Product refreshed = product(7L, "APPLE", "New", new BigDecimal("2.25"), true);
        refreshed.setUpdatedAt(LocalDateTime.of(2026, 9, 6, 12, 0));
        when(mapper.selectById(7L)).thenReturn(existing, refreshed);
        when(mapper.updateById(any(Product.class))).thenReturn(1);
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("New"); request.setUnitPrice(new BigDecimal("2.25"));

        assertEquals(refreshed.getUpdatedAt(), service.update(7L, request).getUpdatedAt());
    }

    @Test void setEnabledSoftDisablesWithoutPhysicalDelete() {
        Product product = product(7L, "A", "Apple", BigDecimal.ONE, true);
        when(mapper.selectById(7L)).thenReturn(product);
        service.setEnabled(7L, false);
        assertFalse(product.getEnabled()); verify(mapper).updateById(product); verify(mapper, never()).deleteById(anyLong());
    }

    @Test void missingProductRaisesNotFound() {
        when(mapper.selectById(99L)).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class, () -> service.find(99L));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, error.getErrorCode());
    }

    @Test void listMapsEntitiesToViews() {
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(product(1L, "A", "Apple", BigDecimal.ONE, true)));
        assertEquals("A", service.list().get(0).getCode());
    }

    @Test void mapsConcurrentDuplicateCodeToResourceConflict() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCode("APPLE"); request.setName("Apple"); request.setUnitPrice(new BigDecimal("8.00"));
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(ErrorCode.RESOURCE_CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request)).getErrorCode());
    }

    private Product product(Long id, String code, String name, BigDecimal price, boolean enabled) {
        Product p = new Product(); p.setId(id); p.setCode(code); p.setName(name); p.setUnitPrice(price); p.setEnabled(enabled); return p;
    }
}
