package vn.edu.hust.dms.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataServiceTest extends AbstractIntegrationTest {

    @Autowired
    private DemoDataService demoData;

    @Autowired
    private UserAccountRepository users;

    @Test
    @DisplayName("DEMO seedIfEmpty() on an empty database creates exactly one enabled account per role")
    void seedsOneAccountPerRole() {
        assertThat(demoData.seedIfEmpty()).isTrue();

        assertThat(users.count()).isEqualTo(Role.values().length);
        for (Role role : Role.values()) {
            assertThat(users.countByRole(role)).as(role.name()).isEqualTo(1);
        }
        assertThat(users.findAll()).allSatisfy(account -> {
            assertThat(account.isEnabled()).isTrue();
            assertThat(account.getPasswordHash()).startsWith("{bcrypt}");
            assertThat(account.getCreatedAt()).isEqualTo(clock.instant());
        });
        assertThat(users.findByUsername("student")).get()
                .extracting(UserAccount::getFullName, UserAccount::getRole)
                .containsExactly("Nguyễn Văn An", Role.STUDENT);
    }

    @Test
    @DisplayName("DEMO seedIfEmpty() is idempotent: a second run creates nothing")
    void secondRunIsNoOp() {
        assertThat(demoData.seedIfEmpty()).isTrue();

        assertThat(demoData.seedIfEmpty()).isFalse();

        assertThat(users.count()).isEqualTo(Role.values().length);
    }

    @Test
    @DisplayName("DEMO every seeded account can log in through POST /api/auth/login")
    void everyDemoAccountCanLogIn() {
        demoData.seedIfEmpty();

        for (DemoAccount account : DemoAccount.values()) {
            ApiResponse response = api().loginAs(account.username(), DemoAccount.PASSWORD);

            assertThat(response.status()).as(account.username()).isEqualTo(200);
            assertThat(response.body().path("role").asString()).isEqualTo(account.role().name());
            assertThat(response.body().path("fullName").asString()).isEqualTo(account.fullName());
        }
    }
}
