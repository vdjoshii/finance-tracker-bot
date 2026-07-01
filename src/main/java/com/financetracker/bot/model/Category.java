package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A spending category (e.g. "Food", "Transport").
 * Keywords drive the rule-based categorization engine: if an incoming
 * expense description contains one of these keywords (case-insensitive),
 * the expense is auto-assigned to this category.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    /**
     * Comma-separated keywords stored as a single column for simplicity.
     * Kept lowercase at write-time so matching is case-insensitive.
     */
    @ElementCollection
    @CollectionTable(name = "category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "keyword", length = 64)
    private List<String> keywords = new ArrayList<>();

    @Column(name = "is_custom", nullable = false)
    private boolean custom = false;

    public Category(String name, List<String> keywords) {
        this.name = name;
        this.keywords = keywords;
        this.custom = false;
    }
}
