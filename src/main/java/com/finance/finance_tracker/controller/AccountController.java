package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.Util.CurrencyFormatter;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.TransactionService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = 1L;

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

        Long userId = 1L;

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
    public String accountDetail(@PathVariable Long id, Model model) {
        AccountDto account = accountService.findById(id);

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

    @GetMapping("/{id}/edit")
    public String editAccountPage(@PathVariable Long id, Model model) {
        AccountDto account = accountService.findById(id);

        model.addAttribute("accountDto", account);
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("currencyFormatter", currencyFormatter);

        return "accounts/edit";
    }

/*  может, понадобится
    @PostMapping("/{id}/edit")
    public String updateAccount(@PathVariable Long id,
                                @ModelAttribute("accountDto") @Valid AccountDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", Currency.values());
            return "accounts/edit";
        }

        try {
            accountService.updateAccount(id, dto);
            redirectAttributes.addFlashAttribute("success", "Счет успешно обновлен");
            return "redirect:/accounts/" + id;
        } catch (Exception e) {
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("error", e.getMessage());
            return "accounts/edit";
        }
    }*/

    @PostMapping("/{id}/deposit")
    public String deposit(@PathVariable Long id,
                          @RequestParam @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0") BigDecimal amount,
                          RedirectAttributes redirectAttributes) {
        try {
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
                           RedirectAttributes redirectAttributes) {
        try {
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
    public String deleteAccount(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            AccountDto account = accountService.findById(id);
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Счет '%s' удален", account.getName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }
}