package com.test.sap.sap_rfc_demo.dto;

import java.util.List;
import java.util.Map;

public class BillInfoResponse {
    private Map<String, String> returnInfo;
    private List<Map<String, String>> billList;
    private String recpYm;

    public BillInfoResponse(Map<String, String> returnInfo, List<Map<String, String>> billList, String recpYm) {
        this.returnInfo = returnInfo;
        this.billList = billList;
        this.recpYm = recpYm;
    }

    public Map<String, String> getReturnInfo() {
        return returnInfo;
    }

    public void setReturnInfo(Map<String, String> returnInfo) {
        this.returnInfo = returnInfo;
    }

    public List<Map<String, String>> getBillList() {
        return billList;
    }

    public void setBillList(List<Map<String, String>> billList) {
        this.billList = billList;
    }

    public String getRecpYm() {
        return recpYm;
    }

    public void setRecpYm(String recpYm) {
        this.recpYm = recpYm;
    }
} 