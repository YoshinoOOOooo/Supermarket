package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.Version;
import com.supermarket.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.supermarket.enums.OrderStatus.CANCELLED;
import static com.supermarket.enums.OrderStatus.COMPLETED;
import static com.supermarket.enums.OrderStatus.UNPAID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {
    @Test
    void exposesRequiredOrderStates() {
        assertArrayEquals(new OrderStatus[]{UNPAID, COMPLETED, CANCELLED}, OrderStatus.values());
    }

    @Test
    void productCarriesMoneyAsBigDecimal() throws Exception {
        assertEquals(BigDecimal.class, Product.class.getDeclaredField("unitPrice").getType());
    }

    @Test
    void mutableAggregatesDeclareOptimisticLockVersions() throws Exception {
        assertTrue(Promotion.class.getDeclaredField("version").isAnnotationPresent(Version.class));
        assertTrue(CustomerOrder.class.getDeclaredField("version").isAnnotationPresent(Version.class));
    }
}
