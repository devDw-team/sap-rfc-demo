package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.service.ExcelGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
public class ExcelGeneratorController {

    private final ExcelGeneratorService excelGeneratorService;

    @Autowired
    public ExcelGeneratorController(ExcelGeneratorService excelGeneratorService) {
        this.excelGeneratorService = excelGeneratorService;
    }

    @PostMapping("/generate_excel")
    public ResponseEntity<String> generateExcel(@RequestBody Map<String, Object> data) {
        if (data == null || !data.containsKey("customerInfo") || !data.containsKey("billInfo")) {
            return ResponseEntity.badRequest().body("필수 데이터가 누락되었습니다. (customerInfo, billInfo)");
        }
        
        boolean success = excelGeneratorService.generateExcelFromTemplate(data);
        
        if (success) {
            return ResponseEntity.ok("Excel 파일이 성공적으로 생성되었습니다.");
        } else {
            return ResponseEntity.internalServerError().body("Excel 파일 생성 중 오류가 발생했습니다.");
        }
    }
} 