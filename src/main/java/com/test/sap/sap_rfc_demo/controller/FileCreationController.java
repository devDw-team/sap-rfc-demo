package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.dto.FileCreationTargetDto;
import com.test.sap.sap_rfc_demo.service.FileCreationService;
import com.test.sap.sap_rfc_demo.scheduler.FileCreationScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 생성 API 컨트롤러
 * filecreate-guide.md 기능을 위한 REST API 제공
 */
@RestController
@RequestMapping("/api/file-creation")
@RequiredArgsConstructor
@Slf4j
public class FileCreationController {

    private final FileCreationService fileCreationService;
    private final FileCreationScheduler fileCreationScheduler;

    /**
     * Step 1: 파일 생성 대상 조회
     */
    @GetMapping("/targets")
    public ResponseEntity<Map<String, Object>> getFileCreationTargets() {
        try {
            log.info("파일 생성 대상 조회 API 호출");
            
            List<AutoMailData> targets = fileCreationService.getFileCreationTargets();
            
            // DTO로 변환하여 LocalDateTime 직렬화 문제 해결
            List<FileCreationTargetDto> targetDtos = targets.stream()
                    .map(FileCreationTargetDto::from)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", targetDtos.size());
            response.put("targets", targetDtos);
            response.put("message", "파일 생성 대상 조회 완료");
            
            log.info("파일 생성 대상 조회 완료: {}건", targetDtos.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 생성 대상 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "파일 생성 대상 조회 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Step 2: 개별 데이터에 대한 파일 생성
     */
    @PostMapping("/create/{seq}")
    public ResponseEntity<Map<String, Object>> createFilesForData(@PathVariable Long seq) {
        try {
            log.info("개별 파일 생성 API 호출 - SEQ: {}", seq);
            
            // 해당 SEQ의 데이터 조회 (실제로는 Repository에서 조회해야 함)
            // 여기서는 간단히 처리
            List<AutoMailData> targets = fileCreationService.getFileCreationTargets();
            AutoMailData targetData = targets.stream()
                    .filter(data -> data.getSeq().equals(seq))
                    .findFirst()
                    .orElse(null);
            
            if (targetData == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "해당 SEQ의 데이터를 찾을 수 없습니다: " + seq);
                return ResponseEntity.badRequest().body(response);
            }
            
            fileCreationService.createFilesForData(targetData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("seq", seq);
            response.put("message", "파일 생성 완료");
            
            log.info("개별 파일 생성 완료 - SEQ: {}", seq);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("개별 파일 생성 실패 - SEQ: {}, 오류: {}", seq, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("seq", seq);
            response.put("message", "파일 생성 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 전체 파일 생성 프로세스 실행
     */
    @PostMapping("/execute-all")
    public ResponseEntity<Map<String, Object>> executeAllFileCreation() {
        try {
            log.info("전체 파일 생성 프로세스 실행 API 호출");
            
            List<AutoMailData> targets = fileCreationService.getFileCreationTargets();
            int totalCount = targets.size();
            int successCount = 0;
            int failCount = 0;
            
            for (AutoMailData target : targets) {
                try {
                    fileCreationService.createFilesForData(target);
                    successCount++;
                } catch (Exception e) {
                    log.error("파일 생성 실패 - SEQ: {}, 오류: {}", target.getSeq(), e.getMessage());
                    failCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", totalCount);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("message", String.format("전체 파일 생성 완료 - 성공: %d건, 실패: %d건", successCount, failCount));
            
            log.info("전체 파일 생성 프로세스 완료 - 총: {}건, 성공: {}건, 실패: {}건", totalCount, successCount, failCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("전체 파일 생성 프로세스 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "전체 파일 생성 프로세스 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 배치 작업 수동 실행
     */
    @PostMapping("/batch/execute")
    public ResponseEntity<Map<String, Object>> executeBatchJob() {
        try {
            log.info("파일 생성 배치 작업 수동 실행 API 호출");
            
            fileCreationScheduler.executeFileCreationBatchManually();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일 생성 배치 작업 실행 완료");
            
            log.info("파일 생성 배치 작업 수동 실행 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 생성 배치 작업 수동 실행 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "파일 생성 배치 작업 실행 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 파일 생성 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getFileCreationStatistics() {
        try {
            log.info("파일 생성 통계 조회 API 호출");
            
            List<AutoMailData> targets = fileCreationService.getFileCreationTargets();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalTargets", targets.size());
            statistics.put("pendingCount", targets.size()); // 모두 대기 상태
            statistics.put("completedCount", 0); // 조회 조건상 완료된 건은 0
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("message", "파일 생성 통계 조회 완료");
            
            log.info("파일 생성 통계 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 생성 통계 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "파일 생성 통계 조회 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 사업자별 템플릿 파일 존재 여부 체크
     */
    @GetMapping("/template-status")
    public ResponseEntity<Map<String, Object>> checkTemplateStatus() {
        try {
            log.info("템플릿 파일 상태 조회 API 호출");
            
            List<AutoMailData> targets = fileCreationService.getFileCreationTargets();
            List<Map<String, Object>> templateStatus = fileCreationService.checkTemplateFiles(targets);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templateStatus", templateStatus);
            response.put("message", "템플릿 파일 상태 조회 완료");
            
            log.info("템플릿 파일 상태 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("템플릿 파일 상태 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "템플릿 파일 상태 조회 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 테스트용 샘플 데이터 생성 API
     */
    @PostMapping("/test/create-sample-data")
    public ResponseEntity<Map<String, Object>> createSampleData() {
        try {
            log.info("테스트용 샘플 데이터 생성 API 호출");
            
            // 샘플 JSON 데이터 생성
            String sampleMailData = createSampleMailDataJson();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sampleData", sampleMailData);
            response.put("message", "샘플 데이터 생성 완료");
            response.put("instruction", "이 JSON 데이터를 b2b_automail_dt 테이블의 MAILDATA 컬럼에 삽입하여 테스트하세요.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("샘플 데이터 생성 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "샘플 데이터 생성 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 샘플 MAILDATA JSON 생성
     */
    private String createSampleMailDataJson() {
        return """
        {
          "customer": {
            "stcd2": "1048118121",
            "custNm": "지에스건설(주)",
            "j1kfrepre": "허윤홍외1",
            "j1kftbus": "건설업",
            "j1kftind": "일반건축공사",
            "totalAmount": 15680,
            "preAmt": 0,
            "remainAmt": 0,
            "preMonth": "0",
            "crecpYm": "202501",
            "cdueDate": "20250131"
          },
          "billTypeSummary": [
            {
              "summaryCnt": 15680,
              "summaryAmount": 1,
              "crecpTpTx": "렌탈료"
            }
          ],
          "htmlbills": [
            {
              "recpTpTx": "렌탈료",
              "orderNo": "20BC42405078",
              "vtext": "정수기",
              "goodsTx": "CHPI-5810L",
              "instDt": "20241224",
              "useMonth": "202412",
              "recpYm": "202501",
              "supplyValue": 14254,
              "vat": 1426,
              "billAmt": 15680
            }
          ],
          "excelbills": [
            {
              "orderNo": "20BC42405078",
              "vtext": "정수기",
              "goodsTx": "CHPI-5810L",
              "useDutyMonth": "36",
              "ownerDate": "20291224",
              "useMonth": "202412",
              "recpYm": "202501",
              "fixSupplyValue": 14255,
              "fixVat": 1425,
              "fixBillAmt": 15680,
              "supplyValue": 14254,
              "vat": 1426,
              "billAmt": 15680,
              "membershipBillAmt": 0,
              "asBillAmt": 0,
              "consBillAmt": 0,
              "ovdBillAmt": 0,
              "penaltyBillAmt": 0,
              "preAmt": 0,
              "remainAmt": 0,
              "preMonth": "0",
              "instJuso": "경남 창원시 진해구 신항2로 114상가219호 (용원동)",
              "goodsSn": "10502FI224C0600029",
              "deptNm": "법인경남중부지국-C팀",
              "deptTelnr": "055-267-7881"
            }
          ]
        }
        """;
    }
} 