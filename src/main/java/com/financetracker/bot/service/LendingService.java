package com.financetracker.bot.service;

import com.financetracker.bot.dto.ParsedLending;
import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.LendingComment;
import com.financetracker.bot.model.LendingEntry;
import com.financetracker.bot.repository.LendingEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LendingService {

    private final LendingMessageParser parser;
    private final LendingEntryRepository lendingEntryRepository;

    public LendingService(LendingMessageParser parser, LendingEntryRepository lendingEntryRepository) {
        this.parser = parser;
        this.lendingEntryRepository = lendingEntryRepository;
    }

    public record LogResult(boolean parseFailed, LendingEntry entry) {}

    @Transactional
    public LogResult logEntry(AppUser user, String commandBody) {
        ParsedLending parsed = parser.parse(commandBody);
        if (!parsed.isValid()) {
            return new LogResult(true, null);
        }

        LendingEntry entry = new LendingEntry();
        entry.setUser(user);
        entry.setPersonName(parsed.getPersonName());
        entry.setAmount(parsed.getAmount());
        entry.setDirection(parsed.getDirection());
        entry.setCurrency(user.getDefaultCurrency());
        entry.setDateGiven(LocalDate.now());
        entry.setStatus(LendingEntry.Status.PENDING);
        entry.setAmountSettled(BigDecimal.ZERO);

        LendingEntry saved = lendingEntryRepository.save(entry);
        return new LogResult(false, saved);
    }

    @Transactional
    public Optional<LendingEntry> addComment(Long entryId, AppUser user, String commentText) {
        return lendingEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(entry -> {
                    entry.addComment(new LendingComment(commentText));
                    return lendingEntryRepository.save(entry);
                });
    }

    /**
     * Records a (possibly partial) settlement against an entry. Caps at the
     * outstanding amount so over-settling can't push the balance negative.
     */
    @Transactional
    public Optional<LendingEntry> settle(Long entryId, AppUser user, BigDecimal settledAmount) {
        return lendingEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(entry -> {
                    BigDecimal newSettled = entry.getAmountSettled().add(settledAmount);
                    if (newSettled.compareTo(entry.getAmount()) >= 0) {
                        entry.setAmountSettled(entry.getAmount());
                        entry.setStatus(LendingEntry.Status.FULLY_SETTLED);
                    } else {
                        entry.setAmountSettled(newSettled);
                        entry.setStatus(LendingEntry.Status.PARTIALLY_SETTLED);
                    }
                    return lendingEntryRepository.save(entry);
                });
    }

    public List<LendingEntry> getActiveEntries(AppUser user) {
        return lendingEntryRepository.findByUserAndStatusNotOrderByCreatedAtDesc(
                user, LendingEntry.Status.FULLY_SETTLED);
    }

    public List<LendingEntry> getAllEntries(AppUser user) {
        return lendingEntryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * "Kiska Kitna" -- net balance per person across all their active entries.
     * Positive means they owe the user; negative means the user owes them.
     */
    public record PersonBalance(String personName, BigDecimal netAmount) {}

    public List<PersonBalance> getNetBalancesByPerson(AppUser user) {
        List<LendingEntry> active = getActiveEntries(user);

        Map<String, BigDecimal> totals = active.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getPersonName().toLowerCase(),
                        Collectors.reducing(BigDecimal.ZERO,
                                e -> signedOutstanding(e),
                                BigDecimal::add)));

        // Preserve original display-case name from the most recent entry per person.
        Map<String, String> displayNames = active.stream()
                .collect(Collectors.toMap(
                        e -> e.getPersonName().toLowerCase(),
                        LendingEntry::getPersonName,
                        (a, b) -> a));

        return totals.entrySet().stream()
                .map(e -> new PersonBalance(displayNames.get(e.getKey()), e.getValue()))
                .filter(pb -> pb.netAmount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(PersonBalance::netAmount).reversed())
                .toList();
    }

    /** LENT outstanding is positive (they owe the user); BORROWED outstanding is negative. */
    private BigDecimal signedOutstanding(LendingEntry entry) {
        BigDecimal outstanding = entry.getOutstanding();
        return entry.getDirection() == LendingEntry.Direction.LENT ? outstanding : outstanding.negate();
    }
}
