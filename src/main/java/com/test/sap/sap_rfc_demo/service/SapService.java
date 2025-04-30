package com.test.sap.sap_rfc_demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sap.conn.jco.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SapService {

    private static final Logger logger = LoggerFactory.getLogger(SapService.class);

    @Autowired
    private JCoDestination destination;

    public Map<String, Object> getCustomerInfo(String ivErdat) throws JCoException {
        logger.debug("Calling SAP RFC with erdat: {}", ivErdat);
        
        JCoFunction function = destination.getRepository().getFunction("Z_RE_B2B_CUST_INFO");
        if (function == null) {
            throw new RuntimeException("RFC Z_RE_B2B_CUST_INFO not found in SAP.");
        }

        // Set import parameters
        function.getImportParameterList().setValue("IV_ERDAT", ivErdat);

        // Execute the function
        function.execute(destination);
        logger.debug("SAP RFC executed successfully");

        // Get the result
        Map<String, Object> result = new HashMap<>();
        
        // Get export parameter
        JCoStructure esReturn = function.getExportParameterList().getStructure("ES_RETURN");
        if (esReturn != null) {
            Map<String, Object> returnInfo = new HashMap<>();
            for (JCoField field : esReturn) {
                returnInfo.put(field.getName(), field.getString());
            }
            result.put("returnInfo", returnInfo);
            logger.debug("Return info processed: {}", returnInfo);
        }

        // Get table parameter
        JCoTable resultTable = function.getTableParameterList().getTable("ET_CUST_DATA");
        List<Map<String, Object>> customerList = new ArrayList<>();

        for (int i = 0; i < resultTable.getNumRows(); i++) {
            resultTable.setRow(i);
            Map<String, Object> row = new HashMap<>();
            
            for (JCoField field : resultTable) {
                row.put(field.getName(), field.getString());
            }
            
            customerList.add(row);
        }
        result.put("customerList", customerList);
        logger.debug("Processed {} customer records", customerList.size());

        return result;
    }

    public Map<String, Object> getBillInfo(String ivRecpYm) throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("Z_RE_B2B_BILL_INFO");
        if (function == null) {
            throw new RuntimeException("RFC Z_RE_B2B_BILL_INFO not found in SAP.");
        }

        // Import 파라미터 세팅
        function.getImportParameterList().setValue("IV_RECP_YM", ivRecpYm);

        // RFC 실행
        function.execute(destination);

        Map<String, Object> result = new HashMap<>();

        // Export 파라미터 처리
        JCoStructure esReturn = function.getExportParameterList().getStructure("ES_RETURN");
        if (esReturn != null) {
            Map<String, String> returnInfo = new HashMap<>();
            for (JCoField field : esReturn) {
                returnInfo.put(field.getName(), field.getString());
            }
            result.put("returnInfo", returnInfo);
        }

        // Table 파라미터 처리
        JCoTable billTable = function.getTableParameterList().getTable("ET_BILL_DATA");
        List<Map<String, String>> billList = new ArrayList<>();
        for (int i = 0; i < billTable.getNumRows(); i++) {
            billTable.setRow(i);
            Map<String, String> row = new HashMap<>();
            for (JCoField field : billTable) {
                row.put(field.getName(), field.getString());
            }
            billList.add(row);
        }
        result.put("billList", billList);
        return result;
    }
} 