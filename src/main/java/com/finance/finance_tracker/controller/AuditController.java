package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/list")
    public String auditPage(Model model) {
        List<AuditDto> logs = auditService.getAllAudits();
        model.addAttribute("logs", logs);
        return "audit/list";
    }
}
