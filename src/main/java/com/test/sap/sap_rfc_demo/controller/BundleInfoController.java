package com.test.sap.sap_rfc_demo.controller;

import com.test.sap.sap_rfc_demo.service.BundleInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BundleInfoController {

    private final BundleInfoService bundleInfoService;

    @GetMapping("/bundle-info")
    public String bundleInfoPage() {
        return "bundle-info";
    }

    @PostMapping("/api/bundle-info")
    @ResponseBody
    public Map<String, Object> getBundleInfo(@RequestBody Map<String, String> req) {
        String zgrpno = req.get("zgrpno");
        return bundleInfoService.getBundleInfo(zgrpno);
    }

    @PostMapping("/api/bundle-info/generate")
    @ResponseBody
    public Map<String, String> generateFiles(@RequestBody Map<String, String> req) {
        String zgrpno = req.get("zgrpno");
        Map<String, Object> data = bundleInfoService.getBundleInfo(zgrpno);
        return bundleInfoService.generateFiles(data);
    }
} 