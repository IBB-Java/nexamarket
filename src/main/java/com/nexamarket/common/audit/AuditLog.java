package com.nexamarket.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Append-only audit information; it stores no credentials or request bodies. */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLog(Long actorId, String method, String path, int httpStatus, String correlationId) {
        this.actorId = actorId;
        this.method = method;
        this.path = path;
        this.httpStatus = httpStatus;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }
}
