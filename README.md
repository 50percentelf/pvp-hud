# PvP HUD

A configurable PvP combat HUD for RuneLite. Consolidates fight tracking, status effect timers, resource bars, and combat-protection indicators into a single configurable overlay.

---

## Layouts

| Layout | Description |
|---|---|
| **Horizontal Float** | Chatbox-sized panel. Defaults to **pinned to the chatbox** — it mirrors the chatbox position and size exactly. Disable **Pin to Chatbox** (under the Horizontal Float section) to detach the HUD and drag it anywhere via Alt+drag. |
| **Vertical Float** | Narrow stacked panel, freely draggable via Alt+drag. |
| **Inventory Hug** | Attaches a left rail and top rail flush against the inventory panel without overlapping it. Top rail shows the opponent panel. Left rail shows HP / Prayer / Run / Spec bars; if supported ammo is equipped, a dedicated ammo row appears below Spec showing the actual equipped arrow/bolt icon (18px) and current quantity. The action corner shows OUT / IN damage totals (dealt / received) below the attack clock. |

Float layouts remember their last dragged position independently per layout and per gameframe (Fixed / Resizable Classic / Resizable Modern).

---

## Panels

### Opponent (left / top)

| Element | Notes |
|---|---|
| Name | Opponent's display name. If the opponent is on an ironman hiscores endpoint, the matching badge is drawn next to the name. |
| HP bar | Colour-coded green → yellow → red. Derived from the opponent's health-bar ratio. |
| Estimated HP | Back-calculated from total damage dealt and current ratio while hiscores load; replaced by the hiscores HP once resolved. |
| Overhead prayer | Large icon + label for Protect Melee/Ranged/Magic and Smite. |
| Hiscores stats | ATK / STR / DEF / RNG / MAG populated asynchronously from the hiscores API on combat start (see Hiscores section below). |

### Fight (centre)

| Element | Notes |
|---|---|
| Hit log | Tick-paired rows newest first. Left column (OUT, cyan) = damage dealt by you; right column (IN, red) = damage received. **IN is not guaranteed to originate from the tracked opponent in multi-combat.** |
| Stack format | Single hit: `34`. Two same-tick hits: `20+8`. Three or more: `42 (3)`. |
| Smite drain | `-Np` suffix on outgoing hits where you had Smite active, computed as `floor(damage / 4)`. |
| Session totals | OUT (cyan) dealt total and IN (red) received total, pinned to the bottom. |
| Rows shown | Horizontal Float: 3 most recent ticks. Vertical Float: 2 most recent ticks. |
| Fight Over banner | Shown after opponent death or session timeout. |

### Fight / session lifecycle

A session begins when one of the following confirms combat with a tracked candidate:
- the local player uses the **Attack** menu option on a valid attackable player in a PvP zone (level-range respected in the Wilderness), **or**
- a confirmed outgoing hitsplat (`hitsplat.isMine()`) lands on the candidate, **or**
- an incoming hitsplat lands on the local player while the candidate is interacting with the local player.

Simply hovering or following a player does not populate the opponent panel. Once active, the session survives brief InteractingChanged nulls and short opponent despawn/reappear windows (10-tick grace). The session ends on opponent death, local death, ~30 s of no combat in either direction, the opponent remaining despawned past the grace window, logout / world hop, or plugin shutdown.

### You (right / bottom)

| Bar | Config option |
|---|---|
| HP | Bars + numbers, bars only, numbers only, or hidden. Colour-coded. |
| Prayer | Same options. |
| Run Energy | Same options. |
| Spec | Same options. |

**HP bar shake**: the HP bar and the self-splat glyph shake briefly only on **positive incoming damage** (`hitsplat.getAmount() > 0`). Zero-damage splashes do **not** trigger the shake. Can be disabled via the Appearance section.

### Status strip

Displayed in the YOU panel. Three display styles: **Text**, **Vertical Bar** (icon + timer per row), **Icon Tray** (horizontal icon strip). Entries are grouped by priority: Critical → Tactical → Sustain → Readiness.

