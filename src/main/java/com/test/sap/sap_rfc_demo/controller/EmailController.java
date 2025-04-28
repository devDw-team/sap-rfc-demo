package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.dto.EmailSendRequest;
import com.test.sap.sap_rfc_demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EmailController {
    @Autowired
    private EmailService emailService;

    @GetMapping("/email/form")
    public String showEmailForm(Model model) {
        model.addAttribute("emailSendRequest", new EmailSendRequest());
        return "email-form";
    }

    @PostMapping("/email/send")
    public String sendEmail(@ModelAttribute EmailSendRequest emailSendRequest, Model model) {
        // 실제 환경에서는 입력값 검증 추가 필요
        boolean result = emailService.sendEmail(emailSendRequest);
        model.addAttribute("resultMsg", result ? "이메일 발송 성공!" : "이메일 발송 실패!");
        model.addAttribute("emailSendRequest", emailSendRequest);
        return "email-form";
    }
} 