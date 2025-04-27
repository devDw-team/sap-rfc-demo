package com.test.sap.sap_rfc_demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.test.sap.sap_rfc_demo.service.SapConnectionTestService;

@Controller
public class SapConnectionTestController {

    @Autowired
    private SapConnectionTestService connectionTestService;

    @GetMapping("/test-connection")
    public String testConnection(Model model) {
        model.addAttribute("connectionInfo", connectionTestService.testConnection());
        return "connection-test";
    }
} 