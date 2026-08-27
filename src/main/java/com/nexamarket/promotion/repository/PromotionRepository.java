package com.nexamarket.promotion.repository;

import com.nexamarket.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);

    List<Promotion> findByCodeIn(Collection<String> codes);
}
