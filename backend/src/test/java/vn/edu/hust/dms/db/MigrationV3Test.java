package vn.edu.hust.dms.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.hust.dms.support.AbstractIntegrationTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V3 changes data that already exists, so it is tested on a scratch schema in the same container:
 * migrate to V2, add Phase 0 rows, migrate to V3, check the rows. Runs as root, which may create schemas.
 */
class MigrationV3Test extends AbstractIntegrationTest {

    private static final String SCHEMA = "dms_migration_v3";

    @AfterEach
    void dropScratchSchema() throws SQLException {
        try (Connection root = connect(mysql().getJdbcUrl()); Statement statement = root.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + SCHEMA);
        }
    }

    @Test
    @DisplayName("MIGRATION V3 turns AFFAIRS accounts into BUILDING_MANAGER and replaces the Phase 0 settings with the CLAUDE.md keys")
    void v3ConvertsAffairsAccountsAndReplacesSettings() throws SQLException {
        try (Connection root = connect(mysql().getJdbcUrl()); Statement statement = root.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + SCHEMA);
            statement.execute("CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        String url = scratchUrl();
        migrate(url, "2");
        try (Connection connection = connect(url); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (username, password_hash, full_name, email, role, enabled, created_at) "
                    + "VALUES ('affairs', '{noop}x', 'Hoàng Minh Đức', 'affairs@dms.example', 'AFFAIRS', TRUE, UTC_TIMESTAMP(6)), "
                    + "('student', '{noop}x', 'Nguyễn Văn An', 'student@dms.example', 'STUDENT', TRUE, UTC_TIMESTAMP(6))");
            statement.execute("UPDATE settings SET setting_value = '72' WHERE setting_key = 'offer_hours'");
            statement.execute("UPDATE settings SET setting_value = '5' WHERE setting_key = 'warning_threshold'");
        }

        migrate(url, "3");

        try (Connection connection = connect(url); Statement statement = connection.createStatement()) {
            assertThat(query(statement, "SELECT username, role FROM users ORDER BY username"))
                    .containsExactly(Map.entry("affairs", "BUILDING_MANAGER"), Map.entry("student", "STUDENT"));
            assertThat(query(statement, "SELECT setting_key, setting_value FROM settings ORDER BY setting_key"))
                    .containsExactly(
                            Map.entry("default_hold_minutes", "30"),
                            Map.entry("electricity_price_per_kwh", "3000"),
                            Map.entry("equipment_fee", "300000"),
                            Map.entry("overdue_days_block_stay_on", "30"),
                            Map.entry("repair_deadline_hours_low", "168"),
                            Map.entry("repair_deadline_hours_normal", "72"),
                            Map.entry("repair_deadline_hours_urgent", "24"),
                            Map.entry("warning_threshold", "3"),
                            Map.entry("water_fee_monthly", "40000"));
        }
    }

    private static void migrate(String url, String target) {
        Flyway.configure()
                .dataSource(url, "root", mysql().getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private static String scratchUrl() {
        String url = mysql().getJdbcUrl();
        String database = "/" + mysql().getDatabaseName();
        int at = url.indexOf(database, url.indexOf("//") + 2);
        return url.substring(0, at) + "/" + SCHEMA + url.substring(at + database.length());
    }

    private static Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, "root", mysql().getPassword());
    }

    private static Map<String, String> query(Statement statement, String sql) throws SQLException {
        Map<String, String> rows = new LinkedHashMap<>();
        try (ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getString(2));
            }
        }
        return rows;
    }
}
