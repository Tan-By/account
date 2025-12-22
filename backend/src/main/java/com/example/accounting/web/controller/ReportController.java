package com.example.accounting.web.controller;

import com.example.accounting.service.ReportService;
import com.example.accounting.web.dto.report.BalanceSheetDto;
import com.example.accounting.web.dto.report.ProfitReportDto;
import com.example.accounting.web.dto.report.ReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/profit")
    public ResponseEntity<ProfitReportDto> profit(@Valid @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.generateProfitReport(request));
    }

    @PostMapping("/balance-sheet")
    public ResponseEntity<BalanceSheetDto> balanceSheet(@Valid @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.generateBalanceSheet(request));
    }
}


