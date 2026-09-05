package com.supermarket.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("mysql")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PricingScenariosAcceptanceTest {
    private static final String ADMIN_USERNAME = "acceptance-admin";
    private static final String ADMIN_PASSWORD = "acceptance-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void initializeDedicatedDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            assertTrue(jdbcUrl.matches("(?i)^jdbc:mysql://[^/]+/supermarket_test(?:\\?.*)?$"),
                    "MySQL acceptance tests may only modify the dedicated supermarket_test schema: " + jdbcUrl);
        }

        new ResourceDatabasePopulator(new ClassPathResource("db/test-schema.sql"))
                .execute(dataSource);
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM customer_order");
        jdbcTemplate.update("DELETE FROM promotion");
        jdbcTemplate.update("DELETE FROM product");
        new ResourceDatabasePopulator(new ClassPathResource("db/test-data.sql"))
                .execute(dataSource);
    }

    @Test
    void verifiesExactPricingScenariosAndOrderCompletion() throws Exception {
        setPromotions(false, false);
        assertCheckout("55.00", item("APPLE", 2), item("STRAWBERRY", 3));
        assertCheckout("75.00", item("APPLE", 2), item("STRAWBERRY", 3), item("MANGO", 1));

        setPromotions(true, false);
        assertCheckout("67.20", item("APPLE", 2), item("STRAWBERRY", 3), item("MANGO", 1));

        setPromotions(true, true);
        assertCheckout("102.00", item("APPLE", 5), item("STRAWBERRY", 5), item("MANGO", 1));

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(request(item("APPLE", 5), item("STRAWBERRY", 5), item("MANGO", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNPAID")))
                .andExpect(jsonPath("$.payableAmount", is("102.00")))
                .andReturn();

        JsonNode order = objectMapper.readTree(created.getResponse().getContentAsString());
        String orderNo = order.get("orderNo").asText();

        mockMvc.perform(put("/api/orders/{orderNo}", orderNo)
                        .contentType("application/json").content(request(item("APPLE", 1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UNPAID")))
                .andExpect(jsonPath("$.payableAmount", is("8.00")));

        mockMvc.perform(get("/api/orders/{orderNo}", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo", is(orderNo)))
                .andExpect(jsonPath("$.status", is("UNPAID")))
                .andExpect(jsonPath("$.payableAmount", is("8.00")));

        mockMvc.perform(post("/api/admin/orders/{orderNo}/complete", orderNo)
                        .header("Authorization", basic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo", is(orderNo)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verifiesPromotionEndTimeCanBeCleared();
    }

    private void verifiesPromotionEndTimeCanBeCleared() throws Exception {
        Long promotionId = jdbcTemplate.queryForObject(
                "SELECT id FROM promotion WHERE code = 'STRAWBERRY_80'", Long.class);
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE code = 'STRAWBERRY'", Long.class);
        jdbcTemplate.update("UPDATE promotion SET end_time = ? WHERE id = ?",
                LocalDateTime.of(2026, 12, 31, 23, 59), promotionId);

        String body = "{\"name\":\"Strawberry 80% Discount\","
                + "\"type\":\"PRODUCT_DISCOUNT\",\"productId\":" + productId + ","
                + "\"discountRate\":0.80,\"priority\":100,\"startTime\":null,\"endTime\":null}";
        mockMvc.perform(put("/api/admin/promotions/{id}", promotionId)
                        .header("Authorization", basic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").doesNotExist());

        assertTrue(jdbcTemplate.queryForObject(
                "SELECT end_time IS NULL FROM promotion WHERE id = ?", Boolean.class, promotionId));
    }

    private void assertCheckout(String expectedTotal, String... items) throws Exception {
        mockMvc.perform(post("/api/checkout/calculate")
                        .contentType("application/json")
                        .content(request(items)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payableAmount", is(expectedTotal)));
    }

    private void setPromotions(boolean productDiscount, boolean thresholdReduction) {
        jdbcTemplate.update("UPDATE promotion SET enabled = ? WHERE type = 'PRODUCT_DISCOUNT'", productDiscount);
        jdbcTemplate.update("UPDATE promotion SET enabled = ? WHERE type = 'ORDER_THRESHOLD_REDUCTION'", thresholdReduction);
    }

    private String request(String... items) {
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    private String item(String productCode, int quantity) {
        return "{\"productCode\":\"" + productCode + "\",\"quantity\":" + quantity + "}";
    }

    private String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }
}
