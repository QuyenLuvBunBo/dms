package vn.edu.hust.dms.support;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Empties every table except the Flyway history before each test, then restores the settings rows
 * the migrations seeded, so tests start from the state a fresh migration produces. Everything runs
 * on one connection because SET FOREIGN_KEY_CHECKS is session-scoped.
 */
@Component
public class DatabaseCleaner {

    private static final String FLYWAY_HISTORY = "flyway_schema_history";

    /**
     * The settings rows as Flyway left them, captured on the first clean of the JVM: the container
     * starts empty, so no test has changed them yet. Static so that a second Spring context cannot
     * capture values a test has changed. Taking a snapshot rather than re-running a migration script
     * keeps this class unaware of which migration seeds which key.
     */
    private static List<Object[]> seededSettings;

    private final DataSource dataSource;

    public DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void clean() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            List<Object[]> settings = seededSettings(connection);
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String table : tables) {
                    if (!FLYWAY_HISTORY.equalsIgnoreCase(table)) {
                        statement.execute("TRUNCATE TABLE `" + table + "`");
                    }
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            restore(connection, settings);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clean the test database", e);
        }
    }

    private static synchronized List<Object[]> seededSettings(Connection connection) throws SQLException {
        if (seededSettings == null) {
            List<Object[]> rows = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT setting_key, setting_value, description, updated_at FROM settings")) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getObject(4)});
                }
            }
            seededSettings = List.copyOf(rows);
        }
        return seededSettings;
    }

    private static void restore(Connection connection, List<Object[]> settings) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO settings (setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, ?)")) {
            for (Object[] row : settings) {
                for (int i = 0; i < row.length; i++) {
                    insert.setObject(i + 1, row[i]);
                }
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
