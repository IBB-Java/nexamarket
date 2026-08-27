package com.nexamarket.stock.application;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.stock.api.CreateStockReservationRequest;
import com.nexamarket.stock.api.StockLevelResponse;
import com.nexamarket.stock.api.StockReservationResponse;
import com.nexamarket.stock.api.UpdateStockQuantityRequest;
import com.nexamarket.stock.config.StockReservationProperties;
import com.nexamarket.stock.entity.StockReservation;
import com.nexamarket.stock.entity.StockReservationStatus;
import com.nexamarket.stock.repository.StockReservationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductVariantRepository productVariantRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockReservationProperties reservationProperties;
    private final Clock stockClock;
    private final EntityManager entityManager;
    private final StockMutationLock stockMutationLock;

    @Transactional(readOnly = true)
    public StockLevelResponse getStockLevel(Long variantId) {
        ProductVariant variant = findVariant(variantId);
        return new StockLevelResponse(variant.getId(), variant.getSku(), variant.getStockQuantity());
    }

    @Transactional
    public StockLevelResponse updateStockLevel(
            Long variantId,
            UpdateStockQuantityRequest request,
            AuthPrincipal principal) {
        ProductVariant variant = findVariant(variantId);
        ensureCanManageVariant(variant, principal);
        variant.setStockQuantity(request.stockQuantity());
        return new StockLevelResponse(variant.getId(), variant.getSku(), variant.getStockQuantity());
    }

    @Transactional
    public StockReservationResponse reserve(CreateStockReservationRequest request, AuthPrincipal principal) {
        return stockMutationLock.execute(request.variantId(), () -> reserveWithinLock(request, principal));
    }

    private StockReservationResponse reserveWithinLock(CreateStockReservationRequest request, AuthPrincipal principal) {
        ProductVariant variant = findVariant(request.variantId());

        if (productVariantRepository.decreaseStockIfAvailable(variant.getId(), request.quantity()) == 0) {
            throw new InsufficientStockException(variant.getId());
        }

        entityManager.refresh(variant);
        LocalDateTime now = now();
        StockReservation reservation = StockReservation.builder()
                .reservationCode(UUID.randomUUID().toString())
                .userId(principal.userId())
                .variant(variant)
                .quantity(request.quantity())
                .status(StockReservationStatus.ACTIVE)
                .expiresAt(now.plus(reservationProperties.getDuration()))
                .build();
        stockReservationRepository.save(reservation);
        return toResponse(reservation, variant.getStockQuantity());
    }

    /**
     * Internal cart operation. The cart owns one reservation per line item and
     * extends that reservation atomically when the customer increases quantity.
     */
    @Transactional
    public StockReservationResponse increaseReservationInternally(String reservationCode, int additionalQuantity) {
        if (additionalQuantity < 1) {
            throw new IllegalArgumentException("Additional quantity must be at least one.");
        }
        StockReservation reservation = findLockedReservation(reservationCode);
        return stockMutationLock.execute(reservation.getVariant().getId(),
                () -> increaseReservationWithinLock(reservation, additionalQuantity));
    }

    private StockReservationResponse increaseReservationWithinLock(StockReservation reservation, int additionalQuantity) {
        if (expireWhenNecessary(reservation, now())) {
            throw new InvalidReservationStateException("Süresi dolmuş rezervasyon artırılamaz");
        }
        if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Yalnızca aktif rezervasyonlar artırılabilir");
        }
        ProductVariant variant = reservation.getVariant();
        if (productVariantRepository.decreaseStockIfAvailable(variant.getId(), additionalQuantity) == 0) {
            throw new InsufficientStockException(variant.getId());
        }
        entityManager.refresh(variant);
        reservation.setQuantity(reservation.getQuantity() + additionalQuantity);
        reservation.setExpiresAt(now().plus(reservationProperties.getDuration()));
        return toResponse(reservation, variant.getStockQuantity());
    }

    /** Used by cart expiry and order timeout jobs, which run with system authority. */
    @Transactional
    public void releaseReservationInternally(String reservationCode) {
        StockReservation reservation = findLockedReservation(reservationCode);
        if (reservation.getStatus() == StockReservationStatus.ACTIVE) {
            releaseToStock(reservation, StockReservationStatus.RELEASED, now());
        }
    }

    /** Called after a successful payment so reserved stock becomes committed stock. */
    @Transactional
    public void confirmReservationInternally(String reservationCode) {
        StockReservation reservation = findLockedReservation(reservationCode);
        if (expireWhenNecessary(reservation, now())) {
            throw new InvalidReservationStateException("Süresi dolmuş rezervasyon onaylanamaz");
        }
        if (reservation.getStatus() == StockReservationStatus.ACTIVE) {
            reservation.setStatus(StockReservationStatus.CONFIRMED);
        }
    }

    @Transactional
    public StockReservationResponse confirm(String reservationCode, AuthPrincipal principal) {
        StockReservation reservation = findLockedReservation(reservationCode);
        ensureReservationOwner(reservation, principal);

        if (expireWhenNecessary(reservation, now())) {
            return toResponse(reservation, availableStock(reservation.getVariant()));
        }
        if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Yalnızca aktif rezervasyonlar onaylanabilir");
        }

        reservation.setStatus(StockReservationStatus.CONFIRMED);
        return toResponse(reservation, availableStock(reservation.getVariant()));
    }

    @Transactional
    public StockReservationResponse release(String reservationCode, AuthPrincipal principal) {
        StockReservation reservation = findLockedReservation(reservationCode);
        ensureReservationOwner(reservation, principal);

        if (expireWhenNecessary(reservation, now())) {
            return toResponse(reservation, availableStock(reservation.getVariant()));
        }
        if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Yalnızca aktif rezervasyonlar serbest bırakılabilir");
        }

        releaseToStock(reservation, StockReservationStatus.RELEASED, now());
        return toResponse(reservation, availableStock(reservation.getVariant()));
    }

    @Transactional
    public int expireReservations() {
        LocalDateTime now = now();
        List<StockReservation> expiredReservations = stockReservationRepository.findExpiredReservationsForUpdate(
                StockReservationStatus.ACTIVE, now);
        expiredReservations.forEach(reservation -> releaseToStock(reservation, StockReservationStatus.EXPIRED, now));
        return expiredReservations.size();
    }

    private boolean expireWhenNecessary(StockReservation reservation, LocalDateTime now) {
        if (reservation.getStatus() == StockReservationStatus.ACTIVE && !reservation.getExpiresAt().isAfter(now)) {
            releaseToStock(reservation, StockReservationStatus.EXPIRED, now);
            return true;
        }
        return false;
    }

    private void releaseToStock(StockReservation reservation, StockReservationStatus targetStatus, LocalDateTime now) {
        productVariantRepository.increaseStock(reservation.getVariant().getId(), reservation.getQuantity());
        entityManager.refresh(reservation.getVariant());
        reservation.setStatus(targetStatus);
        reservation.setReleasedAt(now);
    }

    private int availableStock(ProductVariant variant) {
        entityManager.refresh(variant);
        return variant.getStockQuantity();
    }

    private ProductVariant findVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new StockVariantNotFoundException(variantId));
    }

    private StockReservation findLockedReservation(String reservationCode) {
        return stockReservationRepository.findLockedByReservationCode(reservationCode)
                .orElseThrow(() -> new StockReservationNotFoundException(reservationCode));
    }

    private void ensureReservationOwner(StockReservation reservation, AuthPrincipal principal) {
        if (!reservation.getUserId().equals(principal.userId()) && principal.role() != UserRole.ADMIN) {
            throw new StockAccessDeniedException();
        }
    }

    private void ensureCanManageVariant(ProductVariant variant, AuthPrincipal principal) {
        if (principal.role() == UserRole.ADMIN) {
            return;
        }
        if (principal.role() != UserRole.SELLER || !variant.getProduct().getSellerId().equals(principal.userId())) {
            throw new StockAccessDeniedException();
        }
    }

    private StockReservationResponse toResponse(StockReservation reservation, int availableStock) {
        return new StockReservationResponse(
                reservation.getReservationCode(),
                reservation.getVariant().getId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                availableStock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(stockClock);
    }
}
