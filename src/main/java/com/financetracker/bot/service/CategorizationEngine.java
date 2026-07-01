package com.financetracker.bot.service;

import com.financetracker.bot.dto.CategorizationResult;
import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Category;
import com.financetracker.bot.model.Expense;
import com.financetracker.bot.repository.CategoryRepository;
import com.financetracker.bot.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements the layered categorization strategy from the design document,
 * minus the AI-driven layer (explicitly out of scope for this build):
 *
 *   1. Explicit user input        - handled upstream in MessageParser/ExpenseService
 *   2. Keyword/rule-based match   - this class, against Category.keywords
 *   3. Historical pattern lookup  - reuse this user's most recent categorization
 *                                   for a similar description
 *   4. Uncategorized fallback     - if nothing matches
 */
@Service
public class CategorizationEngine {

    private static final String UNCATEGORIZED = "Uncategorized";

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public CategorizationEngine(CategoryRepository categoryRepository,
                                 ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    public CategorizationResult categorize(AppUser user, String description) {
        if (description == null || description.isBlank()) {
            return uncategorized();
        }

        String normalized = description.toLowerCase();

        // Layer 2: keyword match
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            for (String keyword : category.getKeywords()) {
                if (keyword != null && !keyword.isBlank()
                        && normalized.contains(keyword.toLowerCase())) {
                    return new CategorizationResult(category, 1.0, "keyword");
                }
            }
        }

        // Layer 3: historical pattern - check this user's recent expenses for
        // a description that shares a significant word with the new one.
        List<Expense> recent = expenseRepository.findTop10ByUserOrderByDateLoggedDesc(user);
        Optional<Category> historical = findHistoricalMatch(normalized, recent);
        if (historical.isPresent()) {
            return new CategorizationResult(historical.get(), 0.6, "historical");
        }

        // Layer 4: fallback
        return uncategorized();
    }

    private Optional<Category> findHistoricalMatch(String normalizedDescription, List<Expense> recent) {
        List<String> newWords = significantWords(normalizedDescription);
        if (newWords.isEmpty()) {
            return Optional.empty();
        }

        // Count category votes among past expenses sharing at least one significant word.
        Map<Category, Long> votes = recent.stream()
                .filter(e -> e.getCategory() != null && e.getDescription() != null)
                .filter(e -> significantWords(e.getDescription().toLowerCase())
                        .stream().anyMatch(newWords::contains))
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.counting()));

        return votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** Filters out very short/common words so matching focuses on merchant/item names. */
    private List<String> significantWords(String text) {
        return List.of(text.split("\\s+")).stream()
                .filter(w -> w.length() > 3)
                .collect(Collectors.toList());
    }

    private CategorizationResult uncategorized() {
        Category fallback = categoryRepository.findByNameIgnoreCase(UNCATEGORIZED)
                .orElseGet(() -> categoryRepository.save(new Category(UNCATEGORIZED, List.of())));
        return new CategorizationResult(fallback, 0.0, "uncategorized");
    }
}
