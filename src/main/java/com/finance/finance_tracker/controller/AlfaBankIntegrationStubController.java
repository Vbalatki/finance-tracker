// src/main/java/com/finance/finance_tracker/controller/AlfaBankIntegrationStubController.java
package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.service.BankImportService;
import com.finance.finance_tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Активен при alfa.mode=stub (по умолчанию) — те же два URL, что у
 * OAuth2-версии (AlfaBankIntegrationController), но без редиректа на
 * банк: /connect сразу отдаёт форму, как /bank-integration/link у
 * T-Bank. Ровно один из двух контроллеров существует в контексте
 * одновременно — Spring не увидит конфликта маппинга.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/bank-integration/alfa")
@ConditionalOnProperty(prefix = "alfa", name = "mode", havingValue = "stub", matchIfMissing = true)
public class AlfaBankIntegrationStubController {

    private final BankImportService bankImportService;

    @GetMapping("/connect")
    public String connect(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        model.addAttribute("currencies", Currency.values());
        return "bank-integration/alfa-link";
    }

    @PostMapping("/link")
    public String link(@RequestParam String externalAccountNumber,
                       @RequestParam String accountName,
                       @RequestParam Currency currency,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (userDetails == null) return "redirect:/login";
        Long userId = SecurityUtil.getCurrentUserId();

        try {
            Long accountId = bankImportService.linkAccount(
                    userId, "ALFA", externalAccountNumber, accountName, currency);
            redirectAttributes.addFlashAttribute("success",
                    "Счёт Альфа-Банка привязан (тестовый режим). Запустите синхронизацию на странице счёта.");
            return "redirect:/accounts/" + accountId;
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("error", e.getMessage());
            return "bank-integration/alfa-link";
        }
    }
}