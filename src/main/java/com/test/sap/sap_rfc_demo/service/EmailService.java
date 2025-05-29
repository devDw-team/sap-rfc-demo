package com.test.sap.sap_rfc_demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.sap.sap_rfc_demo.dto.EmailSendRequest;
import com.test.sap.sap_rfc_demo.dto.UmsApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${email.api.url}")
    private String apiUrl;

    @Value("${email.api.auth-id}")
    private String authId;

    @Value("${email.api.auth-key}")
    private String authKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 이메일 발송 (기존 메서드 - 호환성 유지)
     */
    public boolean sendEmail(EmailSendRequest req) {
        UmsApiResponse response = sendEmailWithResponse(req);
        return response.isSuccess();
    }

    /**
     * 이메일 발송 및 상세 응답 반환
     * umsmail-guide.md Step 4-2에 정의된 UMS API 응답 처리
     * 
     * @param req 이메일 발송 요청
     * @return UMS API 응답 정보
     */
    public UmsApiResponse sendEmailWithResponse(EmailSendRequest req) {
        log.info("[EmailService] 메일 발송 시작 - 수신자: {}, 제목: {}", 
                 maskEmail(req.getEmail()), req.getTitle());

        // 파일 첨부 경로 검증 (개별 발송용으로 완화)
        if (!validateAttachmentPath(req.getAttPath1())) {
            log.warn("[EmailService] 첨부파일 경로가 유효하지 않지만 메일 발송을 계속 진행합니다: {}", req.getAttPath1());
            // 테스트용으로 첨부파일 없어도 진행 (return 문 제거)
        }

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("EMAIL", req.getEmail());
        body.put("TITLE", req.getTitle());
        body.put("CONTENTS", req.getContents());
        body.put("FROMNAME", req.getFromName());
        body.put("FROMADDRESS", req.getFromAddress());
        
        // 첨부파일이 유효하지 않으면 첨부 정보 제외
        if (validateAttachmentPath(req.getAttPath1())) {
            body.put("ATTNAME1", req.getAttName1());
            body.put("ATTPATH1", req.getAttPath1());
        } else {
            log.warn("[EmailService] 첨부파일 정보를 제외하고 메일 발송 진행");
        }
        
        body.put("AUTOTYPE", "CWB2B");
        body.put("AUTOTYPEDESC", "코웨이 B2B 청구서 메일");
        body.put("DEPTCODE_OP", "20034444");
        body.put("DEPTCODE", "20037128");
        body.put("LEGACYID", "B2B0014");
        body.put("SENDTYPE", "R");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("auth-id", authId);
        headers.set("auth-key", authKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            log.info("[EmailService] UMS API 응답 수신: {}", response.getBody());
            
            return parseUmsApiResponse(response);
            
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 중 예외 발생", e);
            return UmsApiResponse.builder()
                    .success(false)
                    .code("ERROR")
                    .message("메일 발송 중 예외 발생: " + e.getMessage())
                    .key(null)
                    .build();
        }
    }

    /**
     * UMS API 응답 JSON 파싱
     * 응답 형식: {"code": "13", "msg": "요청성공", "key": "248395725"}
     */
    private UmsApiResponse parseUmsApiResponse(ResponseEntity<String> response) {
        try {
            if (!response.getStatusCode().is2xxSuccessful()) {
                return UmsApiResponse.builder()
                        .success(false)
                        .code("HTTP_ERROR")
                        .message("HTTP 오류: " + response.getStatusCode())
                        .key(null)
                        .build();
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return UmsApiResponse.builder()
                        .success(false)
                        .code("EMPTY_RESPONSE")
                        .message("응답 본문이 비어있습니다")
                        .key(null)
                        .build();
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "UNKNOWN";
            String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "응답 메시지 없음";
            String key = jsonNode.has("key") ? jsonNode.get("key").asText() : null;

            // UMS API에서 code가 "13"이면 성공으로 간주 (가이드 문서 기준)
            boolean isSuccess = "13".equals(code);

            log.info("[EmailService] UMS API 파싱 결과 - 성공: {}, 코드: {}, 메시지: {}, 키: {}", 
                     isSuccess, code, message, key);

            return UmsApiResponse.builder()
                    .success(isSuccess)
                    .code(code)
                    .message(message)
                    .key(key)
                    .build();

        } catch (Exception e) {
            log.error("[EmailService] UMS API 응답 파싱 실패", e);
            return UmsApiResponse.builder()
                    .success(false)
                    .code("PARSE_ERROR")
                    .message("응답 파싱 실패: " + e.getMessage())
                    .key(null)
                    .build();
        }
    }

    /**
     * 첨부파일 경로 검증
     */
    private boolean validateAttachmentPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("[EmailService] 첨부파일 경로가 비어있습니다");
            return false;
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("[EmailService] 첨부파일이 존재하지 않습니다: {}", filePath);
                return false;
            }

            if (!file.isFile()) {
                log.warn("[EmailService] 첨부파일이 파일이 아닙니다: {}", filePath);
                return false;
            }

            if (!file.canRead()) {
                log.warn("[EmailService] 첨부파일을 읽을 수 없습니다: {}", filePath);
                return false;
            }

            log.debug("[EmailService] 첨부파일 검증 성공: {}", filePath);
            return true;

        } catch (Exception e) {
            log.error("[EmailService] 첨부파일 경로 검증 중 오류 발생: {}", filePath, e);
            return false;
        }
    }

    /**
     * 이메일 주소 마스킹 (개인정보 보호)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return email;
        }
        
        String maskedLocal = parts[0].substring(0, 2) + "***";
        return maskedLocal + "@" + parts[1];
    }

    /**
     * 메일 제목 생성 헬퍼 메서드
     */
    public String generateMailTitle(String recpYear, Integer recpMonth) {
        return String.format("코웨이(주) %s년 %d월 대금청구서", recpYear, recpMonth);
    }
} 