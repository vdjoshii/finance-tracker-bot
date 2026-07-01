package com.financetracker.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a person using the bot, identified by their Telegram chat ID.
 * Named AppUser (not User) to avoid colliding with MySQL's reserved "user" keyword
 * and Spring Security's own User type.
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "default_currency", length = 3, nullable = false)
    private String defaultCurrency = "INR";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AppUser(Long telegramChatId, String displayName) {
        this.telegramChatId = telegramChatId;
        this.displayName = displayName;
        this.defaultCurrency = "INR";
        this.createdAt = LocalDateTime.now();
    }
}
