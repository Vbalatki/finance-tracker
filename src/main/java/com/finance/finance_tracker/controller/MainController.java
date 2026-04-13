package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.*;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    //home page
    @GetMapping("/")
    public String home(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

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