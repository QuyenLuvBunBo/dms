package vn.edu.hust.dms.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Empties every table except the Flyway history before each test, then re-seeds the settings
 * defaults, so tests start from the state a fresh migration produces. Everything runs on one
 * connection because SET FOREIGN_KEY_CHECKS is session-scoped.
 */
@Component
public class DatabaseCleaner {

    private static final String FLYWAY_HISTORY = "flyway_schema_history";

    private final DataSource dataSource;

    public DatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void clean() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
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
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__seed_settings.sql"));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clean the test database", e);
        }
    }
}
