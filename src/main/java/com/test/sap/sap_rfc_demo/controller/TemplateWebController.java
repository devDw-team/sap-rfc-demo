package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.dto.TemplateRequestDto;
import com.test.sap.sap_rfc_demo.service.TmplService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 템플릿 웹 폼 컨트롤러
 * 사업자 번호 입력 폼 및 템플릿 생성 처리
 */
@Controller
@RequestMapping("/template")
@Slf4j
public class TemplateWebController {
    
    @Autowired
    private TmplService tmplService;
    
    /**
     * 템플릿 생성 폼 페이지
     */
    @GetMapping("/form")
    public String showTemplateForm(Model model) {
        log.info("템플릿 생성 폼 페이지 접근");
        model.addAttribute("templateRequest", new TemplateRequestDto());
        return "template-form";
    }
    
    /**
     * 템플릿 생성 처리
     */
    @PostMapping("/generate")
    public String generateTemplate(@ModelAttribute TemplateRequestDto request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        
        log.info("템플릿 생성 요청 - 사업자번호: {}", request.getBusinessNo());
        
        try {
            // 기본적인 유효성 검증
            if (request.getBusinessNo() == null || request.getBusinessNo().trim().isEmpty()) {
                model.addAttribute("errorMessage", "사업자번호는 필수입니다.");
                model.addAttribute("templateRequest", request);
                return "template-form";
            }
            
            if (!request.isValidFormat()) {
                model.addAttribute("errorMessage", "사업자번호 형식이 올바르지 않습니다. (예: 1234567890)");
                model.addAttribute("templateRequest", request);
                return "template-form";
            }
            
            // 사업자 존재 여부 확인
            if (!tmplService.isBusinessExists(request.getBusinessNo())) {
                model.addAttribute("errorMessage", "해당 사업자번호를 찾을 수 없습니다: " + request.getBusinessNo());
                model.addAttribute("templateRequest", request);
                return "template-form";
            }
            
            // 실제 템플릿 생성
            tmplService.generateTemplateByBusinessNo(request.getBusinessNo());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "템플릿이 성공적으로 생성되었습니다. 사업자번호: " + request.getFormattedBusinessNo());
            redirectAttributes.addAttribute("businessNo", request.getBusinessNo());
            
            return "redirect:/template/result";
            
        } catch (IllegalArgumentException e) {
            log.error("사업자 번호 관련 오류", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("templateRequest", request);
            return "template-form";
        } catch (Exception e) {
            log.error("템플릿 생성 중 오류 발생", e);
            model.addAttribute("errorMessage", "템플릿 생성 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("templateRequest", request);
            return "template-form";
        }
    }
    
    /**
     * 템플릿 생성 결과 페이지
     */
    @GetMapping("/result")
    public String showResult(@RequestParam(required = false) String businessNo, Model model) {
        log.info("템플릿 생성 결과 페이지 접근 - 사업자번호: {}", businessNo);
        
        if (businessNo != null) {
            // 포맷팅된 사업자번호로 표시
            String formattedBusinessNo = TemplateRequestDto.formatBusinessNo(businessNo);
            model.addAttribute("businessNo", businessNo);
            model.addAttribute("formattedBusinessNo", formattedBusinessNo);
        }
        
        return "template-result";
    }
    
    /**
     * 메인 페이지 (추가)
     */
    @GetMapping
    public String index() {
        return "redirect:/template/form";
    }
} 