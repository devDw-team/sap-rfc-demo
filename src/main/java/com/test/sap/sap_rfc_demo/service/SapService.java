package com.test.sap.sap_rfc_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sap.conn.jco.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SapService {

    @Autowired
    private JCoDestination destination;

    public Map<String, Object> getCustomerInfo(String ivErdat) throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("Z_RE_B2B_CUST_INFO");
        if (function == null) {
            throw new RuntimeException("RFC Z_RE_B2B_CUST_INFO not found in SAP.");
        }

        // Set import parameters
        function.getImportParameterList().setValue("IV_ERDAT", ivErdat);

        // Execute the function
        function.execute(destination);

        // Get the result
        Map<String, Object> result = new HashMap<>();
        
        // Get export parameter
        JCoStructure esReturn = function.getExportParameterList().getStructure("ES_RETURN");
        if (esReturn != null) {
            Map<String, String> returnInfo = new HashMap<>();
            for (JCoField field : esReturn) {
                returnInfo.put(field.getName(), field.getString());
            }
            result.put("returnInfo", returnInfo);
        }

        // Get table parameter
        JCoTable resultTable = function.getTableParameterList().getTable("ET_CUST_DATA");
        List<Map<String, String>> customerList = new ArrayList<>();

        for (int i = 0; i < resultTable.getNumRows(); i++) {
            resultTable.setRow(i);
            Map<String, String> row = new HashMap<>();
            
            for (JCoField field : resultTable) {
                row.put(field.getName(), field.getString());
            }
            
            customerList.add(row);
        }
        result.put("customerList", customerList);

        return result;
    }
} 