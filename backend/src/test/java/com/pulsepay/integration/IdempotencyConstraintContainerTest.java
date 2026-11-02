package com.pulsepay.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Confirms the operation-scoped idempotency unique constraint used by financial endpoints. */
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyConstraintContainerTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pulsepay_idempotency_test")
        .withUsername("pulsepay")
        .withPassword("pulsepay_test");

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
    }

    @Test
    void key_is_unique_per_merchant_and_operation_but_reusable_for_a_different_operation() throws Exception {
        UUID merchant = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (PreparedStatement statement = connection.prepareStatement("insert into merchants(id,legal_name,display_name) values(?,?,?)")) {
                statement.setObject(1, merchant);
                statement.setString(2, "Idempotency Test Merchant");
                statement.setString(3, "Idempotency Test");
                statement.executeUpdate();
            }
            assertDoesNotThrow(() -> insert(connection, merchant, "same-key", "payment_intent.create"));
            assertDoesNotThrow(() -> insert(connection, merchant, "same-key", "payment_intent.confirm:pi_demo"));
            assertThrows(SQLException.class, () -> insert(connection, merchant, "same-key", "payment_intent.create"));
        }
    }

    private static void insert(Connection connection, UUID merchant, String key, String operation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into idempotency_records(id,merchant_id,idempotency_key,operation,request_fingerprint,expires_at) values(?,?,?,?,?,now()+interval '1 day')")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, merchant);
            statement.setString(3, key);
            statement.setString(4, operation);
            statement.setString(5, "a".repeat(64));
            statement.executeUpdate();
        }
    }
}
