package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.service.SapService;
import com.test.sap.sap_rfc_demo.service.SapCustomerInfoService;
import com.test.sap.sap_rfc_demo.service.SapBillInfoService;
import com.test.sap.sap_rfc_demo.dto.CustomerInfoResponse;
import com.test.sap.sap_rfc_demo.dto.BillInfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.sap.conn.jco.JCoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class SapApiController {
    private static final Logger logger = LoggerFactory.getLogger(SapApiController.class);

    private final SapService sapService;
    private final SapCustomerInfoService customerInfoService;
    private final SapBillInfoService billInfoService;

    @Autowired
    public SapApiController(SapService sapService, 
                          SapCustomerInfoService customerInfoService,
                          SapBillInfoService billInfoService) {
        this.sapService = sapService;
        this.customerInfoService = customerInfoService;
        this.billInfoService = billInfoService;
    }

    @GetMapping("/customer-info")
    public ResponseEntity<?> getCustomerInfo(@RequestParam(defaultValue = "20240101") String erdat) {
        try {
            Map<String, Object> result = sapService.getCustomerInfo(erdat);
            logger.debug("SAP Response: {}", result);  // 응답 데이터 로깅

            // customerList 키로 데이터 확인
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> customerDataList = (List<Map<String, Object>>) result.get("customerList");
            
            // customerList가 없으면 ET_CUST_DATA 키로 한번 더 확인
            if (customerDataList == null) {
                customerDataList = (List<Map<String, Object>>) result.get("ET_CUST_DATA");
                logger.debug("Using ET_CUST_DATA key, found data: {}", customerDataList != null);
            }

            if (customerDataList != null && !customerDataList.isEmpty()) {
                logger.info("Found {} customer records to save", customerDataList.size());
                try {
                    customerInfoService.saveCustomerInfoFromApi(customerDataList);
                    logger.info("Successfully saved {} customer records", customerDataList.size());
                } catch (Exception e) {
                    logger.error("Error saving customer data to database", e);
                    throw e;
                }
            } else {
                logger.warn("No customer data found in the response");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing customer info request", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/customer-info-json")
    public ResponseEntity<?> getCustomerInfoJson(@RequestParam(defaultValue = "20250423") String erdat) {
        try {
            Map<String, Object> result = sapService.getCustomerInfo(erdat);
            logger.debug("SAP Response for JSON: {}", result);  // 응답 데이터 로깅

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            
            // returnInfo 처리
            Map<String, Object> returnInfo = (Map<String, Object>) result.get("returnInfo");
            if (returnInfo == null) {
                returnInfo = (Map<String, Object>) result.get("ES_RETURN");
            }
            
            if (returnInfo != null) {
                response.put("status", returnInfo.get("TYPE"));
                response.put("message", returnInfo.get("MESSAGE"));
            } else {
                response.put("status", "S");
                response.put("message", "조회완료");
            }

            // 고객 데이터 처리
            List<Map<String, Object>> customerDataList = (List<Map<String, Object>>) result.get("customerList");
            if (customerDataList == null) {
                customerDataList = (List<Map<String, Object>>) result.get("ET_CUST_DATA");
            }
            
            response.put("customerList", customerDataList != null ? customerDataList : new ArrayList<>());
            response.put("searchDate", erdat);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing customer info JSON request", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/bill-info")
    public ResponseEntity<?> getBillInfo(
            @RequestParam(value = "IV_RECP_YM", required = false) String ivRecpYm,
            @RequestParam(value = "IV_ZGRPNO", required = false) String ivZgrpno,
            @RequestParam(value = "recpYm", defaultValue = "202501") String recpYm) {
        try {
            // IV_RECP_YM이 있으면 우선 사용, 없으면 기존 recpYm 사용 (하위호환성)
            String targetRecpYm = (ivRecpYm != null && !ivRecpYm.trim().isEmpty()) ? ivRecpYm : recpYm;
            
            logger.info("Bill info request - IV_RECP_YM: {}, IV_ZGRPNO: {}", targetRecpYm, ivZgrpno);
            
            Map<String, Object> result = sapService.getBillInfo(targetRecpYm);
            logger.debug("SAP Response for bill info: {}", result);

            // billList 키로 데이터 확인
            @SuppressWarnings("unchecked")
            List<Map<String, String>> billDataList = (List<Map<String, String>>) result.get("billList");
            
            // billList가 없으면 ET_BILL_DATA 키로 한번 더 확인
            if (billDataList == null) {
                billDataList = (List<Map<String, String>>) result.get("ET_BILL_DATA");
                logger.debug("Using ET_BILL_DATA key, found data: {}", billDataList != null);
            }

            // IV_ZGRPNO가 제공된 경우 해당 묶음번호로 필터링
            if (ivZgrpno != null && !ivZgrpno.trim().isEmpty() && billDataList != null) {
                logger.info("Filtering by ZGRPNO: {}", ivZgrpno);
                billDataList = billDataList.stream()
                    .filter(bill -> ivZgrpno.equals(String.valueOf(bill.get("ZGRPNO"))))
                    .collect(Collectors.toList());
                logger.info("Filtered result count: {}", billDataList.size());
                
                // 필터링된 결과를 다시 result에 저장
                if (result.containsKey("billList")) {
                    result.put("billList", billDataList);
                } else {
                    result.put("ET_BILL_DATA", billDataList);
                }
            }

            if (billDataList != null && !billDataList.isEmpty()) {
                logger.info("Found {} bill records to save", billDataList.size());
                try {
                    billInfoService.saveBillInfoFromApi(billDataList);
                    logger.info("Successfully saved {} bill records", billDataList.size());
                } catch (Exception e) {
                    logger.error("Error saving bill data to database", e);
                    throw e;
                }
            } else {
                logger.warn("No bill data found in the response");
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing bill info request", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 