package com.financetracker.bot.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks chats waiting for a typed name after tapping "Other" on the
 * Lena Baki / Dena Baki friend picker (see KeyboardFactory.friendOrOtherKeyboard).
 * A chat is in this map if and only if its next plain-text message should be
 * read as the name to attach as a comment, rather than treated as a new expense.
 */
@Component
public class PendingOtherNameStore {

    private final Map<Long, Long> pendingExpenseIdByChat = new ConcurrentHashMap<>();

    public void await(Long chatId, Long expenseId) {
        pendingExpenseIdByChat.put(chatId, expenseId);
    }

    public boolean isAwaiting(Long chatId) {
        return pendingExpenseIdByChat.containsKey(chatId);
    }

    public Long getExpenseId(Long chatId) {
        return pendingExpenseIdByChat.get(chatId);
    }

    public void clear(Long chatId) {
        pendingExpenseIdByChat.remove(chatId);
    }
}