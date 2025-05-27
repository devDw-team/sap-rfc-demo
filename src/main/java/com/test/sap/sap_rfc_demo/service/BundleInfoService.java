package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.repository.BundleInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import com.test.sap.sap_rfc_demo.util.HtmlTemplateUtil;
import com.test.sap.sap_rfc_demo.util.ExcelTemplateUtil;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BundleInfoService {
    private final BundleInfoRepository bundleInfoRepository;

    public Map<String, Object> getBundleInfo(String zgrpno) {
        // 요청된 순서대로 데이터를 LinkedHashMap에 저장
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", bundleInfoRepository.findCustomer(zgrpno));
        result.put("bill_summary", bundleInfoRepository.findBillSummary(zgrpno));
        result.put("bill_type_summary", bundleInfoRepository.findBillTypeSummary(zgrpno));
        result.put("bills", bundleInfoRepository.findBills(zgrpno));
        result.put("remarks", bundleInfoRepository.findRemarks(zgrpno));
        
        // JSON 파일로 저장
        try {
            // JSON 폴더 생성 (존재하지 않는 경우)
            String jsonFolderPath = "src/main/resources/static/json";
            File jsonFolder = new File(jsonFolderPath);
            if (!jsonFolder.exists()) {
                jsonFolder.mkdirs();
                System.out.println("JSON 폴더가 생성되었습니다: " + jsonFolderPath);
            }
            
            // 파일명 생성 (zgrpno + 현재시간)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("bundle_info_%s_%s.json", zgrpno, timestamp);
            String filePath = jsonFolderPath + "/" + fileName;
            
            // JSON 변환 및 파일 저장
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 보기 좋게 들여쓰기
            String jsonOutput = objectMapper.writeValueAsString(result);
            
            // 파일에 JSON 데이터 쓰기
            try (FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(jsonOutput);
            }
            
            System.out.println("JSON 파일이 저장되었습니다: " + filePath);
            
        } catch (IOException e) {
            System.err.println("JSON 파일 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("JSON 변환 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    // 파일 생성 및 경로 반환
    public Map<String, String> generateFiles(Map<String, Object> data) {
        Map<String, String> result = new HashMap<>();
        try {
            // 1. 사업자명, 청구월 추출
            String custNm = (String) ((Map)data.get("customer")).get("CUST_NM");
            String custNo = (String) ((Map)data.get("customer")).get("STCD2");
            String cRecpYm = (String) ((Map)data.get("bill_summary")).get("C_RECP_YM");
            String year = cRecpYm != null && cRecpYm.length() >= 6 ? cRecpYm.substring(0, 4) : "";
            String month = cRecpYm != null && cRecpYm.length() >= 6 ? cRecpYm.substring(4, 6) : "";
            String fileBase = "코웨이(주) " + year + "년 " + month + "월 대금청구서";
            fileBase = fileBase.replaceAll("[\\/:*?\"<>|]", "_");

            // 3. Excel 생성 (필요시)
            String excelPath = new ExcelTemplateUtil().generateExcelFromTemplate(data);
            if (excelPath != null) {
                result.put("excel", excelPath.replace("src/main/resources/static", ""));

                // data Map에도 추가
                data.put("excelDownloadUrl", "http://www.digitalworks.co.kr/coway/" + custNo+".xlsx");
            }

            // 2. HTML 생성
            String htmlPath = HtmlTemplateUtil.generateHtml(
                "src/main/resources/static/html/bill-template.html",
                "src/main/resources/static/html/download",
                fileBase,
                data
            );
            String htmlFileName = fileBase + ".html";
            String htmlRealFileName = custNo + ".html";
            //String htmlFilePath = htmlPath.replace("src/main/resources/static", "");
            String htmlFilePath = "http://www.digitalworks.co.kr/coway/" + htmlRealFileName;
            result.put("htmlFileName", htmlFileName);
            result.put("htmlFilePath", htmlFilePath);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
} 