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

/** Exercises the deferred database ledger constraints, not just service-level checks. */
@Testcontainers(disabledWithoutDocker = true)
class LedgerIntegrityContainerTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("pulsepay_ledger_test")
        .withUsername("pulsepay")
        .withPassword("pulsepay_test");

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
    }

    @Test
    void balanced_journal_commits_and_posted_entry_cannot_be_changed() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            Fixture fixture = fixture(connection, "balanced");
            entry(connection, fixture.journal(), fixture.debitAccount(), 500, 0);
            entry(connection, fixture.journal(), fixture.creditAccount(), 0, 500);
            assertDoesNotThrow(connection::commit);

            try (PreparedStatement statement = connection.prepareStatement("update ledger_entries set debit_minor=400 where journal_id=?")) {
                statement.setObject(1, fixture.journal());
                assertThrows(SQLException.class, statement::executeUpdate);
            }
        }
    }

    @Test
    void unbalanced_journal_is_rejected_at_transaction_commit() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            Fixture fixture = fixture(connection, "unbalanced");
            entry(connection, fixture.journal(), fixture.debitAccount(), 501, 0);
            assertThrows(SQLException.class, connection::commit);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Fixture fixture(Connection connection, String referencePrefix) throws SQLException {
        UUID merchant = UUID.randomUUID();
        UUID journal = UUID.randomUUID();
        UUID debit = UUID.randomUUID();
        UUID credit = UUID.randomUUID();
        try (PreparedStatement merchantStatement = connection.prepareStatement("insert into merchants(id,legal_name,display_name) values(?,?,?)")) {
            merchantStatement.setObject(1, merchant);
            merchantStatement.setString(2, "Ledger Test Merchant");
            merchantStatement.setString(3, "Ledger Test");
            merchantStatement.executeUpdate();
        }
        account(connection, debit, merchant, "TEST_DEBIT_" + journal, "Test debit");
        account(connection, credit, merchant, "TEST_CREDIT_" + journal, "Test credit");
        try (PreparedStatement statement = connection.prepareStatement("insert into ledger_journals(id,transaction_reference,merchant_id,event_type,currency) values(?,?,?,?,?)")) {
            statement.setObject(1, journal);
            statement.setString(2, referencePrefix + ":" + journal);
            statement.setObject(3, merchant);
            statement.setString(4, "test.ledger");
            statement.setString(5, "USD");
            statement.executeUpdate();
        }
        return new Fixture(journal, debit, credit);
    }

    private static void account(Connection connection, UUID id, UUID merchant, String code, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into ledger_accounts(id,merchant_id,code,name,account_type,currency) values(?,?,?,?,?,?)")) {
            statement.setObject(1, id);
            statement.setObject(2, merchant);
            statement.setString(3, code);
            statement.setString(4, name);
            statement.setString(5, "LIABILITY");
            statement.setString(6, "USD");
            statement.executeUpdate();
        }
    }

    private static void entry(Connection connection, UUID journal, UUID account, long debit, long credit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into ledger_entries(journal_id,account_id,debit_minor,credit_minor,description) values(?,?,?,?,?)")) {
            statement.setObject(1, journal);
            statement.setObject(2, account);
            statement.setLong(3, debit);
            statement.setLong(4, credit);
            statement.setString(5, "Ledger invariant test");
            statement.executeUpdate();
        }
    }

    private record Fixture(UUID journal, UUID debitAccount, UUID creditAccount) { }
}
