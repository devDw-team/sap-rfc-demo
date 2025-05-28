package com.test.sap.sap_rfc_demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.repository.AutoMailDataRepository;
import com.test.sap.sap_rfc_demo.util.HtmlTemplateUtil;
import com.test.sap.sap_rfc_demo.util.ExcelTemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 파일 생성 서비스
 * filecreate-guide.md Step 1~3 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileCreationService {

    private final AutoMailDataRepository autoMailDataRepository;
    private final ObjectMapper objectMapper;
    private final ExcelTemplateUtil excelTemplateUtil;

    @Value("${app.file.base-path:src/main/resources/static}")
    private String basePath;

    @Value("${app.file.html-template:src/main/resources/static/html/bill-template.html}")
    private String htmlTemplatePath;

    @Value("${app.file.excel-template:src/main/resources/static/excel/billinfo_template.xlsx}")
    private String excelTemplatePath;

    /**
     * Step 1: 파일 생성을 위한 데이터 조회
     * 조건: SEND_AUTO = 'Y', FILE_CREATE_FLAG = 'N', 현재 년월과 동일
     */
    public List<AutoMailData> getFileCreationTargets() {
        log.info("파일 생성 대상 데이터 조회 시작");
        
        String currentYearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        List<AutoMailData> targets = autoMailDataRepository.findFileCreationTargets(currentYearMonth);
        
        log.info("파일 생성 대상 조회 완료: {}건", targets.size());
        return targets;
    }

    /**
     * Step 2: HTML, Excel 파일 생성 프로세스
     * 개별 데이터에 대해 HTML 및 Excel 파일 생성
     */
    @Transactional
    public void createFilesForData(AutoMailData data) {
        try {
            log.info("파일 생성 시작 - SEQ: {}, 사업자번호: {}", data.getSeq(), maskBusinessNumber(data.getStcd2()));
            
            // JSON 데이터 파싱 및 검증
            Map<String, Object> mailData = parseAndValidateMailData(data.getMailData());
            
            // 사업자번호와 청구년월 추출
            String businessNumber = extractBusinessNumber(mailData);
            String billingYearMonth = extractBillingYearMonth(mailData);
            
            // 타임스탬프 생성
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            
            // HTML 파일 생성
            FileCreationResult htmlResult = createHtmlFile(mailData, businessNumber, billingYearMonth, timestamp);
            
            // Excel 파일 생성
            FileCreationResult excelResult = createExcelFile(mailData, businessNumber, billingYearMonth, timestamp);
            
            // Step 3: DB 업데이트
            updateFileCreationResult(data, htmlResult, excelResult);
            
            log.info("파일 생성 완료 - SEQ: {}", data.getSeq());
            
        } catch (Exception e) {
            log.error("파일 생성 실패 - SEQ: {}, 오류: {}", data.getSeq(), e.getMessage(), e);
            updateFileCreationError(data, e.getMessage());
            throw new RuntimeException("파일 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 데이터 파싱 및 필수 필드 검증
     */
    private Map<String, Object> parseAndValidateMailData(String mailDataJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mailData = objectMapper.readValue(mailDataJson, Map.class);
            
            // 필수 필드 검증
            validateRequiredFields(mailData);
            
            return mailData;
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 필수 필드 검증
     */
    private void validateRequiredFields(Map<String, Object> mailData) {
        // customer 필드 검증
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        if (customer == null) {
            throw new IllegalArgumentException("customer 정보가 없습니다");
        }
        
        String stcd2 = (String) customer.get("stcd2");
        if (stcd2 == null || !stcd2.matches("\\d{10}")) {
            throw new IllegalArgumentException("유효하지 않은 사업자번호: " + stcd2);
        }
        
        String custNm = (String) customer.get("custNm");
        if (custNm == null || custNm.trim().isEmpty()) {
            throw new IllegalArgumentException("고객명이 없습니다");
        }
        
        // billSummary 필드 검증
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        if (billSummary == null) {
            throw new IllegalArgumentException("billSummary 정보가 없습니다");
        }
        
        String crecpYm = (String) billSummary.get("crecpYm");
        if (crecpYm == null || !crecpYm.matches("\\d{6}")) {
            throw new IllegalArgumentException("유효하지 않은 청구년월: " + crecpYm);
        }
    }

    /**
     * 사업자번호 추출
     */
    private String extractBusinessNumber(Map<String, Object> mailData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        return (String) customer.get("stcd2");
    }

    /**
     * 청구년월 추출
     */
    private String extractBillingYearMonth(Map<String, Object> mailData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        return (String) billSummary.get("crecpYm");
    }

    /**
     * HTML 파일 생성
     */
    private FileCreationResult createHtmlFile(Map<String, Object> mailData, String businessNumber, 
                                            String billingYearMonth, String timestamp) {
        try {
            // 파일명 생성
            String year = billingYearMonth.substring(0, 4);
            String month = String.valueOf(Integer.parseInt(billingYearMonth.substring(4, 6)));
            String originalFileName = String.format("코웨이(주) %s년 %s월 대금청구서.html", year, month);
            String changedFileName = String.format("%s_%s.html", businessNumber, timestamp);
            
            // 저장 경로 생성
            String saveDir = String.format("%s/html/download/%s/%s", basePath, businessNumber, billingYearMonth);
            createDirectoryIfNotExists(saveDir);
            
            // HtmlTemplateUtil용 데이터 변환
            Map<String, Object> htmlData = convertDataForHtmlTemplate(mailData);
            
            // HTML 파일 생성 (HtmlTemplateUtil 사용) - 임시 파일명으로 생성
            String outputPath = HtmlTemplateUtil.generateHtml(htmlTemplatePath, saveDir, 
                                                            "temp", htmlData);
            
            // HtmlTemplateUtil이 동적으로 생성한 파일을 원하는 파일명으로 이동
            Path sourcePath = Paths.get(outputPath);
            Path targetPath = Paths.get(saveDir, changedFileName);
            
            // 파일이 존재하는지 확인하고 이동
            if (Files.exists(sourcePath)) {
                Files.move(sourcePath, targetPath);
                log.debug("HTML 파일 이동 완료: {} -> {}", sourcePath, targetPath);
            } else {
                // 동적 파일명으로 생성된 파일 찾기
                String dynamicFileName = String.format("코웨이(주) %s년 %s월 대금청구서.html", year, month);
                Path dynamicPath = Paths.get(saveDir, dynamicFileName);
                if (Files.exists(dynamicPath)) {
                    Files.move(dynamicPath, targetPath);
                    log.debug("HTML 파일 이동 완료 (동적파일명): {} -> {}", dynamicPath, targetPath);
                } else {
                    throw new RuntimeException("생성된 HTML 파일을 찾을 수 없습니다: " + dynamicPath);
                }
            }
            
            // 다운로드 경로 생성 (파일명 제외, 디렉토리만)
            String downloadPath = String.format("/html/download/%s/%s/", businessNumber, billingYearMonth);
            
            return FileCreationResult.builder()
                    .originalFileName(originalFileName)
                    .changedFileName(changedFileName)
                    .filePath(downloadPath)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("HTML 파일 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("HTML 파일 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Excel 파일 생성
     */
    private FileCreationResult createExcelFile(Map<String, Object> mailData, String businessNumber, 
                                             String billingYearMonth, String timestamp) {
        try {
            // 파일명 생성
            String originalFileName = "코웨이 청구 상세내역.xlsx";
            String changedFileName = String.format("%s_%s.xlsx", businessNumber, timestamp);
            
            // 저장 경로 생성
            String saveDir = String.format("%s/excel/download/%s/%s", basePath, businessNumber, billingYearMonth);
            createDirectoryIfNotExists(saveDir);
            
            // ExcelTemplateUtil용 데이터 변환
            Map<String, Object> excelData = convertDataForExcelTemplate(mailData);
            
            // Excel 파일 생성 (ExcelTemplateUtil 사용)
            String outputPath = excelTemplateUtil.generateExcelFromTemplate(excelData);
            
            if (outputPath == null) {
                throw new RuntimeException("Excel 파일 생성 실패");
            }
            
            // 생성된 파일을 목적지로 이동
            Path sourcePath = Paths.get(outputPath);
            Path targetPath = Paths.get(saveDir, changedFileName);
            Files.move(sourcePath, targetPath);
            
            // 다운로드 경로 생성 (파일명 제외, 디렉토리만)
            String downloadPath = String.format("/excel/download/%s/%s/", businessNumber, billingYearMonth);
            
            return FileCreationResult.builder()
                    .originalFileName(originalFileName)
                    .changedFileName(changedFileName)
                    .filePath(downloadPath)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Excel 파일 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Excel 파일 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 디렉토리 생성
     */
    private void createDirectoryIfNotExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.debug("디렉토리 생성: {}", dirPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("디렉토리 생성 실패: " + dirPath, e);
        }
    }

    /**
     * Step 3: 파일 생성 완료 후 결과 업데이트
     */
    @Transactional
    public void updateFileCreationResult(AutoMailData data, FileCreationResult htmlResult, 
                                       FileCreationResult excelResult) {
        data.setFileCreateFlag("Y");
        data.setOriHtmlFilenm(htmlResult.getOriginalFileName());
        data.setChgHtmlFilenm(htmlResult.getChangedFileName());
        data.setHtmlFilepath(htmlResult.getFilePath());
        data.setOriExcelFilenm(excelResult.getOriginalFileName());
        data.setChgExcelFilenm(excelResult.getChangedFileName());
        data.setExcelFilepath(excelResult.getFilePath());
        data.setFileCreateDate(LocalDateTime.now());
        data.setUpdateDate(LocalDateTime.now());
        
        autoMailDataRepository.save(data);
        log.info("파일 생성 결과 업데이트 완료 - SEQ: {}", data.getSeq());
    }

    /**
     * 파일 생성 오류 시 업데이트
     */
    @Transactional
    public void updateFileCreationError(AutoMailData data, String errorMessage) {
        data.setFileCreateFlag("E");
        data.setUpdateDate(LocalDateTime.now());
        // 에러 메시지는 별도 필드가 없으므로 로그로만 기록
        
        autoMailDataRepository.save(data);
        log.error("파일 생성 오류 업데이트 - SEQ: {}, 오류: {}", data.getSeq(), errorMessage);
    }

    /**
     * HtmlTemplateUtil용 데이터 변환
     * filecreate-guide.md의 JSON 구조를 HtmlTemplateUtil이 기대하는 구조로 변환
     */
    private Map<String, Object> convertDataForHtmlTemplate(Map<String, Object> mailData) {
        Map<String, Object> htmlData = new HashMap<>(mailData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        
        // HtmlTemplateUtil이 기대하는 필드명으로 변환
        if (customer != null) {
            htmlData.put("STCD2", customer.get("stcd2"));
            htmlData.put("CUST_NM", customer.get("custNm"));
            htmlData.put("J_1KFREPRE", customer.get("j1kfrepre"));
            htmlData.put("J_1KFTBUS", customer.get("j1kftbus"));
            htmlData.put("J_1KFTIND", customer.get("j1kftind"));
            htmlData.put("PRE_AMT", customer.get("preAmt"));
            htmlData.put("REMAIN_AMT", customer.get("remainAmt"));
            htmlData.put("PRE_MONTH", customer.get("preMonth"));
        }
        
        if (billSummary != null) {
            htmlData.put("TOTAL_AMOUNT", billSummary.get("totalAmount"));
            htmlData.put("C_RECP_YM", billSummary.get("crecpYm"));
            htmlData.put("C_DUE_DATE", billSummary.get("cdueDate"));
            htmlData.put("C_SEL_KUN_CNT", billSummary.get("cselKunCnt"));
        }
        
        // bills 리스트의 필드명도 변환
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bills = (List<Map<String, Object>>) mailData.get("bills");
        if (bills != null) {
            List<Map<String, Object>> convertedBills = new ArrayList<>();
            for (Map<String, Object> bill : bills) {
                Map<String, Object> convertedBill = new HashMap<>();
                convertedBill.put("RECP_TP_TX", bill.get("recpTpTx"));
                convertedBill.put("ORDER_NO", bill.get("orderNo"));
                convertedBill.put("VTEXT", bill.get("vtext"));
                convertedBill.put("GOODS_CD", bill.get("goodsCd"));
                convertedBill.put("INST_DT", bill.get("instDt"));
                convertedBill.put("USE_DUTY_MONTH", bill.get("useDutyMonth"));
                convertedBill.put("OWNER_DATE", bill.get("ownerDate"));
                convertedBill.put("USE_MONTH", bill.get("useMonth"));
                convertedBill.put("RECP_YM", bill.get("recpYm"));
                convertedBill.put("FIX_SUPPLY_VALUE", bill.get("fixSupplyValue"));
                convertedBill.put("FIX_VAT", bill.get("fixVat"));
                convertedBill.put("FIX_BILL_AMT", bill.get("fixBillAmt"));
                convertedBill.put("SUPPLY_VALUE", bill.get("supplyValue"));
                convertedBill.put("VAT", bill.get("vat"));
                convertedBill.put("BILL_AMT", bill.get("billAmt"));
                convertedBill.put("INST_JUSO", bill.get("instJuso"));
                convertedBill.put("GOODS_SN", bill.get("goodsSn"));
                convertedBill.put("DEPT_CD_TX", bill.get("deptCdTx"));
                convertedBill.put("DEPT_TELNR", bill.get("deptTelnr"));
                convertedBill.put("ZBIGO", bill.get("zbigo"));
                convertedBill.put("GOODS_TX", bill.get("goodsTx"));
                convertedBill.put("PRE_AMT", bill.get("preAmt"));
                convertedBill.put("REMAIN_AMT", bill.get("remainAmt"));
                convertedBill.put("PRE_MONTH", bill.get("preMonth"));
                convertedBills.add(convertedBill);
            }
            htmlData.put("bills", convertedBills);
        }
        
        // billTypeSummary도 변환
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> billTypeSummary = (List<Map<String, Object>>) mailData.get("billTypeSummary");
        if (billTypeSummary != null) {
            List<Map<String, Object>> convertedBillTypes = new ArrayList<>();
            for (Map<String, Object> billType : billTypeSummary) {
                Map<String, Object> convertedBillType = new HashMap<>();
                convertedBillType.put("SUMMARY_CNT", billType.get("summaryCnt"));
                convertedBillType.put("SUMMARY_AMOUNT", billType.get("summaryAmount"));
                convertedBillType.put("C_RECP_TP_TX", billType.get("crecpTpTx"));
                convertedBillType.put("C_RECP_TP", billType.get("crecpTp"));
                convertedBillTypes.add(convertedBillType);
            }
            htmlData.put("bill_type_summary", convertedBillTypes);
        }
        
        return htmlData;
    }
    
    /**
     * ExcelTemplateUtil용 데이터 변환
     * filecreate-guide.md의 JSON 구조를 ExcelTemplateUtil이 기대하는 구조로 변환
     */
    private Map<String, Object> convertDataForExcelTemplate(Map<String, Object> mailData) {
        Map<String, Object> excelData = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        
        // customer 정보 변환
        if (customer != null) {
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("STCD2", customer.get("stcd2"));
            customerInfo.put("CUST_NM", customer.get("custNm"));
            customerInfo.put("J_1KFREPRE", customer.get("j1kfrepre"));
            customerInfo.put("J_1KFTBUS", customer.get("j1kftbus"));
            customerInfo.put("J_1KFTIND", customer.get("j1kftind"));
            excelData.put("customer", customerInfo);
        }
        
        // bill_summary 정보 변환 (ExcelTemplateUtil은 bill_summary를 기대)
        if (billSummary != null) {
            Map<String, Object> billSummaryInfo = new HashMap<>();
            billSummaryInfo.put("TOTAL_AMOUNT", billSummary.get("totalAmount"));
            billSummaryInfo.put("C_RECP_YM", billSummary.get("crecpYm"));
            billSummaryInfo.put("C_DUE_DATE", billSummary.get("cdueDate"));
            billSummaryInfo.put("C_SEL_KUN_CNT", billSummary.get("cselKunCnt"));
            excelData.put("bill_summary", billSummaryInfo);
        }
        
        // bills 리스트 변환
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bills = (List<Map<String, Object>>) mailData.get("bills");
        if (bills != null) {
            List<Map<String, Object>> convertedBills = new ArrayList<>();
            for (Map<String, Object> bill : bills) {
                Map<String, Object> convertedBill = new HashMap<>();
                convertedBill.put("ORDER_NO", bill.get("orderNo"));
                convertedBill.put("VTEXT", bill.get("vtext"));
                convertedBill.put("GOODS_TX", bill.get("goodsTx"));
                convertedBill.put("INST_DT", bill.get("instDt"));
                convertedBill.put("USE_DUTY_MONTH", bill.get("useDutyMonth"));
                convertedBill.put("OWNER_DATE", bill.get("ownerDate"));
                convertedBill.put("USE_MONTH", bill.get("useMonth"));
                convertedBill.put("RECP_YM", bill.get("recpYm"));
                convertedBill.put("FIX_SUPPLY_VALUE", bill.get("fixSupplyValue"));
                convertedBill.put("FIX_VAT", bill.get("fixVat"));
                convertedBill.put("FIX_BILL_AMT", bill.get("fixBillAmt"));
                convertedBill.put("SUPPLY_VALUE", bill.get("supplyValue"));
                convertedBill.put("VAT", bill.get("vat"));
                convertedBill.put("BILL_AMT", bill.get("billAmt"));
                convertedBill.put("PRE_AMT", bill.get("preAmt"));
                convertedBill.put("REMAIN_AMT", bill.get("remainAmt"));
                convertedBill.put("PRE_MONTH", bill.get("preMonth"));
                convertedBill.put("INST_JUSO", bill.get("instJuso"));
                convertedBill.put("GOODS_SN", bill.get("goodsSn"));
                convertedBill.put("DEPT_CD_TX", bill.get("deptCdTx"));
                convertedBill.put("DEPT_TELNR", bill.get("deptTelnr"));
                convertedBill.put("ZBIGO", bill.get("zbigo"));
                convertedBills.add(convertedBill);
            }
            excelData.put("bills", convertedBills);
        }
        
        // remarks 정보 추가 (빈 객체로)
        Map<String, Object> remarks = new HashMap<>();
        remarks.put("J_JBIGO", "");
        excelData.put("remarks", remarks);
        
        return excelData;
    }

    /**
     * 사업자번호 마스킹 (보안)
     */
    private String maskBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.length() < 3) {
            return businessNumber;
        }
        return businessNumber.substring(0, 3) + "*******";
    }

    /**
     * 파일 생성 결과 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class FileCreationResult {
        private String originalFileName;
        private String changedFileName;
        private String filePath;
        private boolean success;
        private String errorMessage;
    }
} 