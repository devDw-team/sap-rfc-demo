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
} 