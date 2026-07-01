package com.financetracker.bot.repository;

import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserAndDateIncurredBetweenOrderByDateIncurredDesc(
            AppUser user, LocalDateTime start, LocalDateTime end);

    List<Expense> findByUserOrderByDateIncurredDesc(AppUser user);

    List<Expense> findTop10ByUserOrderByDateLoggedDesc(AppUser user);

    @Query("""
            SELECT e.category.name, COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.user = :user AND e.dateIncurred BETWEEN :start AND :end
            GROUP BY e.category.name
            ORDER BY SUM(e.amount) DESC
            """)
    List<Object[]> sumByCategoryForPeriod(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.user = :user AND e.dateIncurred BETWEEN :start AND :end
            """)
    BigDecimal sumTotalForPeriod(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
