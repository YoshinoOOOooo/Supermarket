package com.supermarket.serviceimpl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.supermarket.dto.CheckoutItemRequest;
import com.supermarket.dto.CheckoutRequest;
import com.supermarket.entity.CustomerOrder;
import com.supermarket.entity.OrderItem;
import com.supermarket.enums.OrderStatus;
import com.supermarket.exception.BusinessException;
import com.supermarket.exception.ErrorCode;
import com.supermarket.mapper.CustomerOrderMapper;
import com.supermarket.mapper.OrderItemMapper;
import com.supermarket.pricing.PricingQuote;
import com.supermarket.service.PricingQuoteService;
import com.supermarket.vo.OrderView;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private final PricingQuoteService pricing = mock(PricingQuoteService.class);
    private final CustomerOrderMapper orders = mock(CustomerOrderMapper.class);
    private final OrderItemMapper items = mock(OrderItemMapper.class);
    private final OrderServiceImpl service = new OrderServiceImpl(pricing, orders, items);
    @BeforeAll
    static void initializeLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderItem.class);
    }

    @Test
    void createRecalculatesAssignsUuidAndPersistsHeaderBeforeSnapshotInOneTransaction() throws Exception {
        CheckoutRequest request = request(item("APPLE", 2));
        when(pricing.quote(request)).thenReturn(quote());
        doAnswer(invocation -> { ((CustomerOrder) invocation.getArgument(0)).setId(41L); return 1; })
                .when(orders).insert(any(CustomerOrder.class));

        OrderView result = service.create(request);

        UUID.fromString(result.getOrderNo());
        assertEquals(OrderStatus.UNPAID, result.getStatus());
        assertMoney("20.00", result.getOriginalAmount());
        assertMoney("3.00", result.getDiscountAmount());
        assertMoney("17.00", result.getPayableAmount());
        assertEquals(1, result.getItems().size());
        assertEquals(Long.valueOf(7L), captureItem().getProductId());
        assertEquals(Long.valueOf(41L), captureItem().getOrderId());
        assertMoney("17.00", captureItem().getPayableAmount());
        InOrder inOrder = inOrder(orders, items);
        inOrder.verify(orders).insert(any(CustomerOrder.class));
        inOrder.verify(items).insert(any(OrderItem.class));
        assertNotNull(OrderServiceImpl.class.getMethod("create", CheckoutRequest.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void createRejectsFormalOrderWhenEveryQuantityIsZero() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(request(item("APPLE", 0), item("MANGO", 0))));
        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
        verifyNoInteractions(pricing, orders, items);
    }

    @Test
    void findReturnsPersistedSnapshotsAndMissingOrderIsNotFound() {
        UUID number = UUID.randomUUID();
        when(orders.selectOne(any())).thenReturn(order(9L, number, OrderStatus.UNPAID));
        when(items.selectList(any())).thenReturn(Collections.singletonList(snapshot(9L)));

        OrderView found = service.findByOrderNo(number);

        assertEquals(number.toString(), found.getOrderNo());
        assertEquals("Apple at checkout", found.getItems().get(0).getProductName());

        reset(orders);
        when(orders.selectOne(any())).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.findByOrderNo(number));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void completeAndCancelGuardTerminalStateAndTreatSameTargetAsIdempotent() {
        UUID number = UUID.randomUUID();
        CustomerOrder unpaid = order(9L, number, OrderStatus.UNPAID);
        AtomicReference<LambdaUpdateWrapper<?>> capturedGuard = new AtomicReference<>();
        when(orders.selectOne(any())).thenReturn(unpaid);
        doAnswer(invocation -> {
            Object argument = invocation.getArgument(1);
            assertTrue(argument instanceof LambdaUpdateWrapper<?>);
            capturedGuard.set((LambdaUpdateWrapper<?>) argument);
            return 1;
        }).when(orders).update(any(CustomerOrder.class), anyOrderWrapper());
        when(items.selectList(any())).thenReturn(Collections.singletonList(snapshot(9L)));

        assertEquals(OrderStatus.COMPLETED, service.complete(number).getStatus());
        LambdaUpdateWrapper<?> guard = capturedGuard.get();
        assertNotNull(guard);
        assertTrue(guard.getSqlSegment().contains("id"));
        assertTrue(guard.getSqlSegment().contains("status"));
        assertTrue(guard.getParamNameValuePairs().containsValue(9L));
        assertTrue(guard.getParamNameValuePairs().containsValue(OrderStatus.UNPAID));

        reset(orders, items);
        when(orders.selectOne(any())).thenReturn(order(9L, number, OrderStatus.UNPAID));
        when(orders.update(any(CustomerOrder.class), anyOrderWrapper())).thenReturn(1);
        when(items.selectList(any())).thenReturn(Collections.singletonList(snapshot(9L)));
        assertEquals(OrderStatus.CANCELLED, service.cancel(number).getStatus());

        reset(orders, items);
        when(orders.selectOne(any())).thenReturn(order(9L, number, OrderStatus.COMPLETED));
        when(items.selectList(any())).thenReturn(Collections.singletonList(snapshot(9L)));
        assertEquals(OrderStatus.COMPLETED, service.complete(number).getStatus());
        verify(orders, never()).update(any(CustomerOrder.class), anyOrderWrapper());
        BusinessException completedToCancelled = assertThrows(BusinessException.class, () -> service.cancel(number));
        assertEquals(ErrorCode.INVALID_ORDER_STATE, completedToCancelled.getErrorCode());

        reset(orders, items);
        when(orders.selectOne(any())).thenReturn(order(9L, number, OrderStatus.CANCELLED));
        when(items.selectList(any())).thenReturn(Collections.singletonList(snapshot(9L)));
        assertEquals(OrderStatus.CANCELLED, service.cancel(number).getStatus());
        BusinessException cancelledToCompleted = assertThrows(BusinessException.class, () -> service.complete(number));
        assertEquals(ErrorCode.INVALID_ORDER_STATE, cancelledToCompleted.getErrorCode());
    }

    @Test
    void transitionReportsConcurrentConflictWhenOptimisticUpdateLoses() {
        UUID number = UUID.randomUUID();
        when(orders.selectOne(any())).thenReturn(order(9L, number, OrderStatus.UNPAID));
        when(orders.update(any(CustomerOrder.class), anyOrderWrapper())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.complete(number));

        assertEquals(ErrorCode.INVALID_ORDER_STATE, error.getErrorCode());
        verify(items, never()).selectList(any());
    }

    private PricingQuote quote() {
        return new PricingQuote(Collections.singletonList(new PricingQuote.Line(
                7L, "APPLE", "Apple at checkout", 2, money("10.00"), money("20.00"),
                money("3.00"), money("17.00"))), money("20.00"), money("3.00"), money("17.00"));
    }

    private CustomerOrder order(long id, UUID number, OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setId(id); order.setOrderNo(number.toString()); order.setStatus(status);
        order.setOriginalAmount(money("20.00")); order.setDiscountAmount(money("3.00"));
        order.setPayableAmount(money("17.00")); order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    private OrderItem snapshot(long orderId) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId); item.setProductId(7L); item.setProductCode("APPLE");
        item.setProductName("Apple at checkout"); item.setQuantity(2); item.setUnitPrice(money("10.00"));
        item.setOriginalAmount(money("20.00")); item.setDiscountAmount(money("3.00"));
        item.setPayableAmount(money("17.00"));
        return item;
    }

    private OrderItem captureItem() {
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(items, atLeastOnce()).insert(captor.capture());
        return captor.getValue();
    }

    private Wrapper<CustomerOrder> anyOrderWrapper() {
        return org.mockito.ArgumentMatchers.<Wrapper<CustomerOrder>>any();
    }

    private CheckoutRequest request(CheckoutItemRequest... items) { return new CheckoutRequest(Arrays.asList(items)); }
    private CheckoutItemRequest item(String code, int quantity) { return new CheckoutItemRequest(code, quantity); }
    private BigDecimal money(String value) { return new BigDecimal(value); }
    private void assertMoney(String expected, BigDecimal actual) { assertEquals(money(expected), actual); }
}
