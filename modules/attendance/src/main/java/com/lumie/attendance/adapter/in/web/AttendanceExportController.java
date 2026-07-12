package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.service.AttendanceExportService;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
public class AttendanceExportController {

    private final AttendanceExportService attendanceExportService;

    @GetMapping(value = "/records/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> exportRecordsCsv() {
        AuthorizationGuard.requireOwner();
        String filename = "%s_attendance_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .body(attendanceExportService::writeCsv);
    }
}
