package vn.edu.hust.dms.common.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    long countByRole(Role role);

    List<UserAccount> findByRoleOrderByFullNameAsc(Role role);
}
