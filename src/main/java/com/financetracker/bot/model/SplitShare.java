package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One friend's share of a SplitExpense. When this is saved, the owning
 * Friend's runningBalance is adjusted by this amount (positive = they owe
 * the user more, i.e. Lena Baki; negative = the user owes them more,
 * i.e. Dena Baki) -- see SplitService for that update logic.
 */
@Entity
@Table(name = "split_shares")
@Getter
@Setter
@NoArgsConstructor
public class SplitShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "split_expense_id", nullable = false)
    private SplitExpense splitExpense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_id", nullable = false)
    private Friend friend;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column(nullable = false)
    private boolean settled = false;

    public SplitShare(Friend friend, BigDecimal shareAmount) {
        this.friend = friend;
        this.shareAmount = shareAmount;
        this.settled = false;
    }
}