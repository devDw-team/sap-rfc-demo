package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.service.MailSendService;
import com.test.sap.sap_rfc_demo.scheduler.MailSendJobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메일 발송 컨트롤러 (Step 4)
 * umsmail-guide.md Step 4 기능을 대시보드에서 테스트할 수 있는 API 제공
 */
@RestController
@RequestMapping("/api/mail-send")
@RequiredArgsConstructor
@Slf4j
public class MailSendController {

    private final MailSendService mailSendService;
    private final MailSendJobScheduler mailSendJobScheduler;

    /**
     * Step 4-1: 메일 발송 대상 조회 (대시보드 표시용)
     * 모든 발송 가능한 데이터를 조회하여 대시보드에 표시
     */
    @GetMapping("/targets")
    public ResponseEntity<Map<String, Object>> getMailSendTargets() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 발송 대상 조회 API 호출 (대시보드 표시용)");
            
            // 대시보드 표시용: 모든 발송 가능한 데이터 조회 (FXDAY 조건 없음)
            List<AutoMailData> targets = mailSendService.getAllMailSendTargets();
            
            // 디버깅을 위한 추가 정보 로그
            log.info("=== 메일 발송 대상 조회 디버깅 정보 ===");
            log.info("조회된 대상 건수: {}", targets.size());
            
            if (targets.isEmpty()) {
                // 데이터가 없는 경우 현재 상태 확인을 위한 정보 조회
                log.info("메일 발송 대상이 없습니다. 데이터 상태를 확인합니다...");
                
                response.put("debugInfo", Map.of(
                    "message", "SEND_AUTO='Y', FILE_CREATE_FLAG='Y' 조건에 맞는 데이터가 없습니다",
                    "suggestion", "테스트용 샘플 데이터를 먼저 생성해보세요"
                ));
            } else {
                // 발송 상태별 통계 추가
                long sentCount = targets.stream().filter(t -> "Y".equals(t.getMailSendFlag())).count();
                long pendingCount = targets.stream().filter(t -> "N".equals(t.getMailSendFlag())).count();
                
                response.put("statistics", Map.of(
                    "totalCount", targets.size(),
                    "sentCount", sentCount,
                    "pendingCount", pendingCount,
                    "currentDay", java.time.LocalDate.now().getDayOfMonth()
                ));
            }
            
            response.put("success", true);
            response.put("message", "메일 발송 대상 조회 완료");
            response.put("totalCount", targets.size());
            response.put("targets", targets);
            
