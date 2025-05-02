package com.test.sap.sap_rfc_demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InvoiceController {
    
    @GetMapping("/invoice/search")
    public String showSearchForm() {
        return "invoice-search";
    }
} 