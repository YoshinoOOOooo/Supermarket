package com.supermarket.exception;

import com.supermarket.dto.CheckoutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidNestedCheckoutItemReturnsUniformBadRequest() throws Exception {
        mockMvc.perform(post("/test/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productCode\":\"APPLE\",\"quantity\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void notFoundBusinessExceptionReturnsUniformNotFound() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("missing product"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void conflictBusinessExceptionReturnsUniformConflict() throws Exception {
        mockMvc.perform(post("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMOTION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("overlapping promotion"));
    }

    @Test
    void unexpectedExceptionReturnsSanitizedInternalError() throws Exception {
        mockMvc.perform(post("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password"))));
    }

    @Test void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/checkout").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test void invalidPathAndQueryTypesReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/number/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/test/query").param("enabled", "perhaps")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/test/required")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test void unrelatedIntegrityFailureRemainsSanitizedInternalError() throws Exception {
        mockMvc.perform(post("/test/integrity")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @RestController
    @Validated
    static class TestController {
        @PostMapping("/test/checkout")
        void checkout(@Valid @RequestBody CheckoutRequest request) {
        }

        @PostMapping("/test/not-found")
        void notFound() {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "missing product");
        }

        @PostMapping("/test/conflict")
        void conflict() {
            throw new BusinessException(ErrorCode.PROMOTION_CONFLICT, "overlapping promotion");
        }

        @PostMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("database password leaked");
        }
        @PostMapping("/test/integrity") void integrity() { throw new DataIntegrityViolationException("secret sql"); }
        @org.springframework.web.bind.annotation.GetMapping("/test/number/{id}") void number(@PathVariable Long id) {}
        @org.springframework.web.bind.annotation.GetMapping("/test/query") void query(@RequestParam Boolean enabled) {}
        @org.springframework.web.bind.annotation.GetMapping("/test/required") void required(@RequestParam String value) {}
    }
}
