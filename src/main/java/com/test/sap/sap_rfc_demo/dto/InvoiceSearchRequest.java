package com.test.sap.sap_rfc_demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceSearchRequest {
    private String orderNo;
    private String stcd2;
    private String kunnr;
} 