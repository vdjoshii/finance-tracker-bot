package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single Udhaari (lending/borrowing) record between the user and another person.
 * Mirrors the data model from the Khaata design brief: direction (lent vs borrowed),
 * amount, dates, status, and an append-only comment thread rather than a single
 * editable note field -- matching how this is actually tracked informally
 * (margin notes added over time in a physical ledger).
 */
@Entity
@Table(name = "lending_entries")
@Getter
@Setter
@NoArgsConstructor
public class LendingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Name of the other person involved -- not a system AppUser, just free text. */
    @Column(name = "person_name", nullable = false, length = 128)
    private String personName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Direction direction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency = "INR";

    @Column(name = "date_given", nullable = false)
    private LocalDate dateGiven;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.PENDING;

    /**
     * Running total already settled against this entry. Lets a person partially
     * repay across multiple updates without losing track of what's left outstanding.
     */
    @Column(name = "amount_settled", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountSettled = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "lendingEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<LendingComment> comments = new ArrayList<>();

    public enum Direction {
        /** "Diya" -- the user lent money to this person. */
        LENT,
        /** "Liya" -- the user borrowed money from this person. */
        BORROWED
    }

    public enum Status {
        PENDING, PARTIALLY_SETTLED, FULLY_SETTLED
    }

    public BigDecimal getOutstanding() {
        return amount.subtract(amountSettled);
    }

    public void addComment(LendingComment comment) {
        comment.setLendingEntry(this);
        this.comments.add(comment);
    }
}
