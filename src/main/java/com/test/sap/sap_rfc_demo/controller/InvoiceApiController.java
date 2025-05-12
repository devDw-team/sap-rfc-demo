package com.test.sap.sap_rfc_demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.sap.sap_rfc_demo.dto.InvoiceSearchRequest;
import com.test.sap.sap_rfc_demo.service.InvoiceService;
import com.test.sap.sap_rfc_demo.service.HtmlGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceApiController {

    private final InvoiceService invoiceService;
    private final HtmlGeneratorService htmlGeneratorService;

    @GetMapping("/excel_data")
    public ResponseEntity<Map<String, Object>> getExcelData(InvoiceSearchRequest request) {
        Map<String, Object> result = invoiceService.getInvoiceData(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate_html")
    public ResponseEntity<String> generateHtml(@RequestBody Map<String, Object> data) {
        try {
            // 임시 JSON 파일로 저장
            String jsonPath = "src/main/resources/static/json/coway-bill-info.json";
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(jsonPath), data);
            // HTML 파일 생성
            String resultPath = htmlGeneratorService.generateHtmlFromTemplate();
            return ResponseEntity.ok("생성된 파일: " + resultPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("HTML 파일 생성 실패: " + e.getMessage());
        }
    }
} 