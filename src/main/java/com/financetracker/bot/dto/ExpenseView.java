package com.financetracker.bot.dto;

import com.financetracker.bot.model.Expense;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Flat, JSON-friendly view of an Expense for the dashboard table.
 * Keeps the web layer decoupled from JPA entities (avoids lazy-loading
 * issues when serializing AppUser/Category associations directly).
 */
@Getter
public class ExpenseView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Long id;
    private final BigDecimal amount;
    private final String currency;
    private final String category;
    private final String description;
    private final String dateIncurred;
    private final boolean edited;

    public ExpenseView(Expense expense) {
        this.id = expense.getId();
        this.amount = expense.getAmount();
        this.currency = expense.getCurrency();
        this.category = expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized";
        this.description = expense.getDescription();
        this.dateIncurred = expense.getDateIncurred().format(DATE_FMT);
        this.edited = expense.isEdited();
    }
}
