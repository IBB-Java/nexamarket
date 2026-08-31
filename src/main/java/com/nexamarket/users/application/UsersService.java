package com.nexamarket.users.application;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.users.api.CreateSellerProfileRequest;
import com.nexamarket.users.api.MyProfileResponse;
import com.nexamarket.users.api.ReviewSellerProfileRequest;
import com.nexamarket.users.api.SellerProfileResponse;
import com.nexamarket.users.api.UpdateMyProfileRequest;
import com.nexamarket.users.api.UpdateUserRoleRequest;
import com.nexamarket.users.api.UpdateSellerProfileRequest;
import com.nexamarket.users.api.UpdateUserStatusRequest;
import com.nexamarket.users.entity.SellerProfile;
import com.nexamarket.users.entity.SellerProfileStatus;
import com.nexamarket.users.entity.UserProfile;
import com.nexamarket.users.repository.SellerProfileRepository;
import com.nexamarket.users.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        UserAccount user = currentUser(userId);
        return MyProfileResponse.from(user, userProfileRepository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        UserAccount user = currentUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());
        if (request.firstName() != null) {
            profile.setFirstName(trimToNull(request.firstName()));
        }
        if (request.lastName() != null) {
            profile.setLastName(trimToNull(request.lastName()));
        }
        if (request.phoneNumber() != null) {
            profile.setPhoneNumber(trimToNull(request.phoneNumber()));
        }
        return MyProfileResponse.from(user, userProfileRepository.save(profile));
    }

    @Transactional
    public SellerProfileResponse createSellerProfile(Long userId, CreateSellerProfileRequest request) {
        UserAccount user = requireSeller(userId);
        if (sellerProfileRepository.findByUserId(userId).isPresent()) {
            throw new SellerProfileConflictException("Bu kullanıcı için bir satıcı profili zaten mevcut");
        }
        String storeName = request.storeName().trim();
        if (sellerProfileRepository.existsByStoreNameIgnoreCase(storeName)) {
            throw new SellerProfileConflictException("Bu mağaza adı zaten kullanılıyor");
        }
        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .storeName(storeName)
                .description(trimToNull(request.description()))
                .commissionRate(new BigDecimal("0.1000"))
                .status(SellerProfileStatus.PENDING_APPROVAL)
                .build();
        return SellerProfileResponse.from(sellerProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getMySellerProfile(Long userId) {
        requireSeller(userId);
        return sellerProfileRepository.findByUserId(userId)
                .map(SellerProfileResponse::from)
                .orElseThrow(() -> new SellerProfileNotFoundException(userId));
    }

    @Transactional
    public SellerProfileResponse updateMySellerProfile(Long userId, UpdateSellerProfileRequest request) {
        requireSeller(userId);
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerProfileNotFoundException(userId));
        if (request.storeName() != null) {
            String storeName = request.storeName().trim();
            if (!profile.getStoreName().equalsIgnoreCase(storeName)
                    && sellerProfileRepository.existsByStoreNameIgnoreCase(storeName)) {
                throw new SellerProfileConflictException("Bu mağaza adı zaten kullanılıyor");
            }
            profile.setStoreName(storeName);
        }
        if (request.description() != null) {
            profile.setDescription(trimToNull(request.description()));
        }
        return SellerProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getActiveSellerProfile(Long sellerId) {
        SellerProfile profile = sellerProfileRepository.findDetailedById(sellerId)
                .orElseThrow(() -> new SellerProfileNotFoundException(sellerId));
        if (profile.getStatus() != SellerProfileStatus.ACTIVE) {
            throw new SellerProfileNotFoundException(sellerId);
        }
        return SellerProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public List<SellerProfileResponse> listPendingSellerProfiles() {
        return sellerProfileRepository.findAllByStatusOrderByStoreNameAsc(SellerProfileStatus.PENDING_APPROVAL).stream()
                .map(SellerProfileResponse::from)
                .toList();
    }

    @Transactional
    public SellerProfileResponse reviewSellerProfile(Long sellerId, ReviewSellerProfileRequest request) {
        if (request.status() == SellerProfileStatus.PENDING_APPROVAL) {
            throw new InvalidSellerReviewException();
        }
        SellerProfile profile = sellerProfileRepository.findDetailedById(sellerId)
                .orElseThrow(() -> new SellerProfileNotFoundException(sellerId));
        profile.setStatus(request.status());
        return SellerProfileResponse.from(profile);
    }

    @Transactional
    public MyProfileResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        if (request.status() != UserStatus.ACTIVE && request.status() != UserStatus.DISABLED) {
            throw new InvalidUserStatusChangeException();
        }
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setStatus(request.status());
        if (request.status() == UserStatus.ACTIVE) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        return MyProfileResponse.from(user, userProfileRepository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public MyProfileResponse assignOperationalRole(Long userId, UpdateUserRoleRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        boolean supportedRole = request.role() == UserRole.SELLER || request.role() == UserRole.COURIER;
        if (!supportedRole || user.getRole() != UserRole.CUSTOMER) {
            throw new InvalidUserRoleChangeException();
        }
        user.setRole(request.role());
        return MyProfileResponse.from(user, userProfileRepository.findByUserId(userId).orElse(null));
    }

    private UserAccount currentUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserAccountUnavailableException();
        }
        return user;
    }

    private UserAccount requireSeller(Long userId) {
        UserAccount user = currentUser(userId);
        if (user.getRole() != UserRole.SELLER) {
            throw new SellerAccessDeniedException();
        }
        return user;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
