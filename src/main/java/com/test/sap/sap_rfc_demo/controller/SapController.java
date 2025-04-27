package com.test.sap.sap_rfc_demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.test.sap.sap_rfc_demo.service.SapService;
import com.sap.conn.jco.JCoException;
import java.util.Map;

@Controller
public class SapController {

    @Autowired
    private SapService sapService;

    @GetMapping("/customer-info")
    public String getCustomerInfo(@RequestParam(defaultValue = "20250423") String erdat, Model model) {
        try {
            Map<String, Object> result = sapService.getCustomerInfo(erdat);
            model.addAttribute("returnInfo", result.get("returnInfo"));
            model.addAttribute("customerList", result.get("customerList"));
            model.addAttribute("erdat", erdat);
        } catch (JCoException e) {
            model.addAttribute("error", "Error occurred while fetching data from SAP: " + e.getMessage());
        }
        return "customer-info";
    }
} 