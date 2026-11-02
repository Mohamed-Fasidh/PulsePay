package com.pulsepay.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies the persistence half of the transactional-outbox contract. */
@Testcontainers(disabledWithoutDocker = true)
class OutboxTransactionContainerTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pulsepay_outbox_test")
        .withUsername("pulsepay")
        .withPassword("pulsepay_test");

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
    }

    @Test
    void committed_event_is_visible_and_rolled_back_event_is_not() throws Exception {
        UUID merchant = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            merchant(connection, merchant);

            connection.setAutoCommit(false);
            UUID committedPayment = payment(connection, merchant, "pi_committed");
            event(connection, merchant, committedPayment, "payment.created");
            connection.commit();
            assertEquals(1, count(connection, merchant, "payment.created"));

            connection.setAutoCommit(false);
            UUID rolledBackPayment = payment(connection, merchant, "pi_rolled_back");
            event(connection, merchant, rolledBackPayment, "payment.processing");
            connection.rollback();
            assertEquals(0, count(connection, merchant, "payment.processing"));
        }
    }

    private static void merchant(Connection connection, UUID id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("insert into merchants(id,legal_name,display_name) values(?,?,?)")) {
            statement.setObject(1, id);
            statement.setString(2, "Outbox Test Merchant");
            statement.setString(3, "Outbox Test");
            statement.executeUpdate();
        }
    }

    private static UUID payment(Connection connection, UUID merchant, String publicId) throws Exception {
        UUID id = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("insert into payment_intents(id,public_id,merchant_id,amount_minor,currency,status) values(?,?,?,?,?,?)")) {
            statement.setObject(1, id);
            statement.setString(2, publicId);
            statement.setObject(3, merchant);
            statement.setLong(4, 100);
            statement.setString(5, "USD");
            statement.setString(6, "REQUIRES_CONFIRMATION");
            statement.executeUpdate();
        }
        return id;
    }

    private static void event(Connection connection, UUID merchant, UUID aggregate, String type) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("insert into outbox_events(id,merchant_id,aggregate_type,aggregate_id,event_type,correlation_id,payload) values(?,?,? ,?,?,?, '{}'::jsonb)")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, merchant);
            statement.setString(3, "payment_intent");
            statement.setObject(4, aggregate);
            statement.setString(5, type);
            statement.setObject(6, UUID.randomUUID());
            statement.executeUpdate();
        }
    }

    private static int count(Connection connection, UUID merchant, String type) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select count(*) from outbox_events where merchant_id=? and event_type=?")) {
            statement.setObject(1, merchant);
            statement.setString(2, type);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
