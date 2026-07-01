package com.financetracker.bot.controller;

import com.financetracker.bot.dto.ExpenseView;
import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Friend;
import com.financetracker.bot.service.ExpenseService;
import com.financetracker.bot.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Read-only data API for the local dashboard (see /dashboard.html).
 * Intentionally unauthenticated: this app assumes a single personal user
 * accessing it from localhost only, per the no-login design decision.
 * No write/delete endpoints are exposed here -- those stay in the Telegram
 * bot flow, where they belong to the user who's actually messaging.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExpenseService expenseService;
    private final FriendService friendService;

    public DashboardController(ExpenseService expenseService, FriendService friendService) {
        this.expenseService = expenseService;
        this.friendService = friendService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getMonthSummary() {
        return withPrimaryUser(user -> {
            ExpenseService.MonthSummary summary = expenseService.getCurrentMonthSummary(user);
            BigDecimal total = summary.total() != null ? summary.total() : BigDecimal.ZERO;
            BigDecimal previousTotal = expenseService.getPreviousMonthTotal(user);

            BigDecimal percentChange = BigDecimal.ZERO;
            if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentChange = total.subtract(previousTotal)
                        .divide(previousTotal, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            int dayOfMonth = java.time.LocalDate.now().getDayOfMonth();
            BigDecimal avgPerDay = total.divide(BigDecimal.valueOf(dayOfMonth), 2, java.math.RoundingMode.HALF_UP);

            int transactionCount = expenseService.getAllExpenses(user).size();

            return Map.of(
                    "total", total,
                    "previousTotal", previousTotal,
                    "percentChange", percentChange,
                    "avgPerDay", avgPerDay,
                    "transactionCount", transactionCount,
                    "currency", user.getDefaultCurrency(),
                    "byCategory", summary.byCategory()
            );
        });
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> getAllExpenses() {
        return withPrimaryUser(user -> {
            List<ExpenseView> views = expenseService.getAllExpenses(user)
                    .stream()
                    .map(ExpenseView::new)
                    .toList();
            return Map.of("expenses", views, "currency", user.getDefaultCurrency());
        });
    }

    /**
     * Friend balances for the Lena Baki / Dena Baki dashboard section.
     * runningBalance encodes direction by sign: positive = they owe the user
     * (Lena Baki), negative = the user owes them (Dena Baki) -- same convention
     * as Friend.runningBalance itself, so no extra mapping is needed here.
     */
    @GetMapping("/friends")
    public ResponseEntity<?> getFriendBalances() {
        return withPrimaryUser(user -> {
            List<Friend> friends = friendService.getFriends(user);
            List<Map<String, Object>> views = friends.stream()
                    .map(f -> Map.<String, Object>of(
                            "name", f.getName(),
                            "balance", f.getRunningBalance()))
                    .toList();
            return Map.of("friends", views, "currency", user.getDefaultCurrency());
        });
    }

    /**
     * Shared "no user yet" handling: until the user has sent at least one
     * message to the Telegram bot, there's no AppUser row to read data for.
     */
    private ResponseEntity<?> withPrimaryUser(java.util.function.Function<AppUser, Object> fn) {
        return expenseService.getPrimaryUser()
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(fn.apply(user)))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "error", "no_user",
                        "message", "No data yet. Send a message to your Telegram bot first."
                )));
    }
}