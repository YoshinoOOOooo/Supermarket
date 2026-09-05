package com.supermarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SupermarketApplicationTest {
    @Test
    void applicationEntryPointIsConfigured() {
        assertTrue(SupermarketApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}
