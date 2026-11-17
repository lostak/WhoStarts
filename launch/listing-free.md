# Store listing: WhoStarts (free)

Package: `com.lostak.whostarts`
Play Console path: **Grow users > Store presence > Main store listing**

Each box below is ready to copy and paste. Character counts are checked against Play's limits.

## App name (30 max)

```
WhoStarts: Pool Game Tracker
```

## Short description (80 max)

```
Track pool wins, call solids or stripes, and flip a coin to settle who breaks.
```

## Full description (4,000 max)

```
Who won last game? Who's on solids? Who breaks? WhoStarts settles it, right at the table.

WhoStarts is a pool scoreboard built for the bar, the basement and league night. Set up your regular matchups once, then record every game with a single tap. No sign-up, no internet, no fuss.

TRACK EVERY RIVALRY
• One-on-one, doubles, or up to 8 teams in a single matchup
• Name your teams ("The Sharks") or just use player names
• Pick a color for each team and it follows them everywhere
• Tap the winner, confirm, and enjoy the celebration
• Player names auto-suggest from your past matchups

SETTLE THE BREAK
• Head-to-head? Flip the 3D coin to decide who breaks
• Three or more teams? Spin the wheel
• Tiebreakers never count as a game, so your record stays clean

KNOW WHO'S REALLY ON TOP
• Win counts and win percentage for every team
• Momentum chart that shows who's on a heater
• Recent form at a glance
• Full timestamped history of every game

SOLIDS OR STRIPES
• Tap the chip on each matchup to mark who's on solids and who's on stripes, so nobody "forgets" mid-rack

PRIVATE BY DESIGN
• Everything stays on your phone
• No account, no tracking, works with no signal in the back of the bar

Rack 'em up.
```

## Graphics

| Asset | File | Play spec |
|---|---|---|
| App icon | `launch/graphics/icon-512.png` | 512 x 512 PNG |
| Feature graphic | `launch/graphics/feature-graphic-free.png` | 1024 x 500 PNG |
| Phone screenshots | `launch/graphics/phone-framed/1..4` | 1080 x 1920, upload in order 1 to 4 |
| 7-inch tablet screenshots | `store/screenshots/tablet-7in/1..4` | 1206 x 2144 |
| 10-inch tablet screenshots | `store/screenshots/tablet-10in/1..4` | 2640 x 1485 |

Phone screenshot captions (baked into the framed images):

1. Matchups list: "Every rivalry, one tap away"
2. History dialog: "See who's really on top"
3. Coin flip: "Settle the break with a coin flip"
4. Wheel spin: "A whole crew? Spin the wheel"

Unframed phone shots are in `store/screenshots/phone/` if you'd rather use those. To re-render the graphics after a UI change, run `python3 tools/make_store_graphics.py`.

## Store settings

Play Console path: **Grow users > Store presence > Store settings**

- **App or game:** App
- **Category:** Sports
- **Tags:** pick up to 5 from Play's list. Best fits to look for: sports scoreboard or score keeping, billiards or pool, sports tracking, tournaments.
- **Email:** the address you want shown publicly on the listing (required)
- **Website:** `https://lostak.github.io/WhoStarts/` (optional, works once GitHub Pages is on)
- **Phone:** leave blank
