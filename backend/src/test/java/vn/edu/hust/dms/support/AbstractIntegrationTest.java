package vn.edu.hust.dms.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base of every integration test: one MySQL 8.4 container for the whole JVM, the real HTTP
 * server on a random port, the mutable clock as the Clock bean, and a clean database before
 * each test. Tests are deliberately not @Transactional: requests are served on other threads,
 * and later phases need committed data for their locking tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        MYSQL.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected TestBedOccupancy bedOccupancy;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void resetSharedState() {
        databaseCleaner.clean();
        clock.reset();
        bedOccupancy.clear();
        SecurityContextHolder.clearContext();
    }

    /** A fresh "browser": no cookies yet. */
    protected ApiClient api() {
        return new ApiClient(port);
    }

    /** A fresh "browser" already logged in as the given user. */
    protected ApiClient loggedIn(TestUsers.Credentials user) {
        ApiClient api = api();
        FacilityFixture.expect(200, api.loginAs(user));
        return api;
    }

    /** The shared MySQL container, for tests that need a second schema (migration tests). */
    protected static MySQLContainer mysql() {
        return MYSQL;
    }
}
