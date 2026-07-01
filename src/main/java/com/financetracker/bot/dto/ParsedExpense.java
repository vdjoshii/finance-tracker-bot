package com.financetracker.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of parsing a raw incoming message. If amount is null, parsing
 * failed and the caller should ask the user to resend with a clear number.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedExpense {

    private BigDecimal amount;

    /** Category name if the user stated one explicitly (e.g. "/log 450 Food lunch"), else null. */
    private String explicitCategory;

    /** Free-text description/merchant remaining after stripping the amount and command tokens. */
    private String description;

    public boolean isValid() {
        return amount != null;
    }
}
