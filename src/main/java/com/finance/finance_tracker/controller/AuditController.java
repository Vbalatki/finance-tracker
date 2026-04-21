package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/list")
    public String auditPage() {

    }
}
