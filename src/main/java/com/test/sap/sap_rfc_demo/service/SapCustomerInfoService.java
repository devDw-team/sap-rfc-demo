package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.entity.SapCustomerInfo;
import com.test.sap.sap_rfc_demo.repository.SapCustomerInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Service
public class SapCustomerInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SapCustomerInfoService.class);
    private final SapCustomerInfoRepository customerInfoRepository;

    @Autowired
    public SapCustomerInfoService(SapCustomerInfoRepository customerInfoRepository) {
        this.customerInfoRepository = customerInfoRepository;
    }

    @Transactional
    public void saveCustomerInfoFromApi(List<Map<String, Object>> customerDataList) {
        logger.debug("Starting to save {} customer records", customerDataList.size());
        
        for (Map<String, Object> customerData : customerDataList) {
            try {
                SapCustomerInfo customerInfo = new SapCustomerInfo();
                
                // API 응답 데이터를 엔티티에 매핑
                customerInfo.setOrderNo(convertToString(customerData.get("ORDER_NO")));
                customerInfo.setStcd2(convertToString(customerData.get("STCD2")));
                customerInfo.setKunnr(convertToString(customerData.get("KUNNR")));
                customerInfo.setCustNm(convertToString(customerData.get("CUST_NM")));
                customerInfo.setFxday(parseShort(customerData.get("FXDAY")));
                customerInfo.setZgrpno(parseLong(customerData.get("ZGRPNO")));
                customerInfo.setSelKun(convertToString(customerData.get("SEL_KUN")));
                customerInfo.setJuso(convertToString(customerData.get("JUSO")));
                customerInfo.setPstlz(convertToString(customerData.get("PSTLZ")));
                customerInfo.setJ1kftbus(convertToString(customerData.get("J_1KFTBUS")));
                customerInfo.setJ1kftind(convertToString(customerData.get("J_1KFTIND")));
                customerInfo.setJ1kfrepre(convertToString(customerData.get("J_1KFREPRE")));
                customerInfo.setEmail(convertToString(customerData.get("EMAIL")));
                customerInfo.setEmail2(convertToString(customerData.get("EMAIL2")));
                customerInfo.setPayMthd(convertToString(customerData.get("PAY_MTHD")));
                customerInfo.setPayMthdTx(convertToString(customerData.get("PAY_MTHD_TX")));
                customerInfo.setPayCom(convertToString(customerData.get("PAY_COM")));
                customerInfo.setPayComTx(convertToString(customerData.get("PAY_COM_TX")));
                customerInfo.setPayNo(convertToString(customerData.get("PAY_NO")));
                customerInfo.setPreMonth(parseBigDecimal(customerData.get("PRE_MONTH")));
                customerInfo.setPreAmt(parseBigDecimal(customerData.get("PRE_AMT")));
                customerInfo.setRemainAmt(parseBigDecimal(customerData.get("REMAIN_AMT")));
                customerInfo.setSendAuto(convertToString(customerData.get("SEND_AUTO")));

                customerInfoRepository.save(customerInfo);
                logger.debug("Successfully saved customer with ORDER_NO: {}", customerInfo.getOrderNo());
            } catch (Exception e) {
                logger.error("Error saving customer data: {}", customerData, e);
                throw new RuntimeException("Failed to save customer data", e);
            }
        }
    }

    private String convertToString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof String) {
                String strValue = (String) value;
                return strValue.isEmpty() ? null : Integer.parseInt(strValue);
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer value: {}", value);
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Long) return (Long) value;
            if (value instanceof Integer) return ((Integer) value).longValue();
            if (value instanceof String) {
                String strValue = (String) value;
                return strValue.isEmpty() ? null : Long.parseLong(strValue);
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse long value: {}", value);
            return null;
        }
    }

    private java.math.BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof java.math.BigDecimal) return (java.math.BigDecimal) value;
            if (value instanceof String) {
                String strValue = (String) value;
                return strValue.isEmpty() ? null : new java.math.BigDecimal(strValue);
            }
            if (value instanceof Number) {
                return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse BigDecimal value: {}", value);
            return null;
        }
    }

    private Short parseShort(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Short) return (Short) value;
            if (value instanceof Integer) return ((Integer) value).shortValue();
            if (value instanceof String) {
                String strValue = (String) value;
                return strValue.isEmpty() ? null : Short.parseShort(strValue);
            }
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            }
            return null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse short value: {}", value);
            return null;
        }
    }

    public List<SapCustomerInfo> getAllCustomerInfo() {
        return customerInfoRepository.findAll();
    }
} 