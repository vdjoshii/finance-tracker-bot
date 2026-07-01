package com.financetracker.bot.service;

import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Friend;
import com.financetracker.bot.repository.FriendRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class FriendService {

    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    @Transactional
    public Friend addFriend(AppUser user, String name) {
        return friendRepository.findByUserAndNameIgnoreCase(user, name)
                .orElseGet(() -> friendRepository.save(new Friend(user, name)));
    }

    public List<Friend> getFriends(AppUser user) {
        return friendRepository.findByUserOrderByNameAsc(user);
    }

    public Optional<Friend> findByName(AppUser user, String name) {
        return friendRepository.findByUserAndNameIgnoreCase(user, name);
    }

    public Optional<Friend> findById(Long friendId) {
        return friendRepository.findById(friendId);
    }

    /**
     * Adjusts a friend's running balance by the given signed amount.
     * Positive = they now owe the user more (Lena Baki increases).
     * Negative = the user now owes them more (Dena Baki increases).
     */
    @Transactional
    public void adjustBalance(Friend friend, BigDecimal delta) {
        friend.setRunningBalance(friend.getRunningBalance().add(delta));
        friendRepository.save(friend);
    }
}