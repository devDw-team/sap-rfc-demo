package com.test.sap.sap_rfc_demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.test.sap.sap_rfc_demo.service.SapService;
import com.test.sap.sap_rfc_demo.dto.CustomerInfoResponse;
import com.sap.conn.jco.JCoException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SapApiController {

    @Autowired
    private SapService sapService;

    @GetMapping("/customer-info")
    public CustomerInfoResponse getCustomerInfo(@RequestParam(defaultValue = "20250423") String erdat) throws JCoException {
        Map<String, Object> result = sapService.getCustomerInfo(erdat);
        return new CustomerInfoResponse(
            (Map<String, String>) result.get("returnInfo"),
            (List<Map<String, String>>) result.get("customerList"),
            erdat
        );
    }
} 