package com.financetracker.bot.service;

import com.financetracker.bot.dto.ParsedExpense;
import com.financetracker.bot.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses free-text or structured ("/log") messages into a ParsedExpense.
 *
 * Supported formats:
 *   "/log 450 Food Lunch with team"   -> structured: amount, category, description
 *   "450 groceries dmart"             -> shorthand: amount first, rest is free text
 *   "Spent 450 on groceries at DMart" -> free text: amount found anywhere in the message
 *
 * This is a deliberately simple, explainable parser (regex + a category-name lookup),
 * matching the "keyword/rule-based only" design decision -- no external NLP/AI calls.
 */
@Service
public class MessageParser {

    // Matches the first standalone number in the message, allowing decimals
    // and optional thousands separators (e.g. "1,200.50").
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)");

    private final CategoryRepository categoryRepository;

    public MessageParser(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ParsedExpense parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new ParsedExpense(null, null, null);
        }

        String text = rawMessage.trim();

        // Strip a leading "/log" command token if present.
        if (text.toLowerCase().startsWith("/log")) {
            text = text.substring(4).trim();
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new ParsedExpense(null, null, null);
        }

        String amountToken = matcher.group(1);
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountToken.replace(",", ""));
        } catch (NumberFormatException ex) {
            return new ParsedExpense(null, null, null);
        }

        // Everything except the matched amount token, with filler words removed.
        String remainder = text.substring(0, matcher.start()) + text.substring(matcher.end());
        remainder = remainder
                .replaceAll("(?i)\\bspent\\b", "")
                .replaceAll("(?i)\\bon\\b", "")
                .replaceAll("(?i)\\bat\\b", "")
                .replaceAll("(?i)\\bfor\\b", "")
                .replaceAll("\\s{2,}", " ")
                .trim();

        // If the first remaining word matches a known category name exactly,
        // treat it as an explicit category and the rest as description.
        String explicitCategory = null;
        if (!remainder.isEmpty()) {
            String firstWord = remainder.split("\\s+")[0];
            List<String> knownNames = categoryRepository.findAll()
                    .stream()
                    .map(c -> c.getName().toLowerCase())
                    .toList();
            if (knownNames.contains(firstWord.toLowerCase())) {
                explicitCategory = firstWord;
                remainder = remainder.substring(firstWord.length()).trim();
            }
        }

        String description = remainder.isBlank() ? null : remainder;

        return new ParsedExpense(amount, explicitCategory, description);
    }
}
