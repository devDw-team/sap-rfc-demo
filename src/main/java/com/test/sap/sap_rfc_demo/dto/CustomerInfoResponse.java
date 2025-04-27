package com.test.sap.sap_rfc_demo.dto;

import java.util.List;
import java.util.Map;

public class CustomerInfoResponse {
    private Map<String, String> returnInfo;
    private List<Map<String, String>> customerList;
    private String erdat;

    public CustomerInfoResponse(Map<String, String> returnInfo, List<Map<String, String>> customerList, String erdat) {
        this.returnInfo = returnInfo;
        this.customerList = customerList;
        this.erdat = erdat;
    }

    public Map<String, String> getReturnInfo() {
        return returnInfo;
    }

    public void setReturnInfo(Map<String, String> returnInfo) {
        this.returnInfo = returnInfo;
    }

    public List<Map<String, String>> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Map<String, String>> customerList) {
        this.customerList = customerList;
    }

    public String getErdat() {
        return erdat;
    }

    public void setErdat(String erdat) {
        this.erdat = erdat;
    }
} 