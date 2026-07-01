package com.financetracker.bot.repository;

import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    List<Friend> findByUserOrderByNameAsc(AppUser user);

    Optional<Friend> findByUserAndNameIgnoreCase(AppUser user, String name);
}