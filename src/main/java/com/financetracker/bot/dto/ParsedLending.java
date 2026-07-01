package com.financetracker.bot.dto;

import com.financetracker.bot.model.LendingEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of parsing a "/udhaar" command, e.g. "/udhaar Rohit 5000 diya".
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedLending {

    private String personName;
    private BigDecimal amount;
    private LendingEntry.Direction direction;

    public boolean isValid() {
        return personName != null && !personName.isBlank() && amount != null && direction != null;
    }
}
