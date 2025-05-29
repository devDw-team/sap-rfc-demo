package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.dto.AutoMailTargetDto;
import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.repository.AutoMailDataRepository;
import com.test.sap.sap_rfc_demo.service.AutoMailService;
import com.test.sap.sap_rfc_demo.service.BatchExecutionService;
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
    private final BatchExecutionService batchExecutionService;

    /**
     * 자동메일 관리 메인 페이지
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 오늘 생성된 데이터 통계
        List<AutoMailData> todayData = autoMailDataRepository.findTodayBatchTargets();
        List<AutoMailData> allActiveData = autoMailDataRepository.findBySendAutoAndDelFlag("Y", "N");
        List<AutoMailData> fileCreatedData = autoMailDataRepository.findByFileCreateFlagAndDelFlag("Y", "N");
        List<AutoMailData> mailSentData = autoMailDataRepository.findByMailSendFlagAndDelFlag("Y", "N");
        List<AutoMailData> mailSendTargets = autoMailDataRepository.findMailSendTargets();

        model.addAttribute("todayDataCount", todayData.size());
        model.addAttribute("totalActiveCount", allActiveData.size());
        model.addAttribute("fileCreatedCount", fileCreatedData.size());
        model.addAttribute("mailSentCount", mailSentData.size());
        model.addAttribute("mailPendingCount", mailSendTargets.size());
        
        // 전체 데이터 목록 (최신 순으로 정렬)
        List<AutoMailData> allData = autoMailDataRepository.findAllByOrderBySeqDesc();
        model.addAttribute("recentData", allData);

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
            response.put("executedAt", LocalDateTime.now().toString());
            
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
            response.put("executedAt", LocalDateTime.now().toString());
            
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
     * 자동메일 데이터 목록 조회 (페이징)
     */
    @GetMapping("/api/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAutoMailData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<AutoMailData> dataList = autoMailDataRepository.findAll(pageable).getContent();
            long totalCount = autoMailDataRepository.count();
            
            // AutoMailData 리스트를 DTO로 변환
            List<Map<String, Object>> dataDtoList = dataList.stream()
                .map(this::convertToDto)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dataDtoList);
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
     * AutoMailData를 DTO Map으로 변환하는 헬퍼 메서드
     */
    private Map<String, Object> convertToDto(AutoMailData data) {
        Map<String, Object> dataDto = new HashMap<>();
        dataDto.put("seq", data.getSeq());
        dataDto.put("formId", data.getFormId());
        dataDto.put("sendAuto", data.getSendAuto());
        dataDto.put("stcd2", data.getStcd2());
        dataDto.put("custNm", data.getCustNm());
        dataDto.put("kunnr", data.getKunnr());
        dataDto.put("zgrpno", data.getZgrpno());
        dataDto.put("orderNo", data.getOrderNo());
        dataDto.put("fxday", data.getFxday());
        dataDto.put("email", data.getEmail());
        dataDto.put("email2", data.getEmail2());
        dataDto.put("recpYm", data.getRecpYm());
        dataDto.put("mailData", data.getMailData());
        dataDto.put("fileCreateFlag", data.getFileCreateFlag());
        dataDto.put("mailSendFlag", data.getMailSendFlag());
        dataDto.put("oriHtmlFilenm", data.getOriHtmlFilenm());
        dataDto.put("chgHtmlFilenm", data.getChgHtmlFilenm());
        dataDto.put("htmlFilepath", data.getHtmlFilepath());
        dataDto.put("oriExcelFilenm", data.getOriExcelFilenm());
        dataDto.put("chgExcelFilenm", data.getChgExcelFilenm());
        dataDto.put("excelFilepath", data.getExcelFilepath());
        dataDto.put("umsCode", data.getUmsCode());
        dataDto.put("umsMsg", data.getUmsMsg());
        dataDto.put("delFlag", data.getDelFlag());
        dataDto.put("createId", data.getCreateId());
        dataDto.put("updateId", data.getUpdateId());
        
        // LocalDateTime을 String으로 변환
        dataDto.put("dtCreateDate", data.getDtCreateDate() != null ? 
            data.getDtCreateDate().toString() : null);
        dataDto.put("fileCreateDate", data.getFileCreateDate() != null ? 
            data.getFileCreateDate().toString() : null);
        dataDto.put("createDate", data.getCreateDate() != null ? 
            data.getCreateDate().toString() : null);
        dataDto.put("updateDate", data.getUpdateDate() != null ? 
            data.getUpdateDate().toString() : null);
            
        return dataDto;
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
            
            // AutoMailData를 DTO로 변환하여 LocalDateTime 직렬화 문제 해결
            Map<String, Object> dataDto = convertToDto(data);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dataDto);
            
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
            long mailSentCount = autoMailDataRepository.findByMailSendFlagAndDelFlag("Y", "N").size();
            long mailPendingCount = autoMailDataRepository.findMailSendTargets().size();
            long todayCount = autoMailDataRepository.findTodayBatchTargets().size();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCount", totalCount);
            statistics.put("activeCount", activeCount);
            statistics.put("fileCreatedCount", fileCreatedCount);
            statistics.put("mailSentCount", mailSentCount);
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

    /**
     * 배치 실행 이력 조회
     */
    @GetMapping("/api/batch/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBatchHistory() {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean hasExecutedThisMonth = batchExecutionService.hasAutoMailJobExecutedThisMonth(now);
            String recentExecutions = batchExecutionService.getRecentAutoMailJobExecutions(10);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasExecutedThisMonth", hasExecutedThisMonth);
            response.put("currentMonth", now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            response.put("recentExecutions", recentExecutions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배치 실행 이력 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "배치 실행 이력 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 파일 다운로드 (HTML 또는 EXCEL)
     */
    @GetMapping("/api/download/{seq}/{fileType}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long seq, 
            @PathVariable String fileType) {
        
        try {
            AutoMailData data = autoMailDataRepository.findById(seq)
                    .orElseThrow(() -> new RuntimeException("데이터를 찾을 수 없습니다. SEQ: " + seq));
            
            // 파일생성플래그 확인
            if (!"Y".equals(data.getFileCreateFlag())) {
                throw new RuntimeException("파일이 생성되지 않은 데이터입니다.");
            }
            
            String filePath;
            String fileName;
            String originalFileName;
            String contentType;
            
            if ("html".equalsIgnoreCase(fileType)) {
                filePath = data.getHtmlFilepath();
                fileName = data.getChgHtmlFilenm();
                originalFileName = data.getOriHtmlFilenm();
                contentType = "text/html; charset=UTF-8";
            } else if ("excel".equalsIgnoreCase(fileType)) {
                filePath = data.getExcelFilepath();
                fileName = data.getChgExcelFilenm();
                originalFileName = data.getOriExcelFilenm();
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                throw new RuntimeException("지원하지 않는 파일 타입입니다: " + fileType);
            }
            
            if (filePath == null || fileName == null) {
                throw new RuntimeException(fileType.toUpperCase() + " 파일 정보가 없습니다.");
            }
            
            // 웹 경로로 파일 URL 생성
            String webPath = filePath.startsWith("/") ? filePath : "/" + filePath;
            if (!webPath.endsWith("/")) {
                webPath += "/";
            }
            String fileUrl = "http://localhost:8080" + webPath + fileName;
            
            log.info("파일 다운로드 시도 - URL: {}, 원본파일명: {}", fileUrl, originalFileName);
            
            // HTTP 클라이언트를 사용하여 파일 내용 가져오기
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(fileUrl))
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<byte[]> response = httpClient.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("파일을 가져올 수 없습니다. HTTP Status: " + response.statusCode());
            }
            
            byte[] fileContent = response.body();
            
            // 원본 파일명이 없으면 변환 파일명 사용
            String downloadFileName = originalFileName != null ? originalFileName : fileName;
            
            // 파일 다운로드 응답 생성 (원본파일명으로 다운로드)
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(downloadFileName, java.nio.charset.StandardCharsets.UTF_8))
                    .body(fileContent);
                    
        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생. SEQ: {}, 파일타입: {}", seq, fileType, e);
            
            // 오류 시 에러 메시지 반환
            String errorMessage = "파일 다운로드 실패: " + e.getMessage();
            return ResponseEntity.badRequest()
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * HTML 파일 미리보기
     */
    @GetMapping("/api/preview/{seq}/html")
    public ResponseEntity<String> previewHtmlFile(@PathVariable Long seq) {
        try {
            AutoMailData data = autoMailDataRepository.findById(seq)
                    .orElseThrow(() -> new RuntimeException("데이터를 찾을 수 없습니다. SEQ: " + seq));
            
            // 파일생성플래그 확인
            if (!"Y".equals(data.getFileCreateFlag())) {
                return ResponseEntity.badRequest()
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body("<html><body><h3>파일이 생성되지 않은 데이터입니다.</h3></body></html>");
            }
            
            String filePath = data.getHtmlFilepath();
            String fileName = data.getChgHtmlFilenm();
            
            if (filePath == null || fileName == null) {
                return ResponseEntity.badRequest()
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body("<html><body><h3>HTML 파일 정보가 없습니다.</h3></body></html>");
            }
            
            // 웹 경로로 파일 URL 생성
            String webPath = filePath.startsWith("/") ? filePath : "/" + filePath;
            if (!webPath.endsWith("/")) {
                webPath += "/";
            }
            String fileUrl = "http://localhost:8080" + webPath + fileName;
            
            log.info("HTML 파일 미리보기 시도 - URL: {}", fileUrl);
            
            // HTTP 클라이언트를 사용하여 HTML 내용 가져오기
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(fileUrl))
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> response = httpClient.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            
            if (response.statusCode() != 200) {
                return ResponseEntity.badRequest()
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body("<html><body><h3>파일을 가져올 수 없습니다. HTTP Status: " + response.statusCode() + "</h3></body></html>");
            }
            
            String htmlContent = response.body();
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(htmlContent);
                    
        } catch (Exception e) {
            log.error("HTML 파일 미리보기 중 오류 발생. SEQ: {}", seq, e);
            
            return ResponseEntity.internalServerError()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body("<html><body><h3>HTML 파일 미리보기 실패: " + e.getMessage() + "</h3></body></html>");
        }
    }

    /**
     * 발송일(FXDAY) 업데이트
     */
    @PutMapping("/api/data/{seq}/fxday")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateFxday(
            @PathVariable Long seq,
            @RequestBody Map<String, Object> request) {
        
        try {
            // 요청 데이터 검증
            Object fxdayObj = request.get("fxday");
            if (fxdayObj == null) {
                throw new RuntimeException("발송일이 입력되지 않았습니다.");
            }
            
            String fxdayStr = fxdayObj.toString().trim();
            
            // 빈 값 체크
            if (fxdayStr.isEmpty()) {
                throw new RuntimeException("발송일을 입력해주세요.");
            }
            
            // 숫자만 입력 가능하도록 검증
            if (!fxdayStr.matches("\\d+")) {
                throw new RuntimeException("발송일은 숫자만 입력 가능합니다.");
            }
            
            // 앞의 0 제거 (01 -> 1)
            int fxdayInt = Integer.parseInt(fxdayStr);
            
            // 1~31 범위 검증
            if (fxdayInt < 1 || fxdayInt > 31) {
                throw new RuntimeException("발송일은 1부터 31까지만 입력 가능합니다.");
            }
            
            // 데이터 조회
            AutoMailData data = autoMailDataRepository.findById(seq)
                    .orElseThrow(() -> new RuntimeException("데이터를 찾을 수 없습니다. SEQ: " + seq));
            
            // FXDAY 업데이트
            Short oldFxday = data.getFxday();
            data.setFxday((short) fxdayInt);
            data.setUpdateDate(LocalDateTime.now());
            data.setUpdateId("SYSTEM"); // 실제 환경에서는 현재 로그인 사용자 ID 사용
            
            autoMailDataRepository.save(data);
            
            log.info("발송일 업데이트 완료 - SEQ: {}, 기존 FXDAY: {}, 새 FXDAY: {}", 
                     seq, oldFxday, fxdayInt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "발송일이 성공적으로 업데이트되었습니다.");
            response.put("seq", seq);
            response.put("oldFxday", oldFxday);
            response.put("newFxday", fxdayInt);
            response.put("updatedAt", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("발송일 업데이트 중 숫자 형식 오류. SEQ: {}", seq, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "올바른 숫자 형식이 아닙니다.");
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("발송일 업데이트 중 오류 발생. SEQ: {}", seq, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "발송일 업데이트 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 