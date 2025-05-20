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
                "src/main/resources/static/html/Coway-Bill-Info-template.html",
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