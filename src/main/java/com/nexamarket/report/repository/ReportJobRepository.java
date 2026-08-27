package com.nexamarket.report.repository;

import com.nexamarket.report.entity.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportJobRepository extends JpaRepository<ReportJob, UUID> {
}
