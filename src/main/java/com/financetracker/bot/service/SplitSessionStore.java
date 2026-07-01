package com.financetracker.bot.service;

import com.financetracker.bot.dto.SplitSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks in-progress /split conversations per Telegram chat ID. A chat
 * has an active session if and only if it's present in this map -- this
 * is also how the bot decides whether an incoming plain-text message
 * should be routed into the split flow instead of treated as a new expense.
 */
@Component
public class SplitSessionStore {

    private final Map<Long, SplitSession> sessions = new ConcurrentHashMap<>();

    public boolean hasActiveSession(Long chatId) {
        return sessions.containsKey(chatId);
    }

    public SplitSession start(Long chatId) {
        SplitSession session = new SplitSession();
        sessions.put(chatId, session);
        return session;
    }

    public SplitSession get(Long chatId) {
        return sessions.get(chatId);
    }

    public void clear(Long chatId) {
        sessions.remove(chatId);
    }
}