package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A split bill: a total amount, tagged with a category (Food, Travel,
 * Lena Baki, Dena Baki, etc. -- same simple tag system as regular expenses,
 * per the "just tag it like everything else" requirement), divided across
 * one or more Friends via SplitShare rows.
 */
@Entity
@Table(name = "split_expenses")
@Getter
@Setter
@NoArgsConstructor
public class SplitExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3, nullable = false)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 255)
    private String description;

    @Column(name = "date_incurred", nullable = false)
    private LocalDateTime dateIncurred = LocalDateTime.now();

    @OneToMany(mappedBy = "splitExpense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SplitShare> shares = new ArrayList<>();

    public void addShare(SplitShare share) {
        share.setSplitExpense(this);
        this.shares.add(share);
    }
}