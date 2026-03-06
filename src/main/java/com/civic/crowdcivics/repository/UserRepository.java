package com.civic.crowdcivics.repository;

import com.civic.crowdcivics.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByAdminId(String adminId);

    boolean existsByPhone(String phone);
}