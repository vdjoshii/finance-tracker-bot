package com.financetracker.bot.config;

import com.financetracker.bot.bot.FinanceTrackerBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotRegistrationConfig {

    private final FinanceTrackerBot bot;

    @Autowired
    public TelegramBotRegistrationConfig(FinanceTrackerBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            throw new IllegalStateException(
                    "Failed to register Telegram bot. Check TELEGRAM_BOT_TOKEN is set correctly.", e);
        }
    }
}
