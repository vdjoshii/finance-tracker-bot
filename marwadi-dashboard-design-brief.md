
# Design Brief: "Khaata" — A Gen Z Marwadi Finance Dashboard

## Overall Theme & Vibe

The thesis: **a digital khaata book that your dadi would recognize and your friends would screenshot.**

Marwadi business culture already has a strong, specific visual and linguistic identity, ledger books (bahi-khata), red-and-gold hisaab registers, the unmistakable confidence of "paisa hi paisa hoga" — and the goal here isn't to dilute that into generic "ethnic pattern + gradient" design, but to take the actual material culture (lattice work, ledger typography, seal stamps) and run it through a Gen Z product lens: bold, high-contrast, a little irreverent, built for screenshots and stories.

The vibe is **"heritage flex, not heritage costume."** Nothing should feel like a tourist-brochure version of Rajasthan. Instead: the dashboard should feel like it was designed by someone who grew up doing hisaab-kitab with their nana and also grew up on Instagram. Confident, business-minded, funny about money instead of awkward about it.

**One sentence anchor:** if Zerodha and a 19th-century Marwadi trading ledger had a baby app, raised by someone who posts finance memes.

---

## Color Palette

Named, specific, and pulled from real material sources rather than a generic "warm earthy" default:

| Name | Hex | Where it's from | Usage |
|---|---|---|---|
| **Bikaneri Red** | `#A6232E` | Traditional ledger binding cloth, wedding card red | Primary brand color, CTAs, alerts |
| **Haldi Gold** | `#D4A017` | Turmeric, gota-patti embroidery, coin motifs | Accent, highlights, "profit" states |
| **Jharokha Cream** | `#F3EAD8` | Sandstone haveli walls | Background, base surface |
| **Indigo Bandhani** | `#2B3A6B` | Leheriya/bandhani dye, traditional turban indigo | Secondary accent, links, "you're owed" states |
| **Soot Black** | `#1E1A17` | Ink used in actual bahi-khata ledgers | Primary text, line work |
| **Mehendi Green** | `#5C7A4A` | Henna, used sparingly | "Paid back" / settled status only — keep rare so it stays meaningful |

Avoid: neon gradients, generic purple-to-pink fintech gradients, and anything that reads as "Diwali sale banner." The palette should feel like it could be screen-printed on fabric, not rendered exclusively in Figma.

---

## Typography

- **Display / headers**: A bold slab-serif or high-contrast serif with some weight and presence — something that nods to hand-painted shop signage lettering (the kind you see on Rajasthan market storefronts) without literally using a "Devanagari-style" Latin font, which tends to look like a costume font. Think along the lines of **Fraunces** or **Tiempos Headline**, set heavy.
- **Body / UI text**: A clean grotesque sans for actual usability — **Inter** or **General Sans**. Gen Z dashboards live or die on legibility; the cultural flavor should live in the display type and color, not fight the body text.
- **Numerals / amounts**: A monospace face (**JetBrains Mono** or **Space Mono**) for all money figures — ledgers are about alignment and precision, and mono numerals read as "this is a real number," not a marketing number.

---

## Iconography Style

- Line-weight icons inspired by **jharokha lattice cutwork** — geometric, symmetrical, built from repeating angular motifs rather than rounded "friendly fintech" blobs.
- A recurring **seal-stamp motif** (like a wax seal or ledger stamp) used for "confirmed," "settled," or "verified" states — this becomes a signature, stamped in Haldi Gold when an entry is marked paid.
- Category icons drawn as small **woodblock-print style glyphs** (the kind used in traditional textile printing) rather than generic emoji or flat icons — a coin stack, a turban, a camel, a till — kept minimal and single-color so they don't tip into caricature.
- Avoid: literal camel/desert/turban clip-art unless it's this specific woodblock-print treatment. The line between "cultural specificity" and "tourist stereotype" is execution — keep every icon geometric and intentional, never cartoonish.

---

## Key Feature Concepts

