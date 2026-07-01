package com.financetracker.bot.repository;

import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.LendingEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LendingEntryRepository extends JpaRepository<LendingEntry, Long> {

    List<LendingEntry> findByUserOrderByCreatedAtDesc(AppUser user);

    List<LendingEntry> findByUserAndStatusNotOrderByCreatedAtDesc(AppUser user, LendingEntry.Status status);

    List<LendingEntry> findByUserAndPersonNameIgnoreCaseOrderByCreatedAtDesc(AppUser user, String personName);
}
