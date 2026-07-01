package com.financetracker.bot.bot;

import com.financetracker.bot.config.TelegramBotConfig;
import com.financetracker.bot.dto.SplitSession;
import com.financetracker.bot.model.AppUser;
import com.financetracker.bot.model.Expense;
import com.financetracker.bot.model.Friend;
import com.financetracker.bot.model.LendingEntry;
import com.financetracker.bot.model.SplitExpense;
import com.financetracker.bot.service.ExpenseService;
import com.financetracker.bot.service.FriendService;
import com.financetracker.bot.service.KeyboardFactory;
import com.financetracker.bot.service.LendingService;
import com.financetracker.bot.service.PendingOtherNameStore;
import com.financetracker.bot.service.SplitService;
import com.financetracker.bot.service.SplitSessionStore;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FinanceTrackerBot extends TelegramLongPollingBot {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final String LENA_BAKI = "Lena Baki";
    private static final String DENA_BAKI = "Dena Baki";

    private final TelegramBotConfig config;
    private final ExpenseService expenseService;
    private final KeyboardFactory keyboardFactory;
    private final LendingService lendingService;
    private final FriendService friendService;
    private final SplitService splitService;
    private final SplitSessionStore splitSessionStore;
    private final PendingOtherNameStore pendingOtherNameStore;

    public FinanceTrackerBot(TelegramBotConfig config,
                             ExpenseService expenseService,
                             KeyboardFactory keyboardFactory,
                             LendingService lendingService,
                             FriendService friendService,
                             SplitService splitService,
                             SplitSessionStore splitSessionStore,
                             PendingOtherNameStore pendingOtherNameStore) {
        super(config.getToken());
        this.config = config;
        this.expenseService = expenseService;
        this.keyboardFactory = keyboardFactory;
        this.lendingService = lendingService;
        this.friendService = friendService;
        this.splitService = splitService;
        this.splitSessionStore = splitSessionStore;
        this.pendingOtherNameStore = pendingOtherNameStore;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception ex) {
            if (update.hasMessage()) {
                safeSend(update.getMessage().getChatId(),
                        "Something went wrong processing that message. Please try again.", null);
            }
        }
    }

    // ---------- Top-level message routing ----------

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        AppUser user = expenseService.getOrCreateUser(chatId, message.getChat().getFirstName());

        // Priority 1: "Other" name capture for Lena/Dena Baki
        if (pendingOtherNameStore.isAwaiting(chatId) && !text.startsWith("/")) {
            handleOtherNameEntered(chatId, user, text);
            return;
        }

        // Priority 2: in-progress /split conversation
        if (splitSessionStore.hasActiveSession(chatId) && !text.startsWith("/")) {
            handleSplitTextReply(chatId, user, text);
            return;
        }

        if (text.equalsIgnoreCase("/start")) { sendWelcome(chatId); return; }
        if (text.equalsIgnoreCase("/summary")) { sendMonthSummary(chatId, user); return; }
        if (text.equalsIgnoreCase("/recent")) { sendRecentExpenses(chatId, user); return; }
        if (text.equalsIgnoreCase("/help")) { sendHelp(chatId); return; }
        if (text.toLowerCase().startsWith("/udhaar ")) { handleUdhaar(chatId, user, text.substring(8).trim()); return; }
        if (text.equalsIgnoreCase("/kiskakitna")) { sendKiskaKitna(chatId, user); return; }
        if (text.equalsIgnoreCase("/udhaarlist")) { sendUdhaarList(chatId, user); return; }
        if (text.toLowerCase().startsWith("/note ")) { handleNote(chatId, user, text.substring(6).trim()); return; }
        if (text.toLowerCase().startsWith("/settle ")) { handleSettle(chatId, user, text.substring(8).trim()); return; }
        if (text.toLowerCase().startsWith("/addfriend ")) { handleAddFriend(chatId, user, text.substring(11).trim()); return; }
        if (text.equalsIgnoreCase("/friends")) { sendFriendsList(chatId, user); return; }
        if (text.equalsIgnoreCase("/split")) { startSplit(chatId, user); return; }

        ExpenseService.LogResult result = expenseService.logExpense(user, text);
        if (result.parseFailed()) {
            safeSend(chatId,
                    "I couldn't find an amount in that message. Try something like:\n"
                            + "\"450 groceries dmart\" or \"/log 450 Food Lunch with team\"", null);
            return;
        }

        Expense expense = result.expense();
        safeSend(chatId, buildConfirmation(expense, result.categoryMethod()),
                keyboardFactory.categoryCorrectionKeyboard(expense.getId()));
    }

    // ---------- Callback routing ----------

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        Long chatId = callback.getMessage().getChatId();
        AppUser user = expenseService.getOrCreateUser(chatId, callback.getFrom().getFirstName());

        if (data.startsWith("recat:")) {
            handleRecategorize(chatId, user, data);
        } else if (data.startsWith("bakifriend:")) {
            handleBakiFriendChosen(chatId, user, data);
        } else if (data.startsWith("bakiother:")) {
            handleBakiOtherChosen(chatId, data);
        } else if (data.startsWith("splitcat:")) {
            handleSplitCategoryChosen(chatId, user, data);
        } else if (data.startsWith("splitfriend:")) {
            handleSplitFriendToggled(chatId, user, data);
        } else if (data.equals("splitfriendsdone")) {
            handleSplitFriendsDone(chatId, user);
        } else if (data.startsWith("splittype:")) {
            handleSplitTypeChosen(chatId, user, data);
        }
    }

    /**
     * Called when any category button is tapped under a logged expense.
     * For Lena Baki and Dena Baki: recategorize first, then immediately show
     * the friend picker ("who is this tied to?").
     * For all other categories: recategorize and confirm, same as before.
     */
    private void handleRecategorize(Long chatId, AppUser user, String data) {
        String[] parts = data.split(":", 3);
        if (parts.length != 3) return;

        Long expenseId = Long.parseLong(parts[1]);
        String newCategory = parts[2];
        String displayName = "Uncategorized".equals(newCategory) ? "Other" : newCategory;

        expenseService.recategorize(expenseId, newCategory).ifPresentOrElse(
                expense -> {
                    if (LENA_BAKI.equals(newCategory) || DENA_BAKI.equals(newCategory)) {
                        List<Friend> friends = friendService.getFriends(user);
                        if (friends.isEmpty()) {
                            safeSend(chatId, "Tagged as *" + newCategory + "*.\n"
                                    + "Add friends with /addfriend <name> to track who this is with.", null);
                        } else {
                            safeSend(chatId,
                                    "Tagged as *" + newCategory + "*. Who is this with?",
                                    keyboardFactory.friendOrOtherKeyboard(expenseId, friends));
                        }
                    } else {
                        safeSend(chatId, "Updated to *" + displayName + "*.", null);
                    }
                },
                () -> safeSend(chatId, "Couldn't find that expense -- it may have been deleted.", null));
    }

    /**
     * A saved friend was chosen from the Lena/Dena Baki picker.
     * Updates that friend's running balance by the full expense amount.
     * Lena Baki: positive delta (they owe the user more).
     * Dena Baki: negative delta (the user owes them more).
     */
    private void handleBakiFriendChosen(Long chatId, AppUser user, String data) {
        String[] parts = data.split(":", 3);
        if (parts.length != 3) return;

        Long expenseId = Long.parseLong(parts[1]);
        Long friendId = Long.parseLong(parts[2]);

        expenseService.getExpenseById(expenseId).ifPresentOrElse(expense -> {
            friendService.findById(friendId).ifPresentOrElse(friend -> {
                boolean isDena = DENA_BAKI.equals(expense.getCategory().getName());
                BigDecimal delta = isDena ? expense.getAmount().negate() : expense.getAmount();
                friendService.adjustBalance(friend, delta);

                String sym = currencySymbol(expense.getCurrency());
                String direction = isDena
                        ? String.format(Locale.US, "You owe *%s* %s%.2f", friend.getName(), sym, expense.getAmount())
                        : String.format(Locale.US, "*%s* owes you %s%.2f", friend.getName(), sym, expense.getAmount());
                safeSend(chatId, direction + ". Balance updated.", null);
            }, () -> safeSend(chatId, "Couldn't find that friend.", null));
        }, () -> safeSend(chatId, "Couldn't find that expense.", null));
    }

    /**
     * "Other" was tapped -- put this chat into the name-awaiting state
     * and prompt the user to type a name.
     */
    private void handleBakiOtherChosen(Long chatId, String data) {
        String[] parts = data.split(":", 2);
        if (parts.length != 2) return;

        Long expenseId = Long.parseLong(parts[1]);
        pendingOtherNameStore.await(chatId, expenseId);
        safeSend(chatId, "Type the name to attach as a note:", null);
    }

    /**
     * The user typed a name after tapping "Other". Save it as a note on the
     * expense description field and clear the pending state.
     */
    private void handleOtherNameEntered(Long chatId, AppUser user, String text) {
        Long expenseId = pendingOtherNameStore.getExpenseId(chatId);
        pendingOtherNameStore.clear(chatId);

        expenseService.addNoteToExpense(expenseId, text).ifPresentOrElse(
                expense -> safeSend(chatId, "Noted: *" + text + "* added to the expense.", null),
                () -> safeSend(chatId, "Couldn't find that expense to add a note to.", null));
    }

    // ---------- Expense confirmation ----------

    private String buildConfirmation(Expense expense, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "Logged %s%.2f under *%s*",
                currencySymbol(expense.getCurrency()), expense.getAmount(), expense.getCategory().getName()));
        sb.append(" on ").append(expense.getDateIncurred().format(DATE_FMT)).append(".");
        if ("uncategorized".equals(method)) {
            sb.append("\nI wasn't sure which category fits -- tap one below to set it.");
        } else if ("historical".equals(method)) {
            sb.append("\n(Guessed from a similar past entry -- tap below if this is wrong.)");
        } else {
            sb.append("\nTap below to change the category if this is wrong.");
        }
        return sb.toString();
    }

    // ---------- Udhaar ----------

    private void handleUdhaar(Long chatId, AppUser user, String commandBody) {
        LendingService.LogResult result = lendingService.logEntry(user, commandBody);
        if (result.parseFailed()) {
            safeSend(chatId, "Couldn't read that udhaar entry. Try:\n"
                    + "\"/udhaar Rohit 5000 diya\" (you lent 5000 to Rohit)\n"
                    + "\"/udhaar Priya 2000 liya\" (you borrowed 2000 from Priya)", null);
            return;
        }
        LendingEntry entry = result.entry();
        String verb = entry.getDirection() == LendingEntry.Direction.LENT ? "Diya" : "Liya";
        String prep = entry.getDirection() == LendingEntry.Direction.LENT ? "to" : "from";
        safeSend(chatId, String.format(Locale.US, "%s %s%.2f %s *%s*. Entry #%d logged.\n\n"
                        + "Add a note: /note %d <text>\nMark settled: /settle %d <amount>",
                verb, currencySymbol(entry.getCurrency()), entry.getAmount(), prep,
                entry.getPersonName(), entry.getId(), entry.getId(), entry.getId()), null);
    }

    private void handleNote(Long chatId, AppUser user, String body) {
        String[] parts = body.split("\\s+", 2);
        if (parts.length < 2) { safeSend(chatId, "Usage: /note <entry id> <your note>", null); return; }
        try {
            Long entryId = Long.parseLong(parts[0]);
            lendingService.addComment(entryId, user, parts[1])
                    .ifPresentOrElse(
                            e -> safeSend(chatId, "Noted on entry #" + entryId + ".", null),
                            () -> safeSend(chatId, "Couldn't find entry #" + entryId + ".", null));
        } catch (NumberFormatException ex) {
            safeSend(chatId, "Usage: /note <entry id> <your note>", null);
        }
    }

    private void handleSettle(Long chatId, AppUser user, String body) {
        String[] parts = body.split("\\s+", 2);
        if (parts.length < 2) { safeSend(chatId, "Usage: /settle <entry id> <amount>", null); return; }
        try {
            Long entryId = Long.parseLong(parts[0]);
            BigDecimal amount = new BigDecimal(parts[1]);
            lendingService.settle(entryId, user, amount).ifPresentOrElse(
                    entry -> {
                        if (entry.getStatus() == LendingEntry.Status.FULLY_SETTLED) {
                            safeSend(chatId, "\uD83D\uDD36 *Bahi Stamp* -- entry #" + entryId
                                    + " with " + entry.getPersonName() + " is fully settled.", null);
                        } else {
                            safeSend(chatId, String.format(Locale.US,
                                    "Partial settlement recorded. %s%.2f still outstanding with %s.",
                                    currencySymbol(entry.getCurrency()), entry.getOutstanding(),
                                    entry.getPersonName()), null);
                        }
                    },
                    () -> safeSend(chatId, "Couldn't find entry #" + entryId + ".", null));
        } catch (NumberFormatException ex) {
            safeSend(chatId, "Usage: /settle <entry id> <amount>", null);
        }
    }

    private void sendKiskaKitna(Long chatId, AppUser user) {
        List<LendingService.PersonBalance> balances = lendingService.getNetBalancesByPerson(user);
        if (balances.isEmpty()) { safeSend(chatId, "No outstanding udhaar right now. Sab clear hai!", null); return; }
        String sym = currencySymbol(user.getDefaultCurrency());
        StringBuilder sb = new StringBuilder("*Kiska Kitna*\n\n");
        for (LendingService.PersonBalance pb : balances) {
            BigDecimal amt = pb.netAmount().abs();
            sb.append(pb.netAmount().compareTo(BigDecimal.ZERO) > 0
                    ? String.format(Locale.US, "%s owes you %s%.2f\n", pb.personName(), sym, amt)
                    : String.format(Locale.US, "You owe %s %s%.2f\n", pb.personName(), sym, amt));
        }
        safeSend(chatId, sb.toString(), null);
    }

    private void sendUdhaarList(Long chatId, AppUser user) {
        List<LendingEntry> active = lendingService.getActiveEntries(user);
        if (active.isEmpty()) { safeSend(chatId, "No active udhaar entries. Sab clear hai!", null); return; }
        String sym = currencySymbol(user.getDefaultCurrency());
        StringBuilder sb = new StringBuilder("*Active udhaar entries*\n\n");
        for (LendingEntry e : active) {
            String verb = e.getDirection() == LendingEntry.Direction.LENT ? "Diya to" : "Liya from";
            sb.append(String.format(Locale.US, "#%d  %s  %s%.2f  (%s outstanding %s%.2f)\n",
                    e.getId(), verb, sym, e.getAmount(), e.getPersonName(), sym, e.getOutstanding()));
        }
        safeSend(chatId, sb.toString(), null);
    }

    // ---------- Friends ----------

    private void handleAddFriend(Long chatId, AppUser user, String name) {
        if (name.isBlank()) { safeSend(chatId, "Usage: /addfriend <name>", null); return; }
        Friend friend = friendService.addFriend(user, name);
        safeSend(chatId, "Added *" + friend.getName() + "* to your friends list.", null);
    }

    private void sendFriendsList(Long chatId, AppUser user) {
        List<Friend> friends = friendService.getFriends(user);
        if (friends.isEmpty()) { safeSend(chatId, "No friends saved yet. Add one with /addfriend <name>.", null); return; }
        String sym = currencySymbol(user.getDefaultCurrency());
        StringBuilder sb = new StringBuilder("*Your friends*\n\n");
        for (Friend f : friends) {
            BigDecimal bal = f.getRunningBalance();
            if (bal.compareTo(BigDecimal.ZERO) == 0) sb.append(f.getName()).append(" -- settled up\n");
            else if (bal.compareTo(BigDecimal.ZERO) > 0)
                sb.append(String.format(Locale.US, "%s owes you %s%.2f\n", f.getName(), sym, bal));
            else
                sb.append(String.format(Locale.US, "You owe %s %s%.2f\n", f.getName(), sym, bal.abs()));
        }
        safeSend(chatId, sb.toString(), null);
    }

    // ---------- /split flow ----------

    private void startSplit(Long chatId, AppUser user) {
        List<Friend> friends = friendService.getFriends(user);
        if (friends.isEmpty()) { safeSend(chatId, "Add at least one friend first: /addfriend <name>", null); return; }
        splitSessionStore.start(chatId);
        safeSend(chatId, "Let's split a bill. What's the total amount?", null);
    }

    private void handleSplitTextReply(Long chatId, AppUser user, String text) {
        SplitSession session = splitSessionStore.get(chatId);
        if (session == null) return;
        switch (session.getStep()) {
            case AWAITING_AMOUNT -> handleSplitAmountEntered(chatId, session, text);
            case AWAITING_CUSTOM_AMOUNTS -> handleSplitCustomAmountEntered(chatId, user, session, text);
            default -> safeSend(chatId, "Use the buttons above to continue, or /split to start over.", null);
        }
    }

    private void handleSplitAmountEntered(Long chatId, SplitSession session, String text) {
        try {
            session.setTotalAmount(new BigDecimal(text.trim()));
            session.setStep(SplitSession.Step.AWAITING_CATEGORY);
            safeSend(chatId, "Got it. What category is this?", keyboardFactory.splitCategoryKeyboard());
        } catch (NumberFormatException ex) {
            safeSend(chatId, "That doesn't look like a number. What's the total amount?", null);
        }
    }

    private void handleSplitCategoryChosen(Long chatId, AppUser user, String data) {
        SplitSession session = splitSessionStore.get(chatId);
        if (session == null || session.getStep() != SplitSession.Step.AWAITING_CATEGORY) return;
        String categoryName = data.substring("splitcat:".length());
        session.setCategoryName(categoryName);
        session.setStep(SplitSession.Step.AWAITING_FRIENDS);
        safeSend(chatId, "Tagged as *" + categoryName + "*. Who's splitting this with you?",
                keyboardFactory.friendMultiSelectKeyboard(friendService.getFriends(user), List.of()));
    }

    private void handleSplitFriendToggled(Long chatId, AppUser user, String data) {
        SplitSession session = splitSessionStore.get(chatId);
        if (session == null || session.getStep() != SplitSession.Step.AWAITING_FRIENDS) return;
        Long friendId = Long.parseLong(data.substring("splitfriend:".length()));
        friendService.findById(friendId).ifPresent(friend -> {
            List<Friend> selected = session.getSelectedFriends();
            if (selected.contains(friend)) selected.remove(friend);
            else selected.add(friend);
        });
        List<Long> selectedIds = session.getSelectedFriends().stream().map(Friend::getId).toList();
        safeSend(chatId, "Tap to select/deselect, then hit Done.",
                keyboardFactory.friendMultiSelectKeyboard(friendService.getFriends(user), selectedIds));
    }

    private void handleSplitFriendsDone(Long chatId, AppUser user) {
        SplitSession session = splitSessionStore.get(chatId);
        if (session == null || session.getStep() != SplitSession.Step.AWAITING_FRIENDS) return;
        if (session.getSelectedFriends().isEmpty()) { safeSend(chatId, "Pick at least one friend before tapping Done.", null); return; }
        session.setStep(SplitSession.Step.AWAITING_SPLIT_TYPE);
        safeSend(chatId, "Equal split, or custom amounts per person?", keyboardFactory.splitTypeKeyboard());
    }

    private void handleSplitTypeChosen(Long chatId, AppUser user, String data) {
        SplitSession session = splitSessionStore.get(chatId);
        if (session == null || session.getStep() != SplitSession.Step.AWAITING_SPLIT_TYPE) return;
        if ("equal".equals(data.substring("splittype:".length()))) {
            finalizeAndConfirmSplit(chatId, user, session,
                    splitService.computeEqualShares(session.getSelectedFriends(), session.getTotalAmount()));
        } else {
            session.setStep(SplitSession.Step.AWAITING_CUSTOM_AMOUNTS);
            session.setCustomAmountIndex(0);
            safeSend(chatId, "How much is *" + session.getSelectedFriends().get(0).getName() + "*'s share?", null);
        }
    }

    private void handleSplitCustomAmountEntered(Long chatId, AppUser user, SplitSession session, String text) {
        try {
            Friend friend = session.getSelectedFriends().get(session.getCustomAmountIndex());
            session.getCustomAmounts().put(friend.getId(), new BigDecimal(text.trim()));
            session.setCustomAmountIndex(session.getCustomAmountIndex() + 1);
            if (session.getCustomAmountIndex() < session.getSelectedFriends().size()) {
                safeSend(chatId, "How much is *"
                        + session.getSelectedFriends().get(session.getCustomAmountIndex()).getName() + "*'s share?", null);
                return;
            }
            Map<Friend, BigDecimal> shares = new LinkedHashMap<>();
            for (Friend f : session.getSelectedFriends()) shares.put(f, session.getCustomAmounts().get(f.getId()));
            finalizeAndConfirmSplit(chatId, user, session, shares);
        } catch (NumberFormatException ex) {
            safeSend(chatId, "That doesn't look like a number. Try again.", null);
        }
    }

    private void finalizeAndConfirmSplit(Long chatId, AppUser user, SplitSession session, Map<Friend, BigDecimal> shares) {
        SplitExpense splitExpense = splitService.finalizeSplit(
                user, session.getTotalAmount(), session.getCategoryName(), session.getDescription(), shares);
        splitSessionStore.clear(chatId);
        String sym = currencySymbol(user.getDefaultCurrency());
        StringBuilder sb = new StringBuilder(String.format(Locale.US,
                "Split %s%.2f under *%s* recorded.\n\n", sym, splitExpense.getTotalAmount(),
                splitExpense.getCategory().getName()));
        for (Map.Entry<Friend, BigDecimal> e : shares.entrySet())
            sb.append(String.format(Locale.US, "%s: %s%.2f\n", e.getKey().getName(), sym, e.getValue()));
        safeSend(chatId, sb.toString(), null);
    }

    // ---------- Reporting ----------

    private void sendWelcome(Long chatId) {
        safeSend(chatId, """
                Welcome to Khaata!

                Log an expense:
                "450 groceries dmart" or "Spent 200 on auto"

                After logging, tap a category button -- tapping
                Lena Baki or Dena Baki will ask who it's with.

                Split bills with friends:
                /addfriend <name> - save a friend
                /friends - see friends and balances
                /split - guided bill split

                Udhaar tracker:
                /udhaar <name> <amount> diya|liya
                /kiskakitna - net balance per person
                /udhaarlist - active entries
                /note <id> <text> - add a note
                /settle <id> <amount> - record settlement

                Other:
                /summary - this month's spending
                /recent - last 10 entries
                /help - show this again
                """, null);
    }

    private void sendHelp(Long chatId) { sendWelcome(chatId); }

    private void sendMonthSummary(Long chatId, AppUser user) {
        ExpenseService.MonthSummary summary = expenseService.getCurrentMonthSummary(user);
        if (summary.byCategory().isEmpty()) { safeSend(chatId, "No expenses logged yet this month.", null); return; }
        String sym = currencySymbol(user.getDefaultCurrency());
        StringBuilder sb = new StringBuilder("*This month's spending*\n\n");
        for (ExpenseService.CategoryTotal ct : summary.byCategory())
            sb.append(String.format(Locale.US, "%-15s %s%.2f\n", ct.categoryName(), sym, ct.total()));
        sb.append(String.format(Locale.US, "\n*Total: %s%.2f*", sym, summary.total()));
        safeSend(chatId, sb.toString(), null);
    }

    private void sendRecentExpenses(Long chatId, AppUser user) {
        List<Expense> recent = expenseService.getRecentExpenses(user);
        if (recent.isEmpty()) { safeSend(chatId, "No expenses logged yet.", null); return; }
        StringBuilder sb = new StringBuilder("*Last entries*\n\n");
        for (Expense e : recent)
            sb.append(String.format(Locale.US, "#%d  %s%.2f  %s  (%s)\n",
                    e.getId(), currencySymbol(e.getCurrency()), e.getAmount(),
                    e.getCategory().getName(), e.getDateIncurred().format(DATE_FMT)));
        safeSend(chatId, sb.toString(), null);
    }

    private String currencySymbol(String code) { return "INR".equals(code) ? "\u20B9" : code + " "; }

    private void safeSend(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        if (keyboard != null) message.setReplyMarkup(keyboard);
        try { execute(message); }
        catch (TelegramApiException ex) { System.err.println("Failed to send: " + ex.getMessage()); }
    }
}