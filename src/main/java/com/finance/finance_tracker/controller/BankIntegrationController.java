package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.service.BankImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf-контроллер привязки счёта к внешнему банку. Отдельно от
 * {@link AccountController}, потому что тут ещё нет id существующего
 * Account — сам Account создаётся в процессе привязки.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/bank-integration")
public class BankIntegrationController {

    private final BankImportService bankImportService;

    @GetMapping("/link")
    public String linkPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        model.addAttribute("currencies", Currency.values());
        return "bank-integration/link";
    }

    @PostMapping("/link")
    public String link(@RequestParam String bankCode,
                       @RequestParam String externalAccountNumber,
                       @RequestParam String accountName,
                       @RequestParam Currency currency,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (userDetails == null) return "redirect:/login";
        Long userId = SecurityUtil.getCurrentUserId();

        try {
            Long accountId = bankImportService.linkAccount(
                    userId, bankCode, externalAccountNumber, accountName, currency);
            redirectAttributes.addFlashAttribute("success",
                    "Счёт привязан. Запустите синхронизацию на странице счёта, чтобы загрузить операции.");
            return "redirect:/accounts/" + accountId;
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("error", e.getMessage());
            return "bank-integration/link";
        }
    }
}