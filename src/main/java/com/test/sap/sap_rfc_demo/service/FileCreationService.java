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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Value("${app.domain.base-url:http://localhost:8080}")
    private String baseUrl;

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
            
            // Excel 파일 먼저 생성 (HTML에서 Excel 다운로드 URL 참조를 위해)
            FileCreationResult excelResult = createExcelFile(mailData, businessNumber, billingYearMonth, timestamp);
            
            // Excel 다운로드 URL 생성하여 mailData에 추가
            String excelDownloadUrl = String.format("%s/automail/api/download/%d/excel", baseUrl, data.getSeq());
            log.info("Excel 다운로드 URL 생성: {}", excelDownloadUrl);
            mailData.put("excelDownloadUrl", excelDownloadUrl);
            mailData.put("excelOriginalFileName", excelResult.getOriginalFileName());
            
            // HTML 파일 생성 (Excel 다운로드 URL 정보 포함)
            FileCreationResult htmlResult = createHtmlFile(mailData, businessNumber, billingYearMonth, timestamp);
            
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
            // 사업자별 HTML 템플릿 파일 경로 확인
            String businessHtmlTemplatePath = String.format("%s/template/%s/%s.html", basePath, businessNumber, businessNumber);
            File templateFile = new File(businessHtmlTemplatePath);
            
            if (!templateFile.exists()) {
                log.warn("사업자별 HTML 템플릿 파일이 존재하지 않습니다: {}", businessHtmlTemplatePath);
                throw new RuntimeException("사업자 청구서 템플릿 양식 미생성 - HTML");
            }
            
            // 파일명 생성
            String year = billingYearMonth.substring(0, 4);
            String month = String.valueOf(Integer.parseInt(billingYearMonth.substring(4, 6)));
            String originalFileName = String.format("코웨이(주) %s년 %s월 대금청구서.html", year, month);
            String changedFileName = String.format("%s_%s.html", businessNumber, timestamp);
            
            // 저장 경로 생성
            String saveDir = String.format("%s/html/download/%s/%s", basePath, businessNumber, billingYearMonth);
            createDirectoryIfNotExists(saveDir);
            
            // htmlbills 데이터를 HTML 템플릿에 매핑
            Map<String, Object> htmlData = convertDataForHtmlTemplate(mailData);
            
            // HTML 파일 생성 (사업자별 템플릿 사용)
            String outputPath = HtmlTemplateUtil.generateHtml(businessHtmlTemplatePath, saveDir, 
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
            log.info("Excel 파일 생성 시작 - 사업자번호: {}, 청구년월: {}", maskBusinessNumber(businessNumber), billingYearMonth);
            
            // 사업자별 Excel 템플릿 파일 경로 확인
            String businessExcelTemplatePath = String.format("%s/template/%s/%s.xlsx", basePath, businessNumber, businessNumber);
            File templateFile = new File(businessExcelTemplatePath);
            
            log.debug("Excel 템플릿 파일 경로: {}", businessExcelTemplatePath);
            
            if (!templateFile.exists()) {
                log.warn("사업자별 Excel 템플릿 파일이 존재하지 않습니다: {}", businessExcelTemplatePath);
                throw new RuntimeException("사업자 청구서 템플릿 양식 미생성 - Excel");
            }
            log.debug("Excel 템플릿 파일 존재 확인 완료");

            // 파일명 생성
            String originalFileName = "코웨이 청구 상세내역.xlsx";
            String changedFileName = String.format("%s_%s.xlsx", businessNumber, timestamp);
            log.debug("Excel 파일명 - 원본: {}, 변경: {}", originalFileName, changedFileName);

            // 저장 경로 생성
            String saveDir = String.format("%s/excel/download/%s/%s", basePath, businessNumber, billingYearMonth);
            createDirectoryIfNotExists(saveDir);
            log.debug("Excel 저장 디렉토리: {}", saveDir);

            // excelbills 데이터를 Excel 템플릿에 매핑
            log.debug("Excel 데이터 변환 시작");
            Map<String, Object> excelData = convertDataForExcelTemplate(mailData);
            log.debug("Excel 데이터 변환 완료 - customer: {}, bills: {}", 
                     excelData.containsKey("customer"), excelData.containsKey("bills"));
            
            if (excelData.containsKey("customer")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customer = (Map<String, Object>) excelData.get("customer");
                log.debug("고객 정보 - 사업자번호: {}, 고객명: {}", 
                         customer.get("STCD2"), customer.get("CUST_NM"));
            }
            
            if (excelData.containsKey("bills")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bills = (List<Map<String, Object>>) excelData.get("bills");
                log.debug("청구 데이터 건수: {}", bills != null ? bills.size() : 0);
            }

            // Excel 파일 생성 (사업자별 템플릿 파일 사용)
            log.debug("ExcelTemplateUtil.generateExcelFromTemplate 호출 시작");
            String outputPath = excelTemplateUtil.generateExcelFromTemplate(excelData, businessExcelTemplatePath);
            log.debug("ExcelTemplateUtil.generateExcelFromTemplate 호출 완료 - 결과: {}", outputPath);

            if (outputPath == null) {
                log.error("ExcelTemplateUtil에서 null 반환 - Excel 파일 생성 실패");
                throw new RuntimeException("Excel 파일 생성 실패");
            }
            
            log.debug("Excel 파일 생성 성공: {}", outputPath);

            // 생성된 파일을 목적지로 이동
            Path sourcePath = Paths.get(outputPath);
            Path targetPath = Paths.get(saveDir, changedFileName);
            
            log.debug("파일 이동 - 원본: {}, 대상: {}", sourcePath, targetPath);
            
            if (!Files.exists(sourcePath)) {
                log.error("생성된 Excel 파일이 존재하지 않습니다: {}", sourcePath);
                throw new RuntimeException("생성된 Excel 파일을 찾을 수 없습니다: " + sourcePath);
            }
            
            Files.move(sourcePath, targetPath);
            log.debug("Excel 파일 이동 완료: {} -> {}", sourcePath, targetPath);

            // 다운로드 경로 생성 (파일명 제외, 디렉토리만)
            String downloadPath = String.format("/excel/download/%s/%s/", businessNumber, billingYearMonth);
            
            log.info("Excel 파일 생성 완료 - 사업자번호: {}, 파일: {}", 
                    maskBusinessNumber(businessNumber), changedFileName);

            return FileCreationResult.builder()
                    .originalFileName(originalFileName)
                    .changedFileName(changedFileName)
                    .filePath(downloadPath)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Excel 파일 생성 실패 - 사업자번호: {}, 오류: {}", 
                     maskBusinessNumber(businessNumber), e.getMessage(), e);
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
     * filecreate-guide.md의 JSON 구조에서 htmlbills를 HTML 템플릿에 매핑
     */
    private Map<String, Object> convertDataForHtmlTemplate(Map<String, Object> mailData) {
        Map<String, Object> htmlData = new HashMap<>(mailData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        
        // customer 정보 변환
        if (customer != null) {
            htmlData.put("STCD2", customer.get("stcd2"));
            htmlData.put("CUST_NM", customer.get("custNm"));
            htmlData.put("J_1KFREPRE", customer.get("j1kfrepre"));
            htmlData.put("J_1KFTBUS", customer.get("j1kftbus"));
            htmlData.put("J_1KFTIND", customer.get("j1kftind"));
            htmlData.put("PRE_AMT", customer.get("preAmt"));
            htmlData.put("REMAIN_AMT", customer.get("remainAmt"));
            htmlData.put("PRE_MONTH", customer.get("preMonth"));
            htmlData.put("INVOICE_NOTE", customer.get("invoiceNote"));
        }
        
        // bill_summary 정보 변환 (HtmlTemplateUtil이 기대하는 bill_summary 객체 생성)
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        if (billSummary != null) {
            Map<String, Object> billSummaryInfo = new HashMap<>();
            billSummaryInfo.put("TOTAL_AMOUNT", billSummary.get("totalAmount"));
            billSummaryInfo.put("C_RECP_YM", billSummary.get("crecpYm"));
            billSummaryInfo.put("C_DUE_DATE", billSummary.get("cdueDate"));
            billSummaryInfo.put("C_SEL_KUN_CNT", 0); // 기본값 설정
            htmlData.put("bill_summary", billSummaryInfo);
        }
        
        // htmlbills 리스트를 HTML 템플릿에 매핑 (bills로 변환)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> htmlbills = (List<Map<String, Object>>) mailData.get("htmlbills");
        if (htmlbills != null) {
            List<Map<String, Object>> convertedBills = new ArrayList<>();
            for (Map<String, Object> bill : htmlbills) {
                Map<String, Object> convertedBill = new HashMap<>();
                convertedBill.put("RECP_TP_TX", bill.get("recpTpTx"));
                convertedBill.put("ORDER_NO", bill.get("orderNo"));
                convertedBill.put("VTEXT", bill.get("vtext"));
                convertedBill.put("GOODS_TX", bill.get("goodsTx"));
                convertedBill.put("INST_DT", bill.get("instDt"));
                convertedBill.put("USE_MONTH", bill.get("useMonth"));
                convertedBill.put("RECP_YM", bill.get("recpYm"));
                convertedBill.put("SUPPLY_VALUE", bill.get("supplyValue"));
                convertedBill.put("VAT", bill.get("vat"));
                convertedBill.put("BILL_AMT", bill.get("billAmt"));
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
                convertedBillTypes.add(convertedBillType);
            }
            htmlData.put("bill_type_summary", convertedBillTypes);
        }
        
        return htmlData;
    }
    
    /**
     * ExcelTemplateUtil용 데이터 변환
     * filecreate-guide.md의 JSON 구조에서 excelbills를 Excel 템플릿에 매핑
     */
    private Map<String, Object> convertDataForExcelTemplate(Map<String, Object> mailData) {
        log.debug("Excel 데이터 변환 시작");
        Map<String, Object> excelData = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) mailData.get("customer");
        
        // customer 정보 변환
        if (customer != null) {
            log.debug("customer 정보 변환 - 사업자번호: {}, 고객명: {}", 
                     customer.get("stcd2"), customer.get("custNm"));
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("STCD2", customer.get("stcd2"));
            customerInfo.put("CUST_NM", customer.get("custNm"));
            customerInfo.put("J_1KFREPRE", customer.get("j1kfrepre"));
            customerInfo.put("J_1KFTBUS", customer.get("j1kftbus"));
            customerInfo.put("J_1KFTIND", customer.get("j1kftind"));
            customerInfo.put("INVOICE_NOTE", customer.get("invoiceNote"));
            excelData.put("customer", customerInfo);
            log.debug("customer 정보 변환 완료");
        } else {
            log.warn("customer 정보가 null입니다.");
        }
        
        // bill_summary 정보 변환 (billSummary에서 추출하여 구성)
        @SuppressWarnings("unchecked")
        Map<String, Object> billSummary = (Map<String, Object>) mailData.get("billSummary");
        if (billSummary != null) {
            log.debug("billSummary 정보 변환 - 총액: {}, 청구년월: {}", 
                     billSummary.get("totalAmount"), billSummary.get("crecpYm"));
            Map<String, Object> billSummaryInfo = new HashMap<>();
            billSummaryInfo.put("TOTAL_AMOUNT", billSummary.get("totalAmount"));
            billSummaryInfo.put("C_RECP_YM", billSummary.get("crecpYm"));
            billSummaryInfo.put("C_DUE_DATE", billSummary.get("cdueDate"));
            billSummaryInfo.put("C_SEL_KUN_CNT", 0); // 기본값 설정
            excelData.put("bill_summary", billSummaryInfo);
            log.debug("billSummary 정보 변환 완료");
        } else {
            log.warn("billSummary 정보가 null입니다.");
        }
        
        // excelbills 리스트를 Excel 템플릿에 매핑 (bills로 변환)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> excelbills = (List<Map<String, Object>>) mailData.get("excelbills");
        if (excelbills != null) {
            log.debug("excelbills 데이터 변환 시작 - 건수: {}", excelbills.size());
            List<Map<String, Object>> convertedBills = new ArrayList<>();
            for (int i = 0; i < excelbills.size(); i++) {
                Map<String, Object> bill = excelbills.get(i);
                log.debug("excelbill[{}] 변환 - 주문번호: {}, 상품명: {}", 
                         i, bill.get("orderNo"), bill.get("vtext"));
                
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
                convertedBill.put("MEMBERSHIP_BILL_AMT", bill.get("membershipBillAmt"));
                convertedBill.put("AS_BILL_AMT", bill.get("asBillAmt"));
                convertedBill.put("CONS_BILL_AMT", bill.get("consBillAmt"));
                convertedBill.put("OVD_BILL_AMT", bill.get("ovdBillAmt"));
                convertedBill.put("PENALTY_BILL_AMT", bill.get("penaltyBillAmt"));
                convertedBill.put("PRE_AMT", bill.get("preAmt"));
                convertedBill.put("REMAIN_AMT", bill.get("remainAmt"));
                convertedBill.put("PRE_MONTH", bill.get("preMonth"));
                convertedBill.put("INST_JUSO", bill.get("instJuso"));
                convertedBill.put("GOODS_SN", bill.get("goodsSn"));
                convertedBill.put("DEPT_NM", bill.get("deptNm"));
                convertedBill.put("DEPT_TELNR", bill.get("deptTelnr"));
                convertedBills.add(convertedBill);
            }
            excelData.put("bills", convertedBills);
            log.debug("excelbills 데이터 변환 완료 - 변환된 건수: {}", convertedBills.size());
        } else {
            log.warn("excelbills 정보가 null입니다.");
        }
        
        // remarks 정보 추가 (빈 객체로)
        Map<String, Object> remarks = new HashMap<>();
        remarks.put("J_JBIGO", "");
        excelData.put("remarks", remarks);
        log.debug("remarks 정보 추가 완료");
        
        log.debug("Excel 데이터 변환 완료 - customer: {}, bill_summary: {}, bills: {}, remarks: {}", 
                 excelData.containsKey("customer"), excelData.containsKey("bill_summary"), 
                 excelData.containsKey("bills"), excelData.containsKey("remarks"));
        
        return excelData;
    }

    /**
     * 사업자별 템플릿 파일 존재 여부 체크
     */
    public List<Map<String, Object>> checkTemplateFiles(List<AutoMailData> targets) {
        List<Map<String, Object>> templateStatus = new ArrayList<>();
        
        // 중복 제거를 위해 Set 사용
        Set<String> processedBusinessNumbers = new HashSet<>();
        
        for (AutoMailData target : targets) {
            try {
                // JSON 데이터 파싱하여 사업자번호 추출
                Map<String, Object> mailData = parseAndValidateMailData(target.getMailData());
                String businessNumber = extractBusinessNumber(mailData);
                
                // 이미 처리한 사업자번호는 스킵
                if (processedBusinessNumbers.contains(businessNumber)) {
                    continue;
                }
                processedBusinessNumbers.add(businessNumber);
                
                // 템플릿 파일 경로 체크
                String htmlTemplatePath = String.format("%s/template/%s/%s.html", basePath, businessNumber, businessNumber);
                String excelTemplatePath = String.format("%s/template/%s/%s.xlsx", basePath, businessNumber, businessNumber);
                
                File htmlTemplate = new File(htmlTemplatePath);
                File excelTemplate = new File(excelTemplatePath);
                
                Map<String, Object> status = new HashMap<>();
                status.put("businessNumber", maskBusinessNumber(businessNumber));
                status.put("htmlTemplateExists", htmlTemplate.exists());
                status.put("excelTemplateExists", excelTemplate.exists());
                status.put("bothTemplatesExist", htmlTemplate.exists() && excelTemplate.exists());
                
                if (!htmlTemplate.exists() || !excelTemplate.exists()) {
                    List<String> missingTemplates = new ArrayList<>();
                    if (!htmlTemplate.exists()) {
                        missingTemplates.add("HTML");
                    }
                    if (!excelTemplate.exists()) {
                        missingTemplates.add("Excel");
                    }
                    status.put("missingTemplates", missingTemplates);
                    status.put("message", "사업자 청구서 템플릿 양식 미생성 - " + String.join(", ", missingTemplates));
                } else {
                    status.put("message", "템플릿 파일 정상");
                }
                
                templateStatus.add(status);
                
            } catch (Exception e) {
                log.error("템플릿 파일 체크 중 오류 발생 - SEQ: {}, 오류: {}", target.getSeq(), e.getMessage());
                Map<String, Object> errorStatus = new HashMap<>();
                errorStatus.put("businessNumber", "오류");
                errorStatus.put("htmlTemplateExists", false);
                errorStatus.put("excelTemplateExists", false);
                errorStatus.put("bothTemplatesExist", false);
                errorStatus.put("message", "템플릿 파일 체크 오류: " + e.getMessage());
                templateStatus.add(errorStatus);
            }
        }
        
        return templateStatus;
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