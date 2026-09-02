package com.nexamarket.auth.repository;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    java.util.List<UserAccount> findByRoleAndStatus(UserRole role, UserStatus status);
}
