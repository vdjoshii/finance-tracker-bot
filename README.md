# Finance Tracker Telegram Bot

A personal finance tracker you operate entirely through Telegram chat messages.
Send "450 groceries dmart" and it logs, categorizes, and confirms the expense
back to you. Built with Java 17, Spring Boot 3, MySQL, and the Telegram Bot API.

This implements the design from the conceptual framework document, using:
- **Telegram** (not WhatsApp) for the messaging layer
- **Keyword/rule-based categorization only** -- no external AI calls
- **MySQL** for persistence

---

## 1. Prerequisites

- Java 17 or later (`java -version`)
- Maven 3.8+ (`mvn -version`)
- MySQL Server running locally (you likely already have this from FinFluencer Toolkit)
- A Telegram account

---

## 2. Create your Telegram bot (5 minutes)

1. Open Telegram and search for **@BotFather**.
2. Send `/newbot` and follow the prompts:
   - Choose a display name (e.g. "My Expense Tracker")
   - Choose a username ending in `bot` (e.g. `viraj_expense_tracker_bot`)
3. BotFather replies with an API token that looks like:
   `123456789:AAFr8X9k...` -- this is your `[TELEGRAM_BOT_TOKEN]`.
4. Note the username you chose -- this is your `[TELEGRAM_BOT_USERNAME]`.

Keep the token private. Anyone with it can control your bot.

---

## 3. Set up MySQL

The application will auto-create the `finance_tracker` database and all
tables on first run (via `createDatabaseIfNotExist=true` and Hibernate's
`ddl-auto: update`), so manual setup is optional.

If you'd rather create it explicitly first:

```bash
mysql -u root -p < sql/setup.sql
```

---

## 4. Configure credentials

The app reads credentials from environment variables (preferred) with
placeholders as fallback defaults in `application.yml`. Set these before
running:

```bash
export TELEGRAM_BOT_TOKEN="[TELEGRAM_BOT_TOKEN]"
export TELEGRAM_BOT_USERNAME="[TELEGRAM_BOT_USERNAME]"
export MYSQL_USER="root"
export MYSQL_PASSWORD="[root123]"
```

On Windows (PowerShell):
```powershell
$env:TELEGRAM_BOT_TOKEN="[TELEGRAM_BOT_TOKEN]"
$env:TELEGRAM_BOT_USERNAME="[TELEGRAM_BOT_USERNAME]"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="[root123]"
```

Never commit real values for these into version control.

---

## 5. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

On first startup the app will:
1. Connect to MySQL and create the schema (`app_users`, `categories`,
   `category_keywords`, `expenses` tables).
2. Seed seven default categories with keyword lists (Food, Transport,
   Shopping, Bills, Entertainment, Health, Uncategorized).
3. Register the bot with Telegram and begin long-polling for messages.

There is no public webhook URL needed -- long polling means the bot reaches
out to Telegram, not the other way around, so this runs fine on a laptop
behind NAT/no public IP.

---

## 6. Using the bot

Open Telegram, find your bot by its username, and send `/start`.

| You send | What happens |
|---|---|
| `450 groceries dmart` | Logs ₹450 under Food (keyword match on "groceries") |
| `Spent 200 on auto` | Logs ₹200 under Transport (keyword match on "auto") |
| `/log 450 Food Lunch with team` | Logs ₹450 under Food explicitly, description "Lunch with team" |
| `/summary` | Shows this month's total and per-category breakdown |
| `/recent` | Shows your last 10 logged expenses with IDs |
| Tap a button under a confirmation | Recategorizes that expense in one tap |

If no amount can be found in your message, the bot will ask you to resend
it rather than guessing or silently dropping it.

---

## 7. Project structure

```
src/main/java/com/financetracker/bot/
  bot/            FinanceTrackerBot.java       - Telegram update handling, intent routing
  config/         TelegramBotConfig.java        - credential binding
                  TelegramBotRegistrationConfig.java - registers bot on startup
                  DefaultCategorySeeder.java    - seeds default categories/keywords
  model/          AppUser.java, Category.java, Expense.java - JPA entities
  repository/     Spring Data repositories
  service/        MessageParser.java            - extracts amount/category/description
                  CategorizationEngine.java     - keyword + historical-pattern matching
                  ExpenseService.java           - core orchestration logic
                  KeyboardFactory.java          - inline correction buttons
  dto/            ParsedExpense.java, CategorizationResult.java
sql/setup.sql                                   - optional manual DB setup
```

---

## 8. Extending later

This build deliberately omits two things from the original design doc, both
straightforward to add when needed:

- **AI-driven categorization fallback**: `CategorizationEngine.categorize()`
  has a clear seam where a 4th layer (a call to an LLM API) could slot in
  before the "uncategorized" fallback, for descriptions that match no
  keyword and no historical pattern.
- **WhatsApp support**: the bot-specific logic lives only in
  `FinanceTrackerBot.java` and `KeyboardFactory.java`. A `WhatsAppBotAdapter`
  implementing the same intent-routing calls into `ExpenseService` would let
  both platforms share all business logic untouched.

---

## 9. Customizing categories and keywords

Categories and their keywords are stored in the database (`categories` and
`category_keywords` tables), not hardcoded, so you can edit them directly in
MySQL without redeploying:

```sql
USE finance_tracker;
SELECT * FROM categories;
INSERT INTO category_keywords (category_id, keyword) VALUES (1, 'starbucks');
```

The `DefaultCategorySeeder` only runs once -- if the `categories` table
already has rows, it does nothing, so your edits are safe across restarts.
