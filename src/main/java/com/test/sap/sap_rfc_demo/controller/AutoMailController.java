package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.dto.AutoMailTargetDto;
import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.repository.AutoMailDataRepository;
import com.test.sap.sap_rfc_demo.service.AutoMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B2B 자동메일 컨트롤러
 * automail-guide.md에 정의된 기능의 수동 실행 및 모니터링 제공
 */
@Controller
@RequestMapping("/automail")
@RequiredArgsConstructor
@Slf4j
public class AutoMailController {

    private final AutoMailService autoMailService;
    private final AutoMailDataRepository autoMailDataRepository;
    private final JobLauncher jobLauncher;
    private final Job autoMailJob;

    /**
     * 자동메일 관리 메인 페이지
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 오늘 생성된 데이터 통계
        List<AutoMailData> todayData = autoMailDataRepository.findTodayBatchTargets();
        List<AutoMailData> allActiveData = autoMailDataRepository.findBySendAutoAndDelFlag("Y", "N");
        List<AutoMailData> fileCreatedData = autoMailDataRepository.findByFileCreateFlagAndDelFlag("Y", "N");
        List<AutoMailData> mailSendTargets = autoMailDataRepository.findMailSendTargets();

        model.addAttribute("todayDataCount", todayData.size());
        model.addAttribute("totalActiveCount", allActiveData.size());
        model.addAttribute("fileCreatedCount", fileCreatedData.size());
        model.addAttribute("mailPendingCount", mailSendTargets.size());
        
        // 최근 데이터 목록 (최대 10건)
        Pageable pageable = PageRequest.of(0, 10);
        List<AutoMailData> recentData = autoMailDataRepository.findAll(pageable).getContent();
        model.addAttribute("recentData", recentData);

        return "automail/dashboard";
    }

    /**
     * Step 1: 청구서 발송 대상 조회 (수동 실행)
     */
    @GetMapping("/api/step1/targets")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAutoMailTargets() {
        log.info("Step 1: 청구서 발송 대상 조회 API 호출");
        
        try {
            List<AutoMailTargetDto> targets = autoMailService.getAutoMailTargets();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "청구서 발송 대상 조회 완료");
            response.put("totalCount", targets.size());
            response.put("validCount", targets.stream().mapToLong(t -> t.isValidForProcessing() ? 1 : 0).sum());
            response.put("targets", targets);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Step 1 실행 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Step 1 + Step 2: 전체 프로세스 수동 실행
     */
    @PostMapping("/api/execute")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> executeAutoMailProcess() {
        log.info("B2B 자동메일 프로세스 수동 실행 API 호출");
        
        try {
            autoMailService.executeAutoMailProcess();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "B2B 자동메일 프로세스 실행 완료");
            response.put("executedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("B2B 자동메일 프로세스 실행 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "프로세스 실행 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Spring Batch Job 수동 실행
     */
    @PostMapping("/api/batch/run")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runBatchJob() {
        log.info("B2B 자동메일 배치 Job 수동 실행 API 호출");
        
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString())
                    .addString("jobType", "manual")
                    .toJobParameters();

            jobLauncher.run(autoMailJob, jobParameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "배치 Job 실행 완료");
            response.put("executedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배치 Job 실행 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "배치 Job 실행 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 자동메일 데이터 목록 조회
     */
    @GetMapping("/api/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAutoMailData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<AutoMailData> data = autoMailDataRepository.findAll(pageable).getContent();
            long totalCount = autoMailDataRepository.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("totalCount", totalCount);
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("자동메일 데이터 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "데이터 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 자동메일 데이터 상세 조회
     */
    @GetMapping("/api/data/{seq}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAutoMailDataDetail(@PathVariable Long seq) {
        try {
            AutoMailData data = autoMailDataRepository.findById(seq)
                    .orElseThrow(() -> new RuntimeException("데이터를 찾을 수 없습니다. SEQ: " + seq));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("자동메일 데이터 상세 조회 중 오류 발생. SEQ: {}", seq, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "데이터 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 통계 정보 조회
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            long totalCount = autoMailDataRepository.count();
            long activeCount = autoMailDataRepository.findBySendAutoAndDelFlag("Y", "N").size();
            long fileCreatedCount = autoMailDataRepository.findByFileCreateFlagAndDelFlag("Y", "N").size();
            long mailPendingCount = autoMailDataRepository.findMailSendTargets().size();
            long todayCount = autoMailDataRepository.findTodayBatchTargets().size();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCount", totalCount);
            statistics.put("activeCount", activeCount);
            statistics.put("fileCreatedCount", fileCreatedCount);
            statistics.put("mailPendingCount", mailPendingCount);
            statistics.put("todayCount", todayCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("통계 정보 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "통계 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 