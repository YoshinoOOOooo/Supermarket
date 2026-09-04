package com.supermarket.controller.admin;

import com.supermarket.service.ProductService;
import com.supermarket.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCatalogControllerTest {
    @Test void productAdminExposesListAndEnabledRoutes() throws Exception {
        ProductService service = mock(ProductService.class); when(service.list()).thenReturn(Collections.emptyList());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductAdminController(service)).build();
        mvc.perform(get("/api/admin/products")).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/products/7/enabled").param("enabled", "false")).andExpect(status().isOk());
        verify(service).setEnabled(7L, false);
    }

    @Test void promotionAdminExposesListAndEnabledRoutes() throws Exception {
        PromotionService service = mock(PromotionService.class); when(service.list()).thenReturn(Collections.emptyList());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PromotionAdminController(service)).build();
        mvc.perform(get("/api/admin/promotions")).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/promotions/9/enabled").param("enabled", "true")).andExpect(status().isOk());
        verify(service).setEnabled(9L, true);
    }
}
