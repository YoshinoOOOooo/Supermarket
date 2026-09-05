package com.supermarket.config;

import com.supermarket.service.OrderService;
import com.supermarket.service.ProductService;
import com.supermarket.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.admin.password=openapi-test-password")
@AutoConfigureMockMvc
class AdminOpenApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private PromotionService promotionService;

    @MockBean
    private OrderService orderService;

    @Test
    void generatedAdminOperationsDeclareAdminBasicSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/products'].get.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/products'].post.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/products/{id}'].get.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/products/{id}'].put.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/products/{id}/enabled'].patch.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/promotions'].get.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/promotions'].post.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/promotions/{id}'].get.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/promotions/{id}'].put.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/promotions/{id}/enabled'].patch.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/orders'].get.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/orders/{orderNo}/complete'].post.security[0].adminBasic").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/orders/{orderNo}/cancel'].post.security[0].adminBasic").isArray());
    }
}
