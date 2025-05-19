package com.test.sap.sap_rfc_demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.test.sap.sap_rfc_demo.service.BundleMailService;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class SendMailController {

    @Autowired
    private BundleMailService bundleMailService;

    @GetMapping("/send-bill-mail-form")
    public String showForm() {
        return "sendMailForm";
    }

    @PostMapping("/send-bill-mail")
    public String sendBillMail(@RequestParam("zgrpno") String zgrpno, Model model) {
        try {
            bundleMailService.sendBillMailByZgrpno(zgrpno);
            model.addAttribute("message", "메일 발송이 정상적으로 처리되었습니다. (ZGRPNO: " + zgrpno + ")");
        } catch (Exception e) {
            model.addAttribute("message", "메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "sendMailForm";
    }
} 