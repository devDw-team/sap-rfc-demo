package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;

@Data
public class CustomerInfo {
    private String customerId;
    private String customerName;
    private String address;
    // 필요한 고객 정보 필드 추가
} 