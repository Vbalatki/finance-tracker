package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.dto.TransactionDto;
import com.finance.finance_tracker.util.CurrencyFormatter;
import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.TransactionService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CurrencyFormatter currencyFormatter;

    @GetMapping
    public String accountsPage(Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        Long userId = SecurityUtil.getCurrentUserId();

        List<AccountDto> accounts = accountService.getUserAccounts(userId);

        BigDecimal totalBalance = userService.getUserTotalBalanceInRub(accounts);

        Map<Currency, BigDecimal> balanceByCurrency = accounts.stream()
                .filter(acc -> acc.getBalance() != null)
                .collect(Collectors.groupingBy(
                        AccountDto::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, AccountDto::getBalance, BigDecimal::add)
                ));

        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance != null ? totalBalance : BigDecimal.ZERO);
        model.addAttribute("balanceByCurrency", balanceByCurrency);
        model.addAttribute("currencyFormatter", currencyFormatter);

        return "accounts/list";
    }

    @GetMapping("/create")
    public String createAccountPage(Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Long userId = SecurityUtil.getCurrentUserId();

        AccountDto accountDto = new AccountDto();
        accountDto.setUserId(userId);

        model.addAttribute("accountDto", accountDto);
        model.addAttribute("currencies", Currency.values());

        return "accounts/create";
    }

    @PostMapping
    public String createAccount(@ModelAttribute("accountDto") @Valid AccountDto dto,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("currencies", Currency.values());
            return "accounts/create";
        }

        try {
            accountService.saveAccount(dto);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Счет '%s' успешно создан!", dto.getName()));
            return "redirect:/accounts";
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("error", e.getMessage());
            return "accounts/create";
        }
    }

    @GetMapping("/{id}")
    public String accountDetail(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        AccountDto account = accountService.findById(id);
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!account.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Нет доступа к этому счёту");
        }
        List<TransactionDto> transactions = transactionService.findByAccountId(id);

        String formattedBalance = currencyFormatter.formatAmount(
                account.getBalance(),
                account.getCurrency()
        );

        model.addAttribute("account", account);
        model.addAttribute("formattedBalance", formattedBalance);
        model.addAttribute("transactions", transactions);
        model.addAttribute("depositAmount", BigDecimal.ZERO);
        model.addAttribute("withdrawAmount", BigDecimal.ZERO);

        return "accounts/detail";
    }

    /**
     * Страница формы редактирования счёта.
     *
     * @param id    id счёта
     * @param model модель представления
     * @return {@code "accounts/edit"}
     * @throws AccessDeniedException если счёт принадлежит другому пользователю
     */
    @GetMapping("/{id}/edit")
    public String editAccountPage(@PathVariable Long id, Model model) {
        AccountDto account = accountService.findById(id);
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!account.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Нет доступа к этому счёту");
        }

        model.addAttribute("accountDto", account);
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("currencyFormatter", currencyFormatter);

        return "accounts/edit";
    }

    /**
     * Обрабатывает отправку формы редактирования счёта. Баланс этим методом
     * не меняется — {@code dto.getBalance()} передаётся скрытым полем
     * только для прохождения {@code @NotNull}-валидации dto, сам сервис
     * {@link AccountService#updateAccount(Long, AccountDto)} его не читает.
     * Для изменения баланса есть отдельные {@code deposit}/{@code withdraw}.
     *
     * @param id                 id счёта
     * @param dto                новые значения имени/валюты
     * @param result             результат валидации
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @param model              модель представления (при повторном рендере формы)
     * @return редирект на {@code /accounts/{id}} при успехе, иначе {@code "accounts/edit"}
     * @throws AccessDeniedException если счёт принадлежит другому пользователю
     */
    @PostMapping("/{id}/edit")
    public String updateAccount(@PathVariable Long id,
                                @Valid @ModelAttribute("accountDto") AccountDto dto,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        AccountDto existing = accountService.findById(id);
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!existing.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Нет доступа к этому счёту");
        }

        if (result.hasErrors()) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("currencyFormatter", currencyFormatter);
            return "accounts/edit";
        }

        try {
            accountService.updateAccount(id, dto);
            redirectAttributes.addFlashAttribute("success", "Счёт успешно обновлён");
            return "redirect:/accounts/" + id;
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("currencyFormatter", currencyFormatter);
            model.addAttribute("error", e.getMessage());
            return "accounts/edit";
        }
    }

    @PostMapping("/{id}/deposit")
    public String deposit(@PathVariable Long id,
                          @RequestParam @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0") BigDecimal amount,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            AccountDto account = accountService.findById(id);
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!account.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Нет доступа к этому счёту");
            }
            accountService.deposit(id, amount);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Счет пополнен на %s",
                            currencyFormatter.formatAmount(amount, Currency.RUB)));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                           @RequestParam @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0") BigDecimal amount,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            AccountDto account = accountService.findById(id);
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!account.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Нет доступа к этому счёту");
            }
            accountService.withdraw(id, amount);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Со счета снято %s",
                            currencyFormatter.formatAmount(amount, Currency.RUB)));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/delete")
    public Object deleteAccount(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                @RequestHeader(value = "Accept", required = false) String accept,
                                RedirectAttributes redirectAttributes) {
        boolean wantsJson = accept != null && accept.contains("application/json");

        try {
            AccountDto account = accountService.findById(id);
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!account.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Нет доступа к этому счёту");
            }

            String accountName = account.getName();
            accountService.deleteAccount(id);
            String message = String.format("Счет '%s' удален", accountName);

            if (wantsJson) {
                return ResponseEntity.ok(Map.of("success", true, "message", message));
            }
            redirectAttributes.addFlashAttribute("success", message);
            return "redirect:/accounts";

        } catch (Exception e) {
            if (wantsJson) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }
}