package com.supermarket.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseFixtureResourceTest {
    @Test
    void mysqlFixturesExistAndCannotSwitchDatabases() throws Exception {
        assertSafeFixture("db/test-schema.sql");
        assertSafeFixture("db/test-data.sql");
    }

    private void assertSafeFixture(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), path + " must exist");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).toUpperCase();
        assertFalse(sql.contains("CREATE DATABASE"), path + " must not create a database");
        assertFalse(sql.matches("(?s).*\\bUSE\\s+[A-Z0-9_]+.*"), path + " must not switch databases");
    }
}