            log.info("메일 발송 대상 조회 성공: {}건", targets.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메일 발송 대상 조회 실패", e);
            
            response.put("success", false);
            response.put("message", "메일 발송 대상 조회 실패: " + e.getMessage());
            response.put("totalCount", 0);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4-2: 특정 SEQ에 대한 개별 메일 발송 (수동 발송)
     * SEQ 조건으로 해당 건만 조회하여 발송 (FXDAY 조건 없음)
     */
    @PostMapping("/send/{seq}")
    public ResponseEntity<Map<String, Object>> sendMailBySeq(@PathVariable Long seq) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("개별 메일 발송 API 호출 - SEQ: {}", seq);
            
            // 개별 발송용: 특정 SEQ의 데이터만 조회 (FXDAY 조건 없음)
            AutoMailData target = mailSendService.getMailSendTargetBySeq(seq);
            
            if (target == null) {
                response.put("success", false);
                response.put("message", "메일 발송 대상을 찾을 수 없습니다. SEQ: " + seq + 
                           " (SEND_AUTO='Y', FILE_CREATE_FLAG='Y' 조건을 확인하세요)");
                return ResponseEntity.status(404).body(response);
            }
            
            // 개별 메일 발송 처리 (수동 발송용 메서드 사용)
            mailSendService.processIndividualMailSending(target);
            
            response.put("success", true);
            response.put("message", "개별 메일 발송 완료 (수동 발송)");
            response.put("seq", seq);
            response.put("customerName", target.getCustNm());
            response.put("fxday", target.getFxday());
            response.put("currentDay", java.time.LocalDate.now().getDayOfMonth());
            
            log.info("개별 메일 발송 성공 - SEQ: {}, 고객: {}, FXDAY: {}", 
                     seq, target.getCustNm(), target.getFxday());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("개별 메일 발송 실패 - SEQ: {}", seq, e);
            
            response.put("success", false);
            response.put("message", "개별 메일 발송 실패: " + e.getMessage());
            response.put("seq", seq);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4: 전체 메일 발송 프로세스 실행 (FXDAY 조건 포함)
     * 오늘이 발송일인 고객들에게만 발송
     */
    @PostMapping("/execute-all")
    public ResponseEntity<Map<String, Object>> executeAllMailSending() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("전체 메일 발송 프로세스 API 호출 (FXDAY 조건 포함)");
            
            // 전체 발송용: FXDAY 조건 포함하여 오늘이 발송일인 대상만 조회
            List<AutoMailData> targets = mailSendService.getMailSendTargets();
            
            if (targets.isEmpty()) {
                response.put("success", true);
                response.put("message", "오늘(" + java.time.LocalDate.now().getDayOfMonth() + 
                           "일) 발송 대상이 없습니다");
                response.put("totalCount", 0);
                response.put("processedCount", 0);
                response.put("currentDay", java.time.LocalDate.now().getDayOfMonth());
                return ResponseEntity.ok(response);
            }
            
            // Step 4-2: 전체 메일 발송 처리
            mailSendService.processMailSending(targets);
            
            response.put("success", true);
            response.put("message", "전체 메일 발송 프로세스 완료 (FXDAY 조건 적용)");
            response.put("totalCount", targets.size());
            response.put("processedCount", targets.size());
            response.put("currentDay", java.time.LocalDate.now().getDayOfMonth());
            
            log.info("전체 메일 발송 프로세스 성공: {}건 처리 (오늘: {}일)", 
                     targets.size(), java.time.LocalDate.now().getDayOfMonth());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("전체 메일 발송 프로세스 실패", e);
            
            response.put("success", false);
            response.put("message", "전체 메일 발송 프로세스 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4 배치 작업 실행 (Step 4-3)
     */
    @PostMapping("/batch/execute")
    public ResponseEntity<Map<String, Object>> executeBatchMailSending() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Step 4 배치 작업 API 호출");
            
            mailSendService.executeStep4();
            
            response.put("success", true);
            response.put("message", "Step 4 배치 작업 실행 완료");
            response.put("executedAt", java.time.LocalDateTime.now().toString());
            
            log.info("Step 4 배치 작업 실행 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Step 4 배치 작업 실행 실패", e);
            
            response.put("success", false);
            response.put("message", "Step 4 배치 작업 실행 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 메일 발송 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMailSendStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 발송 통계 조회 API 호출");
            
            // MailSendService에서 통계 정보 조회
            Map<String, Object> statistics = mailSendService.getMailSendStatistics();
            
            response.put("success", true);
            response.put("message", "메일 발송 통계 조회 완료");
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메일 발송 통계 조회 실패", e);
            
            response.put("success", false);
            response.put("message", "메일 발송 통계 조회 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 메일 템플릿 미리보기
     */
    @GetMapping("/preview-template")
    public ResponseEntity<Map<String, Object>> previewMailTemplate(
            @RequestParam(defaultValue = "테스트고객") String custNm,
            @RequestParam(defaultValue = "2024") String recpYear,
            @RequestParam(defaultValue = "12") Integer recpMonth) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 템플릿 미리보기 API 호출 - 고객명: {}, 년월: {}년 {}월", 
                     custNm, recpYear, recpMonth);
            
            // TemplateService를 통한 HTML 생성 (실제 구현 시 필요)
            // 여기서는 간단한 응답만 제공
            response.put("success", true);
            response.put("message", "메일 템플릿 미리보기 생성 완료");
            response.put("custNm", custNm);
            response.put("recpYear", recpYear);
            response.put("recpMonth", recpMonth);
            response.put("templateVariables", Map.of(
                "CUST_NM", custNm,
                "C_RECP_YEAR", recpYear,
                "C_RECP_MONTH", recpMonth
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메일 템플릿 미리보기 실패", e);
            
            response.put("success", false);
            response.put("message", "메일 템플릿 미리보기 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 메일 발송 결과 조회 (Dashboard 용)
     * umsmail-guide.md Step 4-4에 정의된 메일 발송 결과 확인 기능
     */
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getMailSendResults(
            @RequestParam(required = false) String targetDate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 발송 결과 조회 API 호출 - 날짜: {}", targetDate);
            
            List<Map<String, Object>> results;
            if (targetDate != null && !targetDate.trim().isEmpty()) {
                results = mailSendService.getMailSendResultsByDate(targetDate);
            } else {
                results = mailSendService.getMailSendResults();
            }
            
            response.put("success", true);
            response.put("message", "메일 발송 결과 조회 완료");
            response.put("results", results);
            response.put("totalCount", results.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메일 발송 결과 조회 실패", e);
            
            response.put("success", false);
            response.put("message", "메일 발송 결과 조회 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4: 메일 발송 배치 스케줄러 수동 실행
     * 매일 8시에 자동 실행되는 스케줄러를 수동으로 강제 실행
     */
    @PostMapping("/scheduler/execute")
    public ResponseEntity<Map<String, Object>> executeMailSendScheduler() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 발송 배치 스케줄러 수동 실행 API 호출");
            
            mailSendJobScheduler.executeMailSendBatchManually();
            
            response.put("success", true);
            response.put("message", "메일 발송 배치 스케줄러 수동 실행 완료");
            response.put("executedAt", java.time.LocalDateTime.now().toString());
            
            log.info("메일 발송 배치 스케줄러 수동 실행 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메일 발송 배치 스케줄러 수동 실행 실패", e);
            
            response.put("success", false);
            response.put("message", "메일 발송 배치 스케줄러 수동 실행 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4: 특정 일자(FXDAY)에 대한 메일 발송 배치 실행
     * 특정 고정일에 해당하는 고객들에게 강제로 메일 발송
     */
    @PostMapping("/scheduler/execute-for-day/{targetDay}")
    public ResponseEntity<Map<String, Object>> executeMailSendForSpecificDay(
            @PathVariable int targetDay) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("특정 일자({}) 메일 발송 배치 스케줄러 실행 API 호출", targetDay);
            
            // 일자 유효성 검사
            if (targetDay < 1 || targetDay > 31) {
                response.put("success", false);
                response.put("message", "유효하지 않은 일자입니다. 1~31 사이의 값을 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
            mailSendJobScheduler.executeMailSendForSpecificDay(targetDay);
            
            response.put("success", true);
            response.put("message", "특정 일자(" + targetDay + ") 메일 발송 배치 실행 완료");
            response.put("targetDay", targetDay);
            response.put("executedAt", java.time.LocalDateTime.now().toString());
            
            log.info("특정 일자({}) 메일 발송 배치 스케줄러 실행 성공", targetDay);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("특정 일자({}) 메일 발송 배치 스케줄러 실행 실패", targetDay, e);
            
            response.put("success", false);
            response.put("message", "특정 일자(" + targetDay + ") 메일 발송 배치 실행 실패: " + e.getMessage());
            response.put("targetDay", targetDay);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Step 4: 배치 스케줄러 상태 조회
     * 스케줄러 관련 정보 및 다음 실행 예정 시간 등을 조회
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("메일 발송 배치 스케줄러 상태 조회 API 호출");
            
            // 현재 시간 기준으로 다음 실행 시간 계산
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime nextExecution = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
            
            // 이미 오늘 8시가 지났다면 내일 8시로 설정
            if (now.getHour() >= 8) {
                nextExecution = nextExecution.plusDays(1);
            }
            
            Map<String, Object> schedulerInfo = new HashMap<>();
            schedulerInfo.put("scheduleExpression", "0 0 8 * * ? (매일 오전 8시)");
            schedulerInfo.put("nextExecutionTime", nextExecution.toString());
            schedulerInfo.put("isEnabled", true);
            schedulerInfo.put("description", "FXDAY가 현재 일자와 일치하는 고객에게 메일 발송");
            
            response.put("success", true);
            response.put("message", "스케줄러 상태 조회 완료");
            response.put("schedulerInfo", schedulerInfo);
            response.put("currentTime", now.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스케줄러 상태 조회 실패", e);
            
            response.put("success", false);
            response.put("message", "스케줄러 상태 조회 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
} 