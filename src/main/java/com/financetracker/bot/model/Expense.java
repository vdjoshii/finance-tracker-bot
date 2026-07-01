package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single logged expense. Mirrors the data model from the design document:
 * amount, category, date, description, plus audit fields (raw message, source platform).
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * 0.0-1.0 confidence score from the categorization engine.
     * Null for explicit user-stated categories (treated as full confidence).
     */
    @Column(name = "category_confidence")
    private Double categoryConfidence;

    @Column(length = 255)
    private String description;

    @Column(name = "raw_message", length = 1000, nullable = false)
    private String rawMessage;

    @Column(name = "date_incurred", nullable = false)
    private LocalDateTime dateIncurred;

    @Column(name = "date_logged", nullable = false)
    private LocalDateTime dateLogged;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", nullable = false, length = 16)
    private SourcePlatform sourcePlatform = SourcePlatform.TELEGRAM;

    @Column(nullable = false)
    private boolean edited = false;

    public enum SourcePlatform {
        TELEGRAM, WHATSAPP
    }
}
