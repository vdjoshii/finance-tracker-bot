package com.financetracker.bot.dto;

import com.financetracker.bot.model.Friend;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-chat in-progress state for the guided /split flow. Held in memory
 * (see SplitSessionStore) since this is a single-user personal bot --
 * no need for persistence across restarts mid-conversation.
 */
@Getter
@Setter
public class SplitSession {

    public enum Step {
        AWAITING_AMOUNT,
        AWAITING_CATEGORY,
        AWAITING_FRIENDS,
        AWAITING_SPLIT_TYPE,
        AWAITING_CUSTOM_AMOUNTS
    }

    private Step step = Step.AWAITING_AMOUNT;
    private BigDecimal totalAmount;
    private String categoryName;
    private String description;

    /** Friends selected so far, in selection order. */
    private List<Friend> selectedFriends = new ArrayList<>();

    /** Friend ID -> custom share amount, filled in during AWAITING_CUSTOM_AMOUNTS. */
    private Map<Long, BigDecimal> customAmounts = new LinkedHashMap<>();

    /** Which friend (by index into selectedFriends) we're currently asking the custom amount for. */
    private int customAmountIndex = 0;

    public boolean isAwaitingCustomAmountFor(int index) {
        return step == Step.AWAITING_CUSTOM_AMOUNTS && customAmountIndex == index;
    }
}