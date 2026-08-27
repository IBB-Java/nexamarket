package com.nexamarket.report.application;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.nexamarket.catalog.storage.ObjectStorage;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.report.entity.ReportFormat;
import com.nexamarket.report.entity.ReportJob;
import com.nexamarket.report.entity.ReportType;
import com.nexamarket.report.repository.ReportJobRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.ZoneOffset;
import java.time.Instant;

/** Separate async worker so report generation never delays the API response. */
@Service
public class ReportGenerationService {

    private final ReportJobRepository reportJobRepository;
    private final SubOrderRepository subOrderRepository;
    private final ObjectStorage objectStorage;

    public ReportGenerationService(ReportJobRepository reportJobRepository, SubOrderRepository subOrderRepository,
                                   ObjectStorage objectStorage) {
        this.reportJobRepository = reportJobRepository;
        this.subOrderRepository = subOrderRepository;
        this.objectStorage = objectStorage;
    }

    @Async("reportTaskExecutor")
    @Transactional
    public void generateAsync(UUID jobId) {
        ReportJob job = reportJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != com.nexamarket.report.entity.ReportStatus.PENDING) {
            return;
        }
        try {
            List<SubOrder> rows = job.getType() == ReportType.SELLER_SALES
                    ? subOrderRepository.findBySellerIdWithOrder(job.getSellerId())
                    : dailyRows(job);
            byte[] content = job.getFormat() == ReportFormat.PDF ? createPdf(job, rows) : createXlsx(job, rows);
            String objectKey = "reports/" + job.getId() + "." + job.getFormat().extension();
            objectStorage.put(objectKey, content, job.getFormat().contentType());
            job.complete(objectKey);
        } catch (RuntimeException exception) {
            job.fail(exception.getMessage());
        }
    }

    private byte[] createPdf(ReportJob job, List<SubOrder> rows) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(job.getType() == ReportType.SELLER_SALES
                    ? "NexaMarket Seller Sales Report" : "NexaMarket Admin Daily Sales Report"));
            document.add(new Paragraph("Sub-order count: " + rows.size()));
            document.add(new Paragraph("Gross sales: " + total(rows)));
            if (job.getType() == ReportType.ADMIN_DAILY) {
                document.add(new Paragraph("Commission (10%): " + commission(rows)));
                document.add(new Paragraph("Return count: " + returnCount(rows)));
            }
            for (SubOrder row : rows) {
                document.add(new Paragraph(row.getId() + " | seller=" + row.getSellerId()
                        + " | " + row.getStatus() + " | " + row.getSubtotal()));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("PDF raporu üretilemedi.", exception);
        }
    }

    private byte[] createXlsx(ReportJob job, List<SubOrder> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(job.getType() == ReportType.SELLER_SALES ? "Seller sales" : "Admin daily");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Sub-order id");
            header.createCell(1).setCellValue("Seller id");
            header.createCell(2).setCellValue("Status");
            header.createCell(3).setCellValue("Subtotal");
            for (int index = 0; index < rows.size(); index++) {
                SubOrder row = rows.get(index);
                var excelRow = sheet.createRow(index + 1);
                excelRow.createCell(0).setCellValue(row.getId().toString());
                excelRow.createCell(1).setCellValue(row.getSellerId());
                excelRow.createCell(2).setCellValue(row.getStatus().name());
                excelRow.createCell(3).setCellValue(row.getSubtotal().doubleValue());
            }
            if (job.getType() == ReportType.ADMIN_DAILY) {
                int summaryRow = rows.size() + 2;
                sheet.createRow(summaryRow).createCell(0).setCellValue("Gross sales");
                sheet.getRow(summaryRow).createCell(3).setCellValue(total(rows).doubleValue());
                sheet.createRow(summaryRow + 1).createCell(0).setCellValue("Commission (10%)");
                sheet.getRow(summaryRow + 1).createCell(3).setCellValue(commission(rows).doubleValue());
                sheet.createRow(summaryRow + 2).createCell(0).setCellValue("Return count");
                sheet.getRow(summaryRow + 2).createCell(3).setCellValue(returnCount(rows));
            }
            for (int column = 0; column < 4; column++) sheet.autoSizeColumn(column);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("XLSX raporu üretilemedi.", exception);
        }
    }

    private BigDecimal total(List<SubOrder> rows) {
        return rows.stream().filter(row -> row.getStatus() != OrderStatus.CANCELLED)
                .map(SubOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SubOrder> dailyRows(ReportJob job) {
        Instant startsAt = job.getRequestedAt().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        return subOrderRepository.findCreatedBetweenWithOrder(startsAt, startsAt.plusSeconds(86_400));
    }

    private BigDecimal commission(List<SubOrder> rows) {
        return total(rows).movePointLeft(1);
    }

    private long returnCount(List<SubOrder> rows) {
        return rows.stream().filter(row -> row.getStatus() == OrderStatus.RETURN_REQUESTED
                || row.getStatus() == OrderStatus.RETURN_APPROVED).count();
    }
}
