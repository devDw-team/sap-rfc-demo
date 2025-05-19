package com.test.sap.sap_rfc_demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.test.sap.sap_rfc_demo.dto.EmailSendRequest;
import com.test.sap.sap_rfc_demo.service.EmailService;

@Service
public class BundleMailService {

    private static final Logger log = LoggerFactory.getLogger(BundleMailService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailService emailService;

    /**
     * 묶음번호(ZGRPNO)로 청구서 파일 생성 및 안내 메일 발송
     */
    public void sendBillMailByZgrpno(String zgrpno) throws Exception {
        log.info("[BundleMailService] 메일 발송 프로세스 시작 - ZGRPNO: {}", zgrpno);
        // 1. DB에서 메일 정보 조회 (16번 쿼리)
        String sql = "SELECT STCD2, CUST_NM, J_1KFREPRE, J_1KFTBUS, J_1KFTIND, EMAIL, EMAIL2, " +
                "(SELECT LEFT(RECP_YM, 4) FROM z_re_b2b_bill_info WHERE ZGRPNO=? GROUP BY RECP_YM) AS C_RECP_YEAR, " +
                "(SELECT RIGHT(RECP_YM, 2) FROM z_re_b2b_bill_info WHERE ZGRPNO=? GROUP BY RECP_YM) AS C_RECP_MONTH " +
                "FROM z_re_b2b_cust_info WHERE ZGRPNO=? LIMIT 1";
        Map<String, Object> mailInfo = jdbcTemplate.queryForMap(sql, zgrpno, zgrpno, zgrpno);
        log.info("[BundleMailService] 메일 정보 DB 조회 결과: {}", mailInfo);

        // 2. 청구서 파일 생성 API 호출 (/api/bundle-info/generate)
        String generateUrl = "http://localhost:8080/api/bundle-info/generate";
        Map<String, String> param = new HashMap<>();
        param.put("zgrpno", zgrpno);
        Map<String, Object> fileResult = restTemplate.postForObject(generateUrl, param, Map.class);
        log.info("[BundleMailService] 파일 생성 결과: {}", fileResult);
        String htmlFileName = (String) fileResult.get("htmlFileName");
        String htmlFilePath = (String) fileResult.get("htmlFilePath");
        // 엑셀 등 추가 파일도 필요시 추출

        // 3. 메일 템플릿 파일 읽기 및 치환
        Resource templateResource = new ClassPathResource("static/mail/webmail.html");
        String templateHtml = FileCopyUtils.copyToString(new InputStreamReader(templateResource.getInputStream(), StandardCharsets.UTF_8));
        templateHtml = templateHtml.replace("${CUST_NM}", String.valueOf(mailInfo.get("CUST_NM")))
                                   .replace("${C_RECP_YEAR}", String.valueOf(mailInfo.get("C_RECP_YEAR")))
                                   .replace("${C_RECP_MONTH}", String.valueOf(mailInfo.get("C_RECP_MONTH")));
        log.info("[BundleMailService] 메일 본문(HTML) 치환 완료");
        // ... 기타 필요한 값 치환

        // 4. 메일 발송 API 호출 (EmailService 사용)
        EmailSendRequest emailReq = new EmailSendRequest();
        emailReq.setEmail((String) mailInfo.get("EMAIL"));
        emailReq.setTitle("코웨이(주) " + mailInfo.get("C_RECP_YEAR") + "년 " + mailInfo.get("C_RECP_MONTH") + "월 대금청구서");
        emailReq.setContents(templateHtml);
        emailReq.setFromName("Coway");
        emailReq.setFromAddress("noreply@coway.com");
        emailReq.setAttName1(htmlFileName);
        emailReq.setAttPath1(htmlFilePath);
        //log.info("[BundleMailService] EmailService 메일 발송 요청: {}", emailReq);
        boolean result = emailService.sendEmail(emailReq);
        log.info("[BundleMailService] EmailService 메일 발송 결과: {}", result);
    }
} 