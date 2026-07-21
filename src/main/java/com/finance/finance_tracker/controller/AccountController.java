package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.Util.CurrencyFormatter;
import com.finance.finance_tracker.Util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.AccessDeniedException;
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

/**
 * Thymeleaf-контроллер для страниц управления счетами: список, создание,
 * детальный просмотр, пополнение/снятие, редактирование, удаление.
 *
 * <p>Доступ к чужим счетам (по id, не принадлежащему текущему
 * аутентифицированному пользователю) запрещается проверкой
 * {@code account.getUserId().equals(SecurityUtil.getCurrentUserId())}
 * в каждом методе, работающем с конкретным счётом.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CurrencyFormatter currencyFormatter;

    /**
     * Страница списка счетов текущего пользователя со сводкой по валютам.
     *
     * @param model       модель представления
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @return {@code "accounts/list"}, либо редирект на {@code /login}, если пользователь не аутентифицирован
     */
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

    /**
     * Страница формы создания нового счёта.
     *
     * @param model       модель представления
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @return {@code "accounts/create"}, либо редирект на {@code /login}
     */
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

    /**
     * Обрабатывает отправку формы создания счёта.
     *
     * @param dto               данные формы
     * @param result            результат валидации
     * @param userDetails       текущий пользователь; {@code null}, если не аутентифицирован
     * @param redirectAttributes атрибуты для flash-сообщений после редиректа
     * @param model             модель представления (используется при повторном рендере формы)
     * @return редирект на {@code /accounts} при успехе, иначе {@code "accounts/create"}
     */
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

    /**
     * Страница деталей счёта: баланс, последние операции, быстрые действия.
     *
     * @param id          id счёта
     * @param userDetails текущий пользователь
     * @param model       модель представления
     * @return {@code "accounts/detail"}
     * @throws AccessDeniedException если счёт принадлежит другому пользователю
     */
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
     * <p><b>Внимание:</b> в отличие от остальных методов, здесь нет проверки
     * принадлежности счёта текущему пользователю — форму по id чужого счёта
     * можно открыть на просмотр (сама форма отправки, впрочем, не подключена
     * отдельным POST-обработчиком в этом контроллере).
     *
     * @param id    id счёта
     * @param model модель представления
     * @return {@code "accounts/edit"}
     */
    @GetMapping("/{id}/edit")
    public String editAccountPage(@PathVariable Long id, Model model) {
        AccountDto account = accountService.findById(id);

        model.addAttribute("accountDto", account);
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("currencyFormatter", currencyFormatter);

        return "accounts/edit";
    }

    /**
     * Пополняет счёт на указанную сумму. Ошибки (включая попытку доступа
     * к чужому счёту) не приводят к HTTP-ошибке — они перехватываются и
     * отображаются как flash-сообщение после редиректа.
     *
     * @param id                 id счёта
     * @param amount             сумма пополнения, минимум 0.01
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /accounts/{id}}
     */
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

    /**
     * Снимает средства со счёта. Ошибки (недостаточно средств, чужой счёт
     * и т.д.) перехватываются и отображаются как flash-сообщение.
     *
     * @param id                 id счёта
     * @param amount             сумма снятия, минимум 0.01
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /accounts/{id}}
     */
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

    /**
     * Удаляет счёт вместе со всеми его транзакциями. Ошибки перехватываются
     * и отображаются как flash-сообщение вместо HTTP-ошибки.
     *
     * @param id                 id счёта
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /accounts}
     */
    @PostMapping("/{id}/delete")
    public String deleteAccount(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            AccountDto account = accountService.findById(id);
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!account.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Нет доступа к этому счёту");
            }
             account = accountService.findById(id);
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Счет '%s' удален", account.getName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }
}