| Status | Trigger |
|---|---|
| `ICE Xs` | Self-freeze timer. Ice spells (Rush/Burst/Blitz/Barrage) start from the "You have been frozen!" game message, using the same-tick impact graphic to select the duration; repeated ice hits while already frozen do not extend it. Bind/Snare/Entangle start directly from their local impact graphics. Clears immediately when movement is detected. Covers Rush (8t), Burst (16t), Blitz (24t), Barrage (32t), Bind (8t), Snare (16t), Entangle (24t). |
| `REFRZ Xt` | Post-thaw refreeze-immunity window (5 ticks). Only granted on natural freeze expiry — breaking the freeze early via movement does not grant it. |
| `TB M:SS` | Tele Block countdown from the varbit. |
| `TB-IMM` | Post-TB reapplication immunity window (raw varbit 1–100). During this window a fresh TB cannot be applied. |
| `PLOCK Xt` | Dragon scimitar Sever protection-prayer lock (8 ticks). Driven by the chat message "Your protection prayers have been disabled". |
| `CORRUPT` + `NXDRAIN` | Corruption total ticks remaining plus the next-drain countdown and per-drain damage. The next-drain countdown appears only once the drain interval has been observed from two successive iteration events. |
| `BURN X` + `BURN+N` | Accumulated burn damage (`PLAYER_OWN_BURN_DAMAGE_TOTAL`) and next burn hit size (`PLAYER_OWN_BURN_DAMAGE_HITSPLAT_SIZE`). |
| `VENOM` / `POISON` | Status indicator. Venom takes precedence over poison. |
| `SOTD Xs` | Staff of the Dead / Toxic SotD / Staff of Light / Staff of Balance melee-damage reduction buff — 100 ticks (60 s). Activated by the weapon special; cleared on weapon unequip or expiry message. |
| `WARD Xs` / `WARD-CD Xs` | Ward of Arceuus buff (magic level ticks) and 50-tick cooldown. |
| `MARK Xs` / `MARK-CD Xs` | Mark of Darkness buff (magic level × 3 ticks) and fixed 10-tick cooldown. |
| `ANTI-V` / `ANTI-P M:SS` | Anti-venom / anti-poison protection countdown. |
| `DSC / DRG / DMG / BAS / BTM / DSA / DSS / DSD` | Divine potion timers (can be hidden). DSC = Supercombat, DRG = Ranging, DMG = Magic, BAS = Bastion, BTM = Battlemage, DSA = Super Attack, DSS = Super Strength, DSD = Super Defence. |
| `MEN M:SS` + `NXT Xt` | Menaphite Remedy total time remaining and next-proc countdown (25-tick interval). The next-proc line appears once the first observed decrement synchronises the phase. |
| `PRYR M:SS` + `P-NXT Xt` | Prayer Regeneration potion total time remaining (varbit × 12 ticks) and next-proc countdown (12-tick interval). |
| `STAM M:SS` | Stamina potion effect (self-tracked: 200 ticks / dose). |
| `AF` / `SAF` | Antifire (normal: 30-tick phases, varbit × 30 ticks total) and Super Antifire (20-tick phases, varbit × 20 ticks total). |
| `SURGE Xs` | Surge potion cooldown (varbit × 10 ticks). |
| `IH-CD M:SS` | Imbued Heart recharge cooldown from the varbit (~800 ticks). |

### Action strip (bottom bar)

| Indicator | Notes |
|---|---|
| ATK dots | One dot per weapon-speed tick; dots go dark as the attack delay expires. The clock is only advanced when the local player plays a known PvP attack animation (see Attack timing below). |
| `E` + 3 dots | Eat cooldown (3 ticks). |
| `P` + 3 dots | Potion cooldown (3 ticks). |
| `T1 / T2 M:SS` | Manual countdown timers, each bound to a configurable hotkey. |
| `W#` | Current Wilderness level (requires Show Zone Info). |
| `MLT` | Multi-combat zone indicator (requires Show Zone Info). |
| `[ammo icon] qty` | Equipped ammo quantity (arrows/bolts from the ammo slot). Colour: white > 25, yellow ≤ 25, red ≤ 10. Conventional ammo only — thrown weapons and dart-stack weapons without an ammo slot are not tracked. In Inventory Hug this appears as a dedicated rail row (not in the action strip) — see layout table above. |

