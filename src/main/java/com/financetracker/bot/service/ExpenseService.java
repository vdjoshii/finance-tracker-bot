package com.financetracker.bot.service;

import com.financetracker.bot.dto.CategorizationResult;
import com.financetracker.bot.dto.ParsedExpense;
import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Category;
import com.financetracker.bot.model.Expense;
import com.financetracker.bot.repository.AppUserRepository;
import com.financetracker.bot.repository.CategoryRepository;
import com.financetracker.bot.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    private final MessageParser messageParser;
    private final CategorizationEngine categorizationEngine;
    private final ExpenseRepository expenseRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(MessageParser messageParser,
                          CategorizationEngine categorizationEngine,
                          ExpenseRepository expenseRepository,
                          AppUserRepository appUserRepository,
                          CategoryRepository categoryRepository) {
        this.messageParser = messageParser;
        this.categorizationEngine = categorizationEngine;
        this.expenseRepository = expenseRepository;
        this.appUserRepository = appUserRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Looks up an existing user by Telegram chat ID, creating a new profile on first contact. */
    @Transactional
    public AppUser getOrCreateUser(Long telegramChatId, String displayName) {
        return appUserRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> appUserRepository.save(new AppUser(telegramChatId, displayName)));
    }

    /**
     * Returns the first registered user. This app has no login and assumes a single
     * personal user (the person running the bot), so the dashboard has no concept of
     * "which user" beyond whoever has talked to the bot at least once via Telegram.
     */
    public Optional<AppUser> getPrimaryUser() {
        return appUserRepository.findAll().stream().findFirst();
    }

    /**
     * Outcome of attempting to log an expense from a raw message.
     * If parseFailed is true, no expense was saved -- the bot should ask the user to resend.
     */
    public record LogResult(boolean parseFailed, Expense expense, String categoryMethod) {}

    @Transactional
    public LogResult logExpense(AppUser user, String rawMessage) {
        ParsedExpense parsed = messageParser.parse(rawMessage);
        if (!parsed.isValid()) {
            return new LogResult(true, null, null);
        }

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(parsed.getAmount());
        expense.setCurrency(user.getDefaultCurrency());
        expense.setDescription(parsed.getDescription());
        expense.setRawMessage(rawMessage);
        expense.setDateIncurred(LocalDateTime.now());
        expense.setDateLogged(LocalDateTime.now());
        expense.setSourcePlatform(Expense.SourcePlatform.TELEGRAM);

        String method;
        if (parsed.getExplicitCategory() != null) {
            Category category = categoryRepository.findByNameIgnoreCase(parsed.getExplicitCategory())
                    .orElseGet(() -> categoryRepository.save(new Category(parsed.getExplicitCategory(), List.of())));
            expense.setCategory(category);
            expense.setCategoryConfidence(1.0);
            method = "explicit";
        } else {
            CategorizationResult result = categorizationEngine.categorize(user, parsed.getDescription());
            expense.setCategory(result.getCategory());
            expense.setCategoryConfidence(result.getConfidence());
            method = result.getMethod();
        }

        Expense saved = expenseRepository.save(expense);
        return new LogResult(false, saved, method);
    }

    public Optional<Expense> getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId);
    }

    @Transactional
    public Optional<Expense> addNoteToExpense(Long expenseId, String note) {
        return expenseRepository.findById(expenseId).map(expense -> {
            String existing = expense.getDescription();
            expense.setDescription(existing != null && !existing.isBlank()
                    ? existing + " | " + note
                    : note);
            return expenseRepository.save(expense);
        });
    }

    @Transactional
    public Optional<Expense> recategorize(Long expenseId, String newCategoryName) {
        return expenseRepository.findById(expenseId).map(expense -> {
            Category category = categoryRepository.findByNameIgnoreCase(newCategoryName)
                    .orElseGet(() -> categoryRepository.save(new Category(newCategoryName, List.of())));
            expense.setCategory(category);
            expense.setCategoryConfidence(1.0);
            expense.setEdited(true);
            return expenseRepository.save(expense);
        });
    }

    @Transactional
    public boolean deleteExpense(Long expenseId, AppUser user) {
        return expenseRepository.findById(expenseId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(e -> {
                    expenseRepository.delete(e);
                    return true;
                })
                .orElse(false);
    }

    /** Monthly summary: total spend and per-category breakdown for a given calendar month. */
    public record MonthSummary(BigDecimal total, List<CategoryTotal> byCategory) {}
    public record CategoryTotal(String categoryName, BigDecimal total) {}

    public MonthSummary getCurrentMonthSummary(AppUser user) {
        return getMonthSummaryFor(user, YearMonth.now());
    }

    public MonthSummary getMonthSummaryFor(AppUser user, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal total = expenseRepository.sumTotalForPeriod(user, start, end);
        List<CategoryTotal> byCategory = expenseRepository.sumByCategoryForPeriod(user, start, end)
                .stream()
                .map(row -> new CategoryTotal((String) row[0], (BigDecimal) row[1]))
                .toList();

        return new MonthSummary(total, byCategory);
    }

    /** Total spend for the calendar month immediately before the current one, for trend comparison. */
    public BigDecimal getPreviousMonthTotal(AppUser user) {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        BigDecimal total = getMonthSummaryFor(user, previousMonth).total();
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Expense> getRecentExpenses(AppUser user) {
        return expenseRepository.findTop10ByUserOrderByDateLoggedDesc(user);
    }

    /** Full expense history for the dashboard table, most recent first. */
    public List<Expense> getAllExpenses(AppUser user) {
        return expenseRepository.findByUserOrderByDateIncurredDesc(user);
    }
}