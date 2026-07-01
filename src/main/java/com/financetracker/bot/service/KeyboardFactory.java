package com.financetracker.bot.service;

import com.financetracker.bot.model.Friend;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    private record QuickCategory(String label, String categoryName) {}

    private static final List<QuickCategory> QUICK_CATEGORIES = List.of(
            new QuickCategory("\uD83C\uDF7D\uFE0F Food", "Food"),
            new QuickCategory("\uD83D\uDE95 Transport", "Transport"),
            new QuickCategory("\uD83D\uDED2 Shopping", "Shopping"),
            new QuickCategory("\uD83E\uDDFE Bills", "Bills"),
            new QuickCategory("\uD83C\uDFAC Entertainment", "Entertainment"),
            new QuickCategory("\uD83D\uDC8A Health", "Health"),
            new QuickCategory("\uD83E\uDD1D Lena Baki", "Lena Baki"),
            new QuickCategory("\uD83D\uDCB8 Dena Baki", "Dena Baki"),
            new QuickCategory("\u2753 Other", "Uncategorized")
    );

    public InlineKeyboardMarkup categoryCorrectionKeyboard(Long expenseId) {
        List<List<InlineKeyboardButton>> rows = twoColumnRows(QUICK_CATEGORIES,
                qc -> qc.label(), qc -> "recat:" + expenseId + ":" + qc.categoryName());
        return markup(rows);
    }

    /** Category picker used during the /split flow -- same tags, different callback prefix. */
    public InlineKeyboardMarkup splitCategoryKeyboard() {
        List<List<InlineKeyboardButton>> rows = twoColumnRows(QUICK_CATEGORIES,
                qc -> qc.label(), qc -> "splitcat:" + qc.categoryName());
        return markup(rows);
    }

    /**
     * Multi-select friend picker. Selected friend IDs are passed in so already-picked
     * friends render with a checkmark, letting the same message be edited in place
     * as the user taps multiple friends before confirming.
     */
    public InlineKeyboardMarkup friendMultiSelectKeyboard(List<Friend> allFriends, List<Long> selectedIds) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Friend friend : allFriends) {
            boolean selected = selectedIds.contains(friend.getId());
            String label = (selected ? "\u2705 " : "") + friend.getName();
            rows.add(List.of(button(label, "splitfriend:" + friend.getId())));
        }
        rows.add(List.of(button("\u27A1\uFE0F Done picking", "splitfriendsdone")));
        return markup(rows);
    }

    public InlineKeyboardMarkup splitTypeKeyboard() {
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(button("\u2696\uFE0F Equal split", "splittype:equal")),
                List.of(button("\u270F\uFE0F Custom amounts", "splittype:custom"))
        );
        return markup(rows);
    }

    /**
     * Single-select "who is this Lena Baki / Dena Baki tied to" picker, shown
     * after tagging a regular expense with one of those two categories.
     * One button per saved friend (two-column grid) plus a final "Other" row
     * for someone not on the saved list.
     */
    public InlineKeyboardMarkup friendOrOtherKeyboard(Long expenseId, List<Friend> friends) {
        List<List<InlineKeyboardButton>> rows = twoColumnRows(friends,
                Friend::getName, f -> "bakifriend:" + expenseId + ":" + f.getId());
        rows.add(List.of(button("\u2753 Other", "bakiother:" + expenseId)));
        return markup(rows);
    }

    private <T> List<List<InlineKeyboardButton>> twoColumnRows(
            List<T> items, java.util.function.Function<T, String> labelFn,
            java.util.function.Function<T, String> callbackFn) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 2) {
            T first = items.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button(labelFn.apply(first), callbackFn.apply(first)));
            if (i + 1 < items.size()) {
                T second = items.get(i + 1);
                row.add(button(labelFn.apply(second), callbackFn.apply(second)));
            }
            rows.add(row);
        }
        return rows;
    }

    private InlineKeyboardMarkup markup(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(label);
        button.setCallbackData(callbackData);
        return button;
    }
}