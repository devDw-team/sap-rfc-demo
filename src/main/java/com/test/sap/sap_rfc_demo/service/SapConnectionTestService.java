package com.test.sap.sap_rfc_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sap.conn.jco.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class SapConnectionTestService {

    @Autowired
    private JCoDestination destination;

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 연결 상태 확인
            destination.ping();
            
            // 연결 속성 정보 가져오기
            JCoAttributes attributes = destination.getAttributes();
            
            result.put("status", "success");
            result.put("message", "SAP 연결 성공");
            result.put("systemId", attributes.getSystemID());
            result.put("client", attributes.getClient());
            result.put("user", attributes.getUser());
            result.put("language", attributes.getLanguage());
            result.put("host", attributes.getPartnerHost());
            result.put("systemNumber", attributes.getSystemNumber());
            
        } catch (JCoException e) {
            result.put("status", "error");
            result.put("message", "SAP 연결 실패: " + e.getMessage());
            result.put("errorCode", String.valueOf(e.getGroup()));
            result.put("errorDetails", e.getKey());
        }
        
        return result;
    }
} 