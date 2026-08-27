package com.nexamarket.report.api;

import com.nexamarket.report.entity.ReportJob;
import com.nexamarket.report.entity.ReportStatus;

import java.util.UUID;

public record ReportJobResponse(UUID id, ReportStatus status, String downloadUrl, String failureReason) {
    static ReportJobResponse from(ReportJob job) {
        String downloadUrl = job.getStatus() == ReportStatus.COMPLETED ? "/api/v1/reports/" + job.getId() + "/download" : null;
        return new ReportJobResponse(job.getId(), job.getStatus(), downloadUrl, job.getFailureReason());
    }
}
