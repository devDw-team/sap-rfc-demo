package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.EmailSendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public boolean sendEmail(EmailSendRequest req) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("EMAIL", req.getEmail());
        body.put("TITLE", req.getTitle());
        body.put("CONTENTS", req.getContents());
        body.put("FROMNAME", req.getFromName());
        body.put("FROMADDRESS", req.getFromAddress());
        body.put("ATTNAME1", req.getAttName1());
        body.put("ATTPATH1", req.getAttPath1());
        body.put("AUTOTYPE", "CWB2B");
        body.put("AUTOTYPEDESC", "코웨이 B2B 청구서 메일");
        body.put("DEPTCODE_OP", "20034444");
        body.put("DEPTCODE", "20037128");
        body.put("LEGACYID", "B2B0014");
        body.put("SENDTYPE", "R");

        //log.info("[EmailService] 메일 발송 요청: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("auth-id", authId);
        headers.set("auth-key", authKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            log.info("[EmailService] 메일 발송 응답: {}", response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 중 예외 발생", e);
            return false;
        }
    }
} 