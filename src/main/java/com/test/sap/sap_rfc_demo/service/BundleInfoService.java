package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.repository.BundleInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import com.test.sap.sap_rfc_demo.util.HtmlTemplateUtil;
import com.test.sap.sap_rfc_demo.util.ExcelTemplateUtil;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class BundleInfoService {
    private final BundleInfoRepository bundleInfoRepository;

    public Map<String, Object> getBundleInfo(String zgrpno) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", bundleInfoRepository.findCustomer(zgrpno));
        result.put("bills", bundleInfoRepository.findBills(zgrpno));
        result.put("bill_summary", bundleInfoRepository.findBillSummary(zgrpno));
        result.put("bill_type_summary", bundleInfoRepository.findBillTypeSummary(zgrpno));
        result.put("remarks", bundleInfoRepository.findRemarks(zgrpno));
        return result;
    }

    // 파일 생성 및 경로 반환
    public Map<String, String> generateFiles(Map<String, Object> data) {
        Map<String, String> result = new HashMap<>();
        try {
            // 1. 사업자명, 청구월 추출
            String custNm = (String) ((Map)data.get("customer")).get("CUST_NM");
            String cRecpYm = (String) ((Map)data.get("bill_summary")).get("C_RECP_YM");
            String fileBase = custNm + " " + cRecpYm + " 대금청구서";
            // 파일명에서 특수문자/공백 처리
            fileBase = fileBase.replaceAll("[\\\\/:*?\"<>|]", "_");

            // 2. HTML 생성
            String htmlPath = HtmlTemplateUtil.generateHtml(
                "src/main/resources/static/html/Coway-Bill-Info-template.html",
                "src/main/resources/static/html/download",
                fileBase,
                data
            );
            result.put("html", htmlPath.replace("src/main/resources/static", ""));

            // 3. Excel 생성
            String excelPath = new ExcelTemplateUtil().generateExcelFromTemplate(data);
            result.put("excel", excelPath != null ? excelPath.replace("src/main/resources/static", "") : null);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
} 