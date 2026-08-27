package com.nexamarket.report.application;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.report.entity.ReportFormat;
import com.nexamarket.report.entity.ReportJob;
import com.nexamarket.report.repository.ReportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReportService {

    private final ReportJobRepository reportJobRepository;
    private final ReportGenerationService reportGenerationService;

    public ReportService(ReportJobRepository reportJobRepository, ReportGenerationService reportGenerationService) {
        this.reportJobRepository = reportJobRepository;
        this.reportGenerationService = reportGenerationService;
    }

    public ReportJob requestSellerSales(Long sellerId, ReportFormat format) {
        ReportJob job = reportJobRepository.save(ReportJob.sellerSales(sellerId, format));
        reportGenerationService.generateAsync(job.getId());
        return job;
    }

    public ReportJob requestAdminDaily(Long adminId, ReportFormat format) {
        ReportJob job = reportJobRepository.save(ReportJob.adminDaily(adminId, format));
        reportGenerationService.generateAsync(job.getId());
        return job;
    }

    @Transactional(readOnly = true)
    public ReportJob findAccessible(UUID jobId, Long requesterId, UserRole role) {
        ReportJob job = reportJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Rapor bulunamadı."));
        if (role != UserRole.ADMIN && !job.getRequestedBy().equals(requesterId)) {
            throw new IllegalArgumentException("Bu rapora erişim yetkiniz yok.");
        }
        return job;
    }
}
