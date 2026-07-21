package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.*;
import com.finance.finance_tracker.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thymeleaf-контроллер корневых страниц: главная (редирект в зависимости
 * от аутентификации) и дашборд со сводкой по счетам и последним транзакциям.
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    /**
     * Корневая страница — просто редиректит дальше в зависимости от
     * статуса аутентификации.
     *
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @return редирект на {@code /dashboard}, если аутентифицирован, иначе на {@code /login}
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    /**
     * Дашборд: данные пользователя, список счетов, суммарный баланс в
     * рублях и 10 последних транзакций.
     *
     * @param model       модель представления
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @param request     текущий HTTP-запрос (используется для подсветки активного пункта меню)
     * @return {@code "dashboard/index"}, либо редирект на {@code /login}
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                            HttpServletRequest request) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        Long userId = user.getId();

        List<AccountDto> accounts = accountService.getUserAccounts(userId);


        List<TransactionDto> recentTransactions = transactionService.findRecentByUserId(userId, 10);

        BigDecimal totalBalance = userService.getUserTotalBalanceInRub(accounts);

        // Добавляем текущий путь для активного меню
        String currentPath = request.getRequestURI();
        model.addAttribute("currentPath", currentPath);

        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("recentTransactions", recentTransactions);

        return "dashboard/index";
    }

}
