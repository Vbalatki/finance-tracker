package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.TransactionService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TransactionController {
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final UserService userService;

    @GetMapping("/transactions")
    public String transactionsPage(@RequestParam(required = false) Long accountId,
                                   @RequestParam(required = false) Long categoryId,
                                   Model model) {
        Long userId = 1L;

        List<AccountDto> allAccounts = accountService.getUserAccounts(userId);
        List<AccountDto> accountsToShow = allAccounts;
        if (accountId != null) {
            accountsToShow = allAccounts.stream()
                    .filter(a -> a.getId().equals(accountId))
                    .collect(Collectors.toList());
        }

        // Реальный баланс выбранных счетов (текущий остаток на счетах)
        BigDecimal balance = userService.getUserTotalBalanceInRub(accountsToShow);

        // Статистика по транзакциям (доходы/расходы) для отображения сумм операций
        Map<AccountDto, List<TransactionDto>> transactionsByAccount = new LinkedHashMap<>();
        List<TransactionDto> allFilteredTransactions = new ArrayList<>();

        for (AccountDto acc : accountsToShow) {
            List<TransactionDto> txList = transactionService.findByAccountId(acc.getId());
            if (categoryId != null) {
                txList = txList.stream()
                        .filter(t -> categoryId.equals(t.getCategoryId()))
                        .collect(Collectors.toList());
            }
            txList.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
            transactionsByAccount.put(acc, txList);
            allFilteredTransactions.addAll(txList);
        }

        long totalTransactions = allFilteredTransactions.size();

        BigDecimal totalIncome = userService.getUserTotalIncomeInRub(allFilteredTransactions);
        BigDecimal totalExpense = userService.getUserTotalExpenseInRub(allFilteredTransactions);

        model.addAttribute("transactionsByAccount", transactionsByAccount);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("totalBalance", balance);
        model.addAttribute("accounts", allAccounts);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedCategoryId", categoryId);

        return "transactions/list";
    }

    @GetMapping("/transactions/create")
    public String createTransactionPage(
            @RequestParam(required = false) Long accountId,
            Model model) {

        Long userId = 1L;

        List<AccountDto> accounts = accountService.getUserAccounts(userId);
        List<CategoryDto> categories = categoryService.getAllCategories();

        TransactionDto dto = new TransactionDto();
        if (accountId != null) {
            dto.setAccountId(accountId);
        }

        model.addAttribute("transactionDto", dto);
        model.addAttribute("accounts", accounts);
        model.addAttribute("categories", categories);
        model.addAttribute("transactionTypes", TransactionType.values());

        return "transactions/create";
    }

    @PostMapping("/transactions")
    public String createTransaction(@ModelAttribute("transactionDto") @Valid TransactionDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (result.hasErrors()) {
            System.out.println("Validation errors: " + result.getAllErrors());
            Long userId = 1L;
            model.addAttribute("accounts", accountService.getUserAccounts(userId));
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("transactionTypes", TransactionType.values());
            return "transactions/create";
        }

        try {
            transactionService.createTransaction(dto);
            redirectAttributes.addFlashAttribute("success", "Транзакция успешно добавлена!");
            return "redirect:/transactions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/transactions/create";
        }
    }

    @GetMapping("transactions/{id}/edit")
    @ResponseBody
    public TransactionDto getTransactionEditForm(@PathVariable Long id) {
        return transactionService.findById(id);
    }

    @PostMapping("/transactions/{id}/update")
    public String updateTransaction(@PathVariable Long id,
                                    @Valid @ModelAttribute TransactionDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка валидации");
            return "redirect:/transactions";
        }
        dto.setId(id);
        try {
            transactionService.updateTransaction(dto);
            redirectAttributes.addFlashAttribute("success", "Транзакция обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transactions";
    }

    @PostMapping("/transactions/{id}/delete")
    public String deleteTransaction(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            TransactionDto transaction = transactionService.getTransactionById(id);
            Long accountId = transaction.getAccountId();
            transactionService.deleteTransaction(id);
            redirectAttributes.addFlashAttribute("success", "Транзакция удалена");
            if (accountId != null) {
                return "redirect:/accounts/" + accountId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transactions";
    }
}