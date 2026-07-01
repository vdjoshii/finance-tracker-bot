package com.financetracker.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds to the "telegram.bot" section of application.yml.
 * Actual token value comes from the TELEGRAM_BOT_TOKEN environment variable --
 * see application.yml and README for the [TELEGRAM_BOT_TOKEN] placeholder.
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter
@Setter
public class TelegramBotConfig {

    private String token;
    private String username;
}
