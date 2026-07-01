package com.financetracker.bot.service;

import com.financetracker.bot.dto.ParsedLending;
import com.financetracker.bot.model.LendingEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses "/udhaar" command text into a ParsedLending.
 *
 * Expected format: "/udhaar <name> <amount> <diya|liya>"
 * Examples:
 *   "/udhaar Rohit 5000 diya"     -> lent 5000 to Rohit
 *   "/udhaar Priya 2000 liya"     -> borrowed 2000 from Priya
 *
 * "diya" (gave) and "liya" (took) map directly to LENT/BORROWED, matching the
 * plain-language toggle described in the design brief rather than a generic +/-.
 */
@Service
public class LendingMessageParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d+(?:\\.\\d{1,2})?");

    public ParsedLending parse(String commandBody) {
        if (commandBody == null || commandBody.isBlank()) {
            return new ParsedLending(null, null, null);
        }

        String text = commandBody.trim();
        String[] tokens = text.split("\\s+");
        if (tokens.length < 3) {
            return new ParsedLending(null, null, null);
        }

        String directionToken = tokens[tokens.length - 1].toLowerCase();
        LendingEntry.Direction direction = switch (directionToken) {
            case "diya", "de", "lent" -> LendingEntry.Direction.LENT;
            case "liya", "le", "borrowed" -> LendingEntry.Direction.BORROWED;
            default -> null;
        };
        if (direction == null) {
            return new ParsedLending(null, null, null);
        }

        String amountToken = tokens[tokens.length - 2];
        Matcher matcher = AMOUNT_PATTERN.matcher(amountToken);
        if (!matcher.matches()) {
            return new ParsedLending(null, null, null);
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountToken);
        } catch (NumberFormatException ex) {
            return new ParsedLending(null, null, null);
        }

        // Everything before the amount token is the person's name.
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < tokens.length - 2; i++) {
            if (nameBuilder.length() > 0) nameBuilder.append(' ');
            nameBuilder.append(tokens[i]);
        }
        String personName = nameBuilder.toString().trim();
        if (personName.isEmpty()) {
            return new ParsedLending(null, null, null);
        }

        return new ParsedLending(personName, amount, direction);
    }
}
