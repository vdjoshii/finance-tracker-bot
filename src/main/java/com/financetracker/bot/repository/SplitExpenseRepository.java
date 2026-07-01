package com.financetracker.bot.repository;

import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.SplitExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SplitExpenseRepository extends JpaRepository<SplitExpense, Long> {

    List<SplitExpense> findByUserOrderByDateIncurredDesc(AppUser user);
}