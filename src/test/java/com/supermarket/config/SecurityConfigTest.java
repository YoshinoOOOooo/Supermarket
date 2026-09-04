package com.supermarket.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig
@ContextConfiguration(classes = {
        SecurityConfig.class,
        SecurityConfigTest.MvcConfiguration.class,
        SecurityConfigTest.ProbeController.class
})
@TestPropertySource(properties = {
        "app.security.admin.username=local-admin",
        "app.security.admin.password=test-secret"
})
class SecurityConfigTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void checkoutRouteIsPublic() throws Exception {
        mockMvc.perform(post("/api/checkout/calculate"))
                .andExpect(status().isNoContent());
    }

    @Test
    void orderRoutesArePublic() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/orders/order-number"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminRouteRejectsMissingCredentials() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRouteRejectsWrongCredentials() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", basic("local-admin", "wrong-secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRouteAcceptsConfiguredAdministrator() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", basic("local-admin", "test-secret")))
                .andExpect(status().isNoContent());
    }

    private static String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    @EnableWebMvc
    static class MvcConfiguration {
    }

    @RestController
    static class ProbeController {
        @PostMapping("/api/checkout/calculate")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void checkout() {
        }

        @PostMapping("/api/orders")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void createOrder() {
        }

        @GetMapping("/api/orders/{orderNo}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void findOrder() {
        }

        @GetMapping("/api/admin/products")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void adminProducts() {
        }
    }
}
