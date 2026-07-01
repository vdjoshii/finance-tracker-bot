package com.financetracker.bot.repository;

import com.financetracker.bot.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByTelegramChatId(Long telegramChatId);
}
