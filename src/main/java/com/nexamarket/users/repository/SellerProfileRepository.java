package com.nexamarket.users.repository;

import com.nexamarket.users.entity.SellerProfile;
import com.nexamarket.users.entity.SellerProfileStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SellerProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<SellerProfile> findDetailedById(Long id);

    @EntityGraph(attributePaths = "user")
    List<SellerProfile> findAllByStatusOrderByStoreNameAsc(SellerProfileStatus status);

    boolean existsByStoreNameIgnoreCase(String storeName);
}
