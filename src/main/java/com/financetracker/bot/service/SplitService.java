package com.financetracker.bot.service;

import com.financetracker.bot.model.*;
import com.financetracker.bot.repository.CategoryRepository;
import com.financetracker.bot.repository.SplitExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Finalizes a completed split (from SplitSession) into a SplitExpense with
 * SplitShare rows, and updates each Friend's running balance accordingly.
 *
 * Balance direction: Friend.runningBalance means "how much they owe the user".
 *   - "Lena Baki" or any normal category (user paid, friend owes back): balance += share
 *   - "Dena Baki" (the user owes the friend their share): balance -= share
 */
@Service
public class SplitService {

    private static final String DENA_BAKI = "Dena Baki";

    private final SplitExpenseRepository splitExpenseRepository;
    private final CategoryRepository categoryRepository;
    private final FriendService friendService;

    public SplitService(SplitExpenseRepository splitExpenseRepository,
                         CategoryRepository categoryRepository,
                         FriendService friendService) {
        this.splitExpenseRepository = splitExpenseRepository;
        this.categoryRepository = categoryRepository;
        this.friendService = friendService;
    }

    /** Equal split, rounded to 2 decimals; any rounding remainder is added to the first friend's share. */
    public Map<Friend, BigDecimal> computeEqualShares(List<Friend> friends, BigDecimal totalAmount) {
        int n = friends.size();
        BigDecimal base = totalAmount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal allocated = base.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = totalAmount.subtract(allocated);

        java.util.LinkedHashMap<Friend, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = (i == 0) ? base.add(remainder) : base;
            result.put(friends.get(i), share);
        }
        return result;
    }

    @Transactional
    public SplitExpense finalizeSplit(AppUser user, BigDecimal totalAmount, String categoryName,
                                       String description, Map<Friend, BigDecimal> shares) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName, List.of())));

        SplitExpense splitExpense = new SplitExpense();
        splitExpense.setUser(user);
        splitExpense.setTotalAmount(totalAmount);
        splitExpense.setCurrency(user.getDefaultCurrency());
        splitExpense.setCategory(category);
        splitExpense.setDescription(description);

        boolean isDenaBaki = DENA_BAKI.equalsIgnoreCase(categoryName);

        for (Map.Entry<Friend, BigDecimal> entry : shares.entrySet()) {
            Friend friend = entry.getKey();
            BigDecimal shareAmount = entry.getValue();
            splitExpense.addShare(new SplitShare(friend, shareAmount));

            BigDecimal delta = isDenaBaki ? shareAmount.negate() : shareAmount;
            friendService.adjustBalance(friend, delta);
        }

        return splitExpenseRepository.save(splitExpense);
    }

    public List<SplitExpense> getAllSplits(AppUser user) {
        return splitExpenseRepository.findByUserOrderByDateIncurredDesc(user);
    }
}