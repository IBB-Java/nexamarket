package com.nexamarket.auth.api;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.common.integration.IdentityUserSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/identity/users")
public class IdentityInternalController {

    private final UserAccountRepository userAccountRepository;

    public IdentityInternalController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public List<IdentityUserSnapshot> findUsers(@RequestParam List<Long> ids) {
        return userAccountRepository.findAllById(ids).stream().map(this::snapshot).toList();
    }

    @GetMapping("/{userId}")
    public IdentityUserSnapshot findUser(@PathVariable Long userId) {
        return userAccountRepository.findById(userId).map(this::snapshot)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    @GetMapping("/couriers")
    public List<Long> findActiveCourierIds() {
        return userAccountRepository.findByRoleAndStatus(UserRole.COURIER, UserStatus.ACTIVE).stream()
                .filter(UserAccount::isEmailVerified)
                .map(UserAccount::getId)
                .toList();
    }

    private IdentityUserSnapshot snapshot(UserAccount user) {
        return new IdentityUserSnapshot(user.getId(), user.getEmail(), user.getRole().name(), user.getStatus().name());
    }
}
