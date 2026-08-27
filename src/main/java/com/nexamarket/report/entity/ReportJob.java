package com.nexamarket.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_jobs")
@Getter
@NoArgsConstructor
public class ReportJob {

    @Id
    private UUID id;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private Long requestedBy;

    @Column(name = "seller_id")
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private ReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private ReportJob(Long requestedBy, Long sellerId, ReportType type, ReportFormat format) {
        this.id = UUID.randomUUID();
        this.requestedBy = requestedBy;
        this.sellerId = sellerId;
        this.type = type;
        this.format = format;
        this.status = ReportStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public static ReportJob sellerSales(Long sellerId, ReportFormat format) {
        return new ReportJob(sellerId, sellerId, ReportType.SELLER_SALES, format);
    }

    public static ReportJob adminDaily(Long adminId, ReportFormat format) {
        return new ReportJob(adminId, null, ReportType.ADMIN_DAILY, format);
    }

    public void complete(String objectKey) {
        this.objectKey = objectKey;
        this.status = ReportStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = ReportStatus.FAILED;
        this.failureReason = reason == null ? "Rapor üretilemedi." : reason.substring(0, Math.min(500, reason.length()));
        this.completedAt = Instant.now();
    }
}
