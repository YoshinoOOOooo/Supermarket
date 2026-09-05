package com.supermarket.controller.admin;

import com.supermarket.dto.ProductCreateRequest;
import com.supermarket.dto.ProductUpdateRequest;
import com.supermarket.dto.PromotionCreateRequest;
import com.supermarket.dto.PromotionUpdateRequest;
import com.supermarket.enums.PromotionType;
import com.supermarket.exception.GlobalExceptionHandler;
import com.supermarket.service.ProductService;
import com.supermarket.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCatalogControllerTest {
    @Test void productAdminCoversCrudListAndEnabledPatchWithDtoBinding() throws Exception {
        ProductService service = mock(ProductService.class);
        when(service.list()).thenReturn(Collections.emptyList());
        MockMvc mvc = mvc(productController(service));

        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\" apple-01 \",\"name\":\"Apple\",\"unitPrice\":3.50}")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/products/7")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/products")).andExpect(status().isOk());
        mvc.perform(put("/api/admin/products/7").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Green Apple\",\"unitPrice\":4.25}")).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/products/7/enabled").param("enabled", "false")).andExpect(status().isOk());

        ArgumentCaptor<ProductCreateRequest> create = ArgumentCaptor.forClass(ProductCreateRequest.class);
        ArgumentCaptor<ProductUpdateRequest> update = ArgumentCaptor.forClass(ProductUpdateRequest.class);
        verify(service).create(create.capture()); verify(service).find(7L); verify(service).list();
        verify(service).update(eq(7L), update.capture()); verify(service).setEnabled(7L, false);
        assertEquals(" apple-01 ", create.getValue().getCode());
        assertEquals(new BigDecimal("3.50"), create.getValue().getUnitPrice());
        assertEquals("Green Apple", update.getValue().getName());
    }

    @Test void productAdminRejectsInvalidCreateAndUpdateDtos() throws Exception {
        ProductService service = mock(ProductService.class); MockMvc mvc = mvc(productController(service));
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"\",\"name\":\"\",\"unitPrice\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/products/7").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"unitPrice\":-1}")).andExpect(status().isBadRequest());
        verify(service, never()).create(any()); verify(service, never()).update(anyLong(), any());
    }

    @Test void promotionAdminCoversCrudListAndEnabledPatchWithDtoBinding() throws Exception {
        PromotionService service = mock(PromotionService.class); when(service.list()).thenReturn(Collections.emptyList());
        MockMvc mvc = mvc(promotionController(service));
        String createJson = "{\"name\":\"Apple sale\",\"type\":\"PRODUCT_DISCOUNT\",\"productId\":3,\"discountRate\":0.8,\"priority\":2,\"enabled\":true,\"startTime\":\"2026-01-01T00:00:00\",\"endTime\":\"2026-02-01T00:00:00\"}";
        String updateJson = "{\"name\":\"Apple sale 2\",\"type\":\"PRODUCT_DISCOUNT\",\"productId\":3,\"discountRate\":0.75,\"priority\":3}";
        mvc.perform(post("/api/admin/promotions").contentType(MediaType.APPLICATION_JSON).content(createJson)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/promotions/9")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/promotions")).andExpect(status().isOk());
        mvc.perform(put("/api/admin/promotions/9").contentType(MediaType.APPLICATION_JSON).content(updateJson)).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/promotions/9/enabled").param("enabled", "true")).andExpect(status().isOk());

        ArgumentCaptor<PromotionCreateRequest> create = ArgumentCaptor.forClass(PromotionCreateRequest.class);
        ArgumentCaptor<PromotionUpdateRequest> update = ArgumentCaptor.forClass(PromotionUpdateRequest.class);
        verify(service).create(create.capture()); verify(service).find(9L); verify(service).list();
        verify(service).update(eq(9L), update.capture()); verify(service).setEnabled(9L, true);
        assertEquals(PromotionType.PRODUCT_DISCOUNT, create.getValue().getType());
        assertEquals(Long.valueOf(3), create.getValue().getProductId());
        assertEquals(new BigDecimal("0.75"), update.getValue().getDiscountRate());
    }

    @Test void promotionAdminRejectsInvalidCreateAndUpdateDtos() throws Exception {
        PromotionService service = mock(PromotionService.class); MockMvc mvc = mvc(promotionController(service));
        mvc.perform(post("/api/admin/promotions").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"type\":null,\"priority\":null}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/promotions/9").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"type\":null,\"priority\":null}")).andExpect(status().isBadRequest());
        verify(service, never()).create(any()); verify(service, never()).update(anyLong(), any());
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ProductAdminController productController(ProductService service) {
        ProductAdminController target = new ProductAdminController();
        ReflectionTestUtils.setField(target, "service", service);
        return target;
    }

    private PromotionAdminController promotionController(PromotionService service) {
        PromotionAdminController target = new PromotionAdminController();
        ReflectionTestUtils.setField(target, "service", service);
        return target;
    }
}
