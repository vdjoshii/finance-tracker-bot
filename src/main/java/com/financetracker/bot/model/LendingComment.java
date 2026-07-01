package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single timestamped note on a LendingEntry, e.g. "Reminded him 12 June" or
 * "Partial 2000 received". Append-only by convention -- the bot only ever adds
 * new comments, never edits or deletes existing ones, to preserve the ledger's
 * margin-note history.
 */
@Entity
@Table(name = "lending_comments")
@Getter
@Setter
@NoArgsConstructor
public class LendingComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lending_entry_id", nullable = false)
    private LendingEntry lendingEntry;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LendingComment(String text) {
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}
