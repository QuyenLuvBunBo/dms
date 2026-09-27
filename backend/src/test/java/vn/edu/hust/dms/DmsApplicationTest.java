package vn.edu.hust.dms;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.MutableClock;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class DmsApplicationTest extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock injectedClock;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("INFRA context starts with Flyway V1 to V4 applied and Hibernate ddl-auto=validate passing")
    void contextStartsWithMigrationsApplied() {
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .containsExactly("1", "2", "3", "4");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settings", Long.class)).isEqualTo(9L);
        assertThat(injectedClock).as("the Clock bean seen by services is the mutable test clock")
                .isInstanceOf(MutableClock.class);
    }
}