| Feature | What it does | Why the name works |
|---|---|---|
| **Khaata** | The main ledger/dashboard home — your full running balance | Literally "ledger" in Marwadi/Hindi business usage — direct, not cutesy |
| **Hisaab-Kitab** | Monthly summary view, total spend by category | The actual phrase for "accounts/reckoning" — instantly legible to the target audience |
| **Udhaari Tracker** | The lending/borrowing tracker (detailed below) | "Udhaar" = credit/lent money, used constantly in everyday Marwadi business speech |
| **Munafa Meter** | A savings-rate or "money saved this month" indicator | "Munafa" = profit — frames saving as profit, which fits the business-minded culture |
| **Pedhi Points** | A streak/gamification mechanic for consistent logging | "Pedhi" = a family trading firm/shop — frames consistency as "running your own firm" |
| **Bahi Stamp** | The seal-stamp confirmation animation when an entry is marked settled | Ties back to the iconography signature element |

---

## Udhaari Tracker — Lending Model (kitna kisko diya)

This is the one functional piece worth speccing properly, since "who owes me what" is a real, common Marwadi business-family habit (formalized lending within trusted circles), not just a UI gimmick.

**Per-entry data model:**

| Field | Type | Notes |
|---|---|---|
| Name | Text | Who you lent to / borrowed from |
| Direction | Toggle: "Diya" (lent) / "Liya" (borrowed) | Plain-language toggle instead of a generic +/- |
| Amount | Currency | |
| Date given | Date | |
| Expected return date | Date (optional) | |
| Status | Pending / Partially settled / Fully settled (Bahi Stamp on completion) | |
| Comments | Free-text, multiple entries per record | See below |
| Running balance per person | Auto-calculated | If the same person appears in multiple entries, show their net balance, not just per-transaction |

**Comments behavior**: each lending entry should support a running thread of dated notes, like a real ledger margin note: "Reminded him 12 June," "Said he'll pay after Diwali bonus," "Partial ₹2,000 received 3 July." This is the detail that actually matches how this tracking happens informally already (a notebook with margin notes), so it shouldn't be a single edit field, it should be an append-only comment log per entry, timestamped automatically.

**View**: a "Kiska Kitna" (who owes how much) summary screen — one row per person, net amount, color-coded (Indigo for "they owe you," Bikaneri Red for "you owe them"), tapping a row expands into the full entry + comment history.

---

## Interactive Element Ideas

- **Stamp-to-settle**: marking an Udhaari entry as fully paid triggers a satisfying wax-seal stamp animation in Haldi Gold — a small moment of delight tied directly to the cultural motif, not a generic confetti burst.
- **Swipe-to-remind**: swiping a pending Udhaari entry surfaces a quick "bhej reminder" action — pre-drafted, lightly cheeky nudge text the person can send via WhatsApp.
- **Hisaab streak**: a Pedhi Points counter that ticks up for consecutive days of logging, displayed as small stacked coin icons rather than a generic flame/streak icon.
- **Voice-note style quick add**: a floating action button styled like a ledger pen-nib, for fast expense entry — tapping it opens a single-line input that parses "500 chai stall" the way a shopkeeper would jot it down.

---

## Example UI Snippet — Home Screen Description

```
┌─────────────────────────────────────┐
│  KHAATA                    🪔 [Profile]│
│  Hisaab-Kitab · June 2026             │
│                                       │
│  Munafa Meter                        │
│  ₹ 12,400 saved this month  ▲ 18%    │
│                                       │
│  ── Kiska Kitna ──                   │
│  Rohit Bhai        +₹5,000  (owes)   │
│  Priya              −₹2,000  (you owe)│
│  [+ Naya Udhaar]                     │
│                                       │
│  ── Recent Hisaab ──                 │
│  Chai stall          ₹40    Food     │
│  Auto                ₹120   Transport│
│                                       │
│         [ ✒️ Quick Add ]              │
└─────────────────────────────────────┘
```

Every label here is doing double duty: it's culturally specific *and* immediately understandable without a glossary, which is the actual design test for whether this lands as authentic rather than performative.

---

## What to deliberately avoid

- No camel silhouettes, no turban clip-art, no "desert sunset" gradients — these are the tourist-brochure defaults and they'll read as costume rather than culture.
- No forced Hinglish in every single label — sprinkle it where it's natural (Khaata, Udhaar, Hisaab) and let plain English carry the rest (Settings, Notifications, Export).
- Don't make the lending tracker feel like a debt-collection app — the tone should stay warm and familial ("Rohit Bhai owes ₹5,000"), not punitive (no red alert sirens, no "OVERDUE" stamps).
