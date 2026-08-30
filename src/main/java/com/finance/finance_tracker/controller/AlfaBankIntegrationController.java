package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.service.BankImportService;
import com.finance.finance_tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Инициацию OAuth2-редиректа (GET /oauth2/authorization/alfabank) и приём
 * колбэка (GET /login/oauth2/code/alfabank) целиком берёт на себя Spring
 * Security — этот контроллер их не реализует. Параметр
 * @RegisteredOAuth2AuthorizedClient — при первом заходе на connect()
 * Spring САМ уводит пользователя на Alfa и обратно, повторно вызывая
 * этот же метод уже с непустым authorizedClient.
 *
 * Список счетов пользователя через API не запрашивается — не нашёл
 * подтверждённого эндпоинта в открытой документации Alfa (только
 * /accounts/{accountId}/statements, который уже требует готовый id).
 * Временно — ручной ввод номера счёта, как у T-Bank.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/bank-integration/alfa")
@ConditionalOnProperty(prefix = "alfa", name = "mode", havingValue = "oauth2")
public class AlfaBankIntegrationController {

    private final BankImportService bankImportService;

    @GetMapping("/connect")
    public String connect(@RegisteredOAuth2AuthorizedClient("alfabank") OAuth2AuthorizedClient authorizedClient,
                          Model model) {
        // authorizedClient гарантированно не null здесь — если бы авторизации
        // не было, Spring перехватил бы запрос раньше и увёл на Alfa
        model.addAttribute("currencies", Currency.values());
        return "bank-integration/alfa-link";
    }

    @PostMapping("/link")
    public String link(@RequestParam String externalAccountNumber,
                       @RequestParam String accountName,
                       @RequestParam Currency currency,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        Long userId = SecurityUtil.getCurrentUserId();

        try {
            Long accountId = bankImportService.linkAccount(
                    userId, "ALFA", externalAccountNumber, accountName, currency);
            redirectAttributes.addFlashAttribute("success",
                    "Счёт Альфа-Банка привязан. Запустите синхронизацию на странице счёта.");
            return "redirect:/accounts/" + accountId;
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("error", e.getMessage());
            return "bank-integration/alfa-link";
        }
    }
}