---

## Attack timing

The ATK clock is driven by a **conservative positive allowlist** of known RuneLite PvP attack animations (see `AttackAnimations`). Only when the local player's animation matches a known attack animation **and** they are interacting with another `Player` is a cooldown started. Unknown or unmapped weapon animations do **not** fabricate a cooldown — the ATK dots simply stay quiet. The cooldown duration is read from `ItemEquipmentStats.getAspeed()` of the equipped weapon.

---

## Hiscores

When a session begins the opponent name is submitted to RuneLite's shared `HiscoreManager`. The manager performs its own async fetches and the plugin polls the cache every three game ticks until either an endpoint returns a positive result or a 30-tick (~18 s) deadline elapses.

Because `lookupAsync` returns `null` both for "still loading" and "player not on this endpoint", there is no reliable negative signal. The plugin instead records the **best observed account type** across all endpoints (Normal → Ironman → Ultimate Ironman → Hardcore Ironman) and commits that verdict at the deadline. A positive Hardcore Ironman observation is committed immediately because nothing outranks it. The resolved account type drives the ironman badge drawn next to the opponent's name.

---

## Smite tracking

When the local player has Smite active, outgoing hits show `-Np` in the fight log where N = `floor(damage / 4)`.

---

## Configuration

| Setting | Default | Notes |
|---|---|---|
| HUD Enabled | On | Master enable/disable. HUD visible on fresh install. |
| HUD Mode | Manual | Manual (always show when enabled), PvP areas only (Wilderness + PvP worlds), or Auto (show only while a fight session is active). |
| HUD Layout | Horizontal Float | See Layouts above. |
| Pin to Chatbox (Horizontal Float) | On | When on, the Horizontal Float HUD is locked to the chatbox region and cannot be dragged. Disable to free-float. |
| Status Display Style | Vertical Bar | Text, Vertical Bar, Icon Tray. Set per layout. |
| Background Opacity | 220 | 0–255. Set per layout. |
| HP Bar Shake on Hit | On | Shake on positive incoming damage only; zero splats are ignored. |
| Show Freeze Timer | On | ICE / REFRZ countdown in the status strip. |
| Potion Effect Timers | On | DSC / DRG / DMG / BAS / BTM / DSA / DSS / DSD / MEN in the status strip. |
| Show Zone Info | Off | W# and MLT indicators in the action strip. |
| HP / Prayer / Run / Spec Bar | Bars + Numbers | Per-bar: Bars + Numbers, Bars Only, Numbers Only, Hidden. |
| Freeze Highlight Colour | None | Optional border around the freeze icon while frozen. |
| Teleblock Highlight Colour | None | Optional border around the Tele Block icon while TB'd. |
| HUD Background Colour | Dark grey | Base fill colour for all HUD panel backgrounds. Opacity is controlled per-layout. |
| Toggle HUD Hotkey | Unset | Shows/hides the entire HUD. |
| Timer 1 / Timer 2 Hotkey | Unset | Start/stop manual countdown timers. |
| Timer 1 / Timer 2 Duration | 300 s | Configurable duration per timer. |

---

## Limitations

- **Ancient sceptre freeze extension**: Ancient sceptre extends ice-spell freeze durations, but the plugin only observes the incoming freeze — it cannot reliably observe the attacker's weapon. Base ice-spell durations are used for incoming freezes regardless of what the opponent wields.
- **Smite drain in multi-combat**: `-Np` reflects what the local player would drain. In multi-combat, confirmation that the opponent's prayer actually fell by that amount is not available.
- **IN column in multi-combat**: incoming hits are recorded whenever they land on the local player. In multi-combat these may originate from parties other than the tracked opponent.
- **Burn/Corruption attribution**: shown as the varbit-reported total and next-hit size; not attributed to a specific weapon or caster.
