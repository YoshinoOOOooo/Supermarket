package com.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.controller.admin.OrderAdminController;
import com.supermarket.enums.OrderStatus;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.exception.GlobalExceptionHandler;
import com.supermarket.service.OrderService;
import com.supermarket.vo.OrderItemView;
import com.supermarket.vo.OrderView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    private final OrderService service = mock(OrderService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new OrderController(service), new OrderAdminController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void publicCreateAndGetReturnSnapshotStates() throws Exception {
        UUID number = UUID.randomUUID();
        when(service.create(any())).thenReturn(view(number, OrderStatus.UNPAID));
        when(service.findByOrderNo(number)).thenReturn(view(number, OrderStatus.UNPAID));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productCode\":\"APPLE\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value(number.toString()))
                .andExpect(jsonPath("$.status").value("UNPAID"))
                .andExpect(jsonPath("$.items[0].productName").value("Apple snapshot"))
                .andExpect(jsonPath("$.payableAmount").value("17.00"));
        mvc.perform(get("/api/orders/{orderNo}", number))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNPAID"));
    }

    @Test
    void publicGetReturnsNotFoundAndCreateValidatesBody() throws Exception {
        UUID missing = UUID.randomUUID();
        when(service.findByOrderNo(missing)).thenThrow(
                new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        mvc.perform(get("/api/orders/{orderNo}", missing))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(service, never()).create(any());
    }

    @Test
    void adminListsPagesAndCompletesOrCancelsOrders() throws Exception {
        UUID number = UUID.randomUUID();
        Page<OrderView> page = new Page<OrderView>(2, 5, 1);
        page.setRecords(Collections.singletonList(view(number, OrderStatus.UNPAID)));
        when(service.list(2, 5)).thenReturn(page);
        when(service.complete(number)).thenReturn(view(number, OrderStatus.COMPLETED));
        when(service.cancel(number)).thenReturn(view(number, OrderStatus.CANCELLED));

        mvc.perform(get("/api/admin/orders").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(2))
                .andExpect(jsonPath("$.records[0].status").value("UNPAID"));
        mvc.perform(post("/api/admin/orders/{orderNo}/complete", number))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/admin/orders/{orderNo}/cancel", number))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminTransitionReturnsConflictForInvalidState() throws Exception {
        UUID number = UUID.randomUUID();
        when(service.cancel(number)).thenThrow(
                new BusinessException(ErrorCode.INVALID_ORDER_STATE, "Invalid order state"));

        mvc.perform(post("/api/admin/orders/{orderNo}/cancel", number))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATE"));
    }

    private OrderView view(UUID number, OrderStatus status) {
        return new OrderView(number.toString(), status, money("20.00"), money("3.00"), money("17.00"),
                Collections.singletonList(new OrderItemView("APPLE", "Apple snapshot", 2,
                        money("10.00"), money("20.00"), money("3.00"), money("17.00"))),
                LocalDateTime.of(2026, 9, 5, 10, 0), LocalDateTime.of(2026, 9, 5, 10, 0));
    }

    private BigDecimal money(String value) { return new BigDecimal(value); }
}
