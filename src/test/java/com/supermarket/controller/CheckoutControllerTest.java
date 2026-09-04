package com.supermarket.controller;

import com.supermarket.service.CheckoutService;
import com.supermarket.vo.CheckoutItemView;
import com.supermarket.vo.CheckoutResultView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private CheckoutService service;

    @Test
    void calculateReturnsPublicQuoteWithTwoDecimalMoney() throws Exception {
        when(service.calculate(any())).thenReturn(new CheckoutResultView(
                Collections.singletonList(new CheckoutItemView("APPLE", "Apple", 2,
                        money("8.00"), money("16.00"), money("0.00"), money("16.00"))),
                money("16.00"), money("0.00"), money("16.00")));

        mvc.perform(post("/api/checkout/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productCode\":\"APPLE\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(8.00))
                .andExpect(jsonPath("$.originalAmount").value(16.00))
                .andExpect(jsonPath("$.discountAmount").value(0.00))
                .andExpect(jsonPath("$.payableAmount").value(16.00));

        verify(service).calculate(any());
    }

    @Test
    void calculateRejectsNegativeQuantityBeforeCallingService() throws Exception {
        mvc.perform(post("/api/checkout/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productCode\":\"APPLE\",\"quantity\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(service, never()).calculate(any());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
