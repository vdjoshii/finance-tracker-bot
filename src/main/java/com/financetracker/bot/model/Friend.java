package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A saved friend for the split-expense feature (/addfriend, /split).
 * runningBalance is the net Lena Baki / Dena Baki figure with this person:
 * positive means they owe the user, negative means the user owes them.
 * Updated automatically whenever a SplitShare involving this friend is recorded.
 */
@Entity
@Table(name = "friends", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "running_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal runningBalance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Friend(AppUser user, String name) {
        this.user = user;
        this.name = name;
        this.runningBalance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friend other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}