package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.service.TmplService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 템플릿 생성 컨트롤러
 * 도장 이미지 조건부 처리 기능 포함
 */
@RestController
@RequestMapping("/api/template")
@Slf4j
public class TemplateController {
    
    @Autowired
    private TmplService tmplService;
    
    /**
     * 특정 사업자번호로 템플릿 생성 (도장 이미지 조건부 처리 포함)
     */
    @PostMapping("/generate/{businessNo}")
    public ResponseEntity<?> generateTemplate(@PathVariable String businessNo) {
        try {
            log.info("템플릿 생성 요청 - 사업자번호: {}", businessNo);
            
            // 사업자 존재 여부 확인
            if (!tmplService.isBusinessExists(businessNo)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("해당 사업자번호를 찾을 수 없습니다: " + businessNo));
            }
            
            // 템플릿 생성 (도장 이미지 조건부 처리 포함)
            tmplService.generateTemplateByBusinessNo(businessNo);
            
            return ResponseEntity.ok(createSuccessResponse(
                "템플릿 생성 완료", 
                "사업자번호: " + businessNo + " (도장 이미지 조건부 처리 포함)"
            ));
            
        } catch (Exception e) {
            log.error("템플릿 생성 실패 - 사업자번호: {}", businessNo, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("템플릿 생성 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 모든 사업자에 대한 템플릿 생성
     */
    @PostMapping("/generate-all")
    public ResponseEntity<?> generateAllTemplates() {
        try {
            log.info("전체 템플릿 생성 요청");
            
            tmplService.generateAllTemplates();
            
            return ResponseEntity.ok(createSuccessResponse(
                "전체 템플릿 생성 완료", 
                "모든 사업자에 대한 템플릿이 생성되었습니다 (도장 이미지 조건부 처리 포함)"
            ));
            
        } catch (Exception e) {
            log.error("전체 템플릿 생성 실패", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("전체 템플릿 생성 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 사업자번호 존재 여부 확인
     */
    @GetMapping("/check/{businessNo}")
    public ResponseEntity<?> checkBusiness(@PathVariable String businessNo) {
        try {
            boolean exists = tmplService.isBusinessExists(businessNo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("businessNo", businessNo);
            response.put("exists", exists);
            response.put("message", exists ? "사업자번호가 존재합니다" : "사업자번호를 찾을 수 없습니다");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사업자번호 확인 실패 - 사업자번호: {}", businessNo, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("사업자번호 확인 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 성공 응답 생성
     */
    private Map<String, Object> createSuccessResponse(String message, String detail) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("detail", detail);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 오류 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
} 