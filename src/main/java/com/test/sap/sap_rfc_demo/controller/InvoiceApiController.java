package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.dto.InvoiceSearchRequest;
import com.test.sap.sap_rfc_demo.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceApiController {

    private final InvoiceService invoiceService;

    @GetMapping("/excel_data")
    public ResponseEntity<Map<String, Object>> getExcelData(InvoiceSearchRequest request) {
        Map<String, Object> result = invoiceService.getInvoiceData(request);
        return ResponseEntity.ok(result);
    }
} 