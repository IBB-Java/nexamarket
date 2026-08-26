package com.nexamarket.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass // Bu sınıfın tek başına bir tablo olmamasını, özelliklerinin miras alan sınıflara geçmesini sağlar.
@EntityListeners(AuditingEntityListener.class) // Tarihleri Spring'in otomatik doldurması için gerekli dinleyici.
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete ve genel durum kontrolü için (Örn: ACTIVE, DELETED, PASSIVE)
    @Column(nullable = false)
    private String status = "ACTIVE";
}
