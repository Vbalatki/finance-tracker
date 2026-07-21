package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.Util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.TransactionService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thymeleaf-контроллер для страниц управления транзакциями: список с
 * фильтрами и статистикой, создание, редактирование через модальное окно
 * (AJAX, {@link #getTransactionEditForm}/{@link #updateTransaction}) и удаление.
 *
 * <p>Доступ к чужим транзакциям запрещается проверкой владельца счёта,
 * к которому привязана транзакция.
 */
@Controller
@RequiredArgsConstructor
public class TransactionController {
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final UserService userService;

    /**
     * Страница списка транзакций пользователя с фильтрами по счёту и
     * категории, а также сводной статистикой (доходы/расходы/баланс).
     *
     * @param accountId   id счёта для фильтрации, необязателен
     * @param categoryId  id категории для фильтрации, необязателен
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @param model       модель представления
     * @return {@code "transactions/list"}, либо редирект на {@code /login}
     */
    @GetMapping("/transactions")
    public String transactionsPage(@RequestParam(required = false) Long accountId,
                                   @RequestParam(required = false) Long categoryId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        if (userDetails == null) return "redirect:/login";
        Long userId = SecurityUtil.getCurrentUserId();

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

    /**
     * Страница формы создания новой транзакции.
     *
     * @param accountId предзаполняемый id счёта, необязателен
     * @param model     модель представления
     * @return {@code "transactions/create"}
     */
    @GetMapping("/transactions/create")
    public String createTransactionPage(
            @RequestParam(required = false) Long accountId,
            Model model) {

        Long userId = SecurityUtil.getCurrentUserId();

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

    /**
     * Обрабатывает отправку формы создания транзакции.
     *
     * @param dto                данные формы
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @param model              модель представления (при повторном рендере формы)
     * @return редирект на {@code /transactions} при успехе, иначе {@code "transactions/create"}
     */
    @PostMapping("/transactions")
    public String createTransaction(@ModelAttribute("transactionDto") @Valid TransactionDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (result.hasErrors()) {
            System.out.println("Validation errors: " + result.getAllErrors());
            Long userId = SecurityUtil.getCurrentUserId();
            model.addAttribute("accounts", accountService.getUserAccounts(userId));
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("transactionTypes", TransactionType.values());
            return "transactions/create";
        }

        try {
            transactionService.saveTransaction(dto);
            redirectAttributes.addFlashAttribute("success", "Транзакция успешно добавлена!");
            return "redirect:/transactions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/transactions/create";
        }
    }

    /**
     * Отдаёт данные транзакции в формате JSON для предзаполнения модального
     * окна редактирования (вызывается через {@code fetch} из клиентского JS).
     *
     * @param id id транзакции
     * @return DTO транзакции
     * @throws AccessDeniedException если транзакция привязана к чужому счёту
     */
    @GetMapping("transactions/{id}/edit")
    @ResponseBody
    public TransactionDto getTransactionEditForm(@PathVariable Long id) {
        TransactionDto existing = transactionService.getTransactionById(id);
        AccountDto account = accountService.findById(existing.getAccountId());
        if (!account.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new AccessDeniedException("Нет доступа к этой транзакции");
        }
        return transactionService.findById(id);
    }

    /**
     * Обрабатывает отправку формы редактирования транзакции (модальное окно).
     *
     * @param id                 id транзакции
     * @param dto                новые значения полей
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /transactions}
     * @throws AccessDeniedException если транзакция привязана к чужому счёту
     */
    @PostMapping("/transactions/{id}/update")
    public String updateTransaction(@PathVariable Long id,
                                    @Valid @ModelAttribute TransactionDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        TransactionDto existing = transactionService.getTransactionById(id);
        AccountDto account = accountService.findById(existing.getAccountId());
        if (!account.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new AccessDeniedException("Нет доступа к этой транзакции");
        }

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

    /**
     * Удаляет транзакцию. Ошибки (включая доступ к чужой транзакции)
     * перехватываются и отображаются как flash-сообщение.
     *
     * @param id                 id транзакции
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /accounts/{accountId}} при успехе, иначе на {@code /transactions}
     */
    @PostMapping("/transactions/{id}/delete")
    public String deleteTransaction(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            TransactionDto transaction = transactionService.getTransactionById(id);
            AccountDto account = accountService.findById(transaction.getAccountId());
            if (!account.getUserId().equals(SecurityUtil.getCurrentUserId())) {
                throw new AccessDeniedException("Нет доступа к этой транзакции");
            }

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
