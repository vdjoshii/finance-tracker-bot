package com.financetracker.bot.dto;

import com.financetracker.bot.model.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategorizationResult {

    private Category category;

    /** 1.0 for explicit/keyword matches, lower for historical pattern guesses, null-ish 0.0 if uncategorized. */
    private double confidence;

    private String method; // "explicit" | "keyword" | "historical" | "uncategorized"
}
