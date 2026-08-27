package com.nexamarket.report.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.catalog.storage.ObjectStorage;
import com.nexamarket.catalog.storage.StoredObject;
import com.nexamarket.report.application.ReportService;
import com.nexamarket.report.entity.ReportFormat;
import com.nexamarket.report.entity.ReportJob;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;
    private final ObjectStorage objectStorage;

    public ReportController(ReportService reportService, ObjectStorage objectStorage) {
        this.reportService = reportService;
        this.objectStorage = objectStorage;
    }

    @PostMapping("/sellers/me/reports/sales")
    public ReportJobResponse sellerSales(@AuthenticationPrincipal AuthPrincipal principal,
                                         @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return ReportJobResponse.from(reportService.requestSellerSales(principal.userId(), format));
    }

    @PostMapping("/admin/reports/daily")
    public ReportJobResponse adminDaily(@AuthenticationPrincipal AuthPrincipal principal,
                                        @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return ReportJobResponse.from(reportService.requestAdminDaily(principal.userId(), format));
    }

    @GetMapping("/reports/{jobId}/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID jobId) {
        ReportJob job = reportService.findAccessible(jobId, principal.userId(), principal.role());
        if (job.getObjectKey() == null) {
            return ResponseEntity.accepted().body(new byte[0]);
        }
        StoredObject object = objectStorage.get(job.getObjectKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("nexamarket-report-" + job.getId() + "." + job.getFormat().extension()).build().toString())
                .body(object.content());
    }
}
