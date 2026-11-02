package com.pulsepay.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresContainerTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pulsepay_test")
        .withUsername("pulsepay")
        .withPassword("pulsepay_test");

    @Test
    void startsPostgresForIntegrationTests() {
        assertTrue(POSTGRES.isRunning());
    }
}
