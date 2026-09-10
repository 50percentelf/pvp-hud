# PvP HUD

A configurable PvP combat HUD for RuneLite. Consolidates fight tracking, buff timers, resource bars, boost monitoring, and combat-protection indicators into a single configurable overlay.

---

## Layouts

| Layout | Description |
|---|---|
| **Chat Locked** | Fills the chatbox region and stays pinned there. Cannot be dragged. |
| **Horizontal Float** | Same proportions as Chat Locked but freely draggable via Alt+drag. |
| **Vertical Float** | Narrow stacked panel, freely draggable via Alt+drag. |
| **Inventory Hug** | Attaches a left rail and top rail flush against the inventory panel without overlapping it. |

Float layouts remember their last dragged position independently per layout and per gameframe (Fixed / Resizable Classic / Resizable Modern).

---

## Panels

### Opponent (left / top)

| Element | Notes |
|---|---|
| Name | Opponent's display name. |
| HP bar | Colour-coded green → yellow → red. Derived from health-bar ratio. |
| Estimated HP | Back-calculated from total damage dealt and current ratio; replaced by hiscores HP once loaded. |
| Overhead prayer | Large icon + label for Protect Melee/Ranged/Magic and Smite. |
| Hiscores stats | ATK / STR / DEF / RNG / MAG populated asynchronously from the hiscores API on combat start. |

### Fight (centre)

| Element | Notes |
|---|---|
| Hit log | Three most recent outgoing (green) and incoming (red) hits, newest at top. |
| Stack label | `[dmg]` brackets indicate multiple hitsplats on the same actor in the same tick (weapon + ring of recoil, etc.). |
| Prayer drain | `-Np` suffix on hits where Smite was active. |
| Session totals | `#D` / `#R` pinned to the bottom: total damage dealt and received. |
| Fight Over banner | Shown after the opponent's HP ratio reaches 0 or the session times out. |

### You (right / bottom)

| Bar | Config option |
|---|---|
| HP | Bars + numbers, bars only, numbers only, or hidden. Colour-coded. |
| Prayer | Same options. |
| Run Energy | Same options. |
| Spec | Same options. |

HP bar shakes for 600 ms after taking a hit (can be disabled).

### Buff strip

Displayed in the YOU panel. Three display styles: **Text**, **Vertical Bar** (icon + timer per row), **Icon Tray** (horizontal icon strip).

| Buff | Trigger |
|---|---|
| `ICE Xs` | Self-freeze timer. Starts only from the "You have been frozen!" game message. Repeated ice impacts while already frozen do not extend it. Clears immediately when movement is detected. Covers Rush (8t), Burst (16t), Blitz (24t), Barrage (32t), Bind (8t), Snare (16t), Entangle (24t). |
| `TB M:SS` | Tele Block countdown from the varbit. |
| `DSC / DRG / DMG / BAS / BTM / MEN` | Divine potion timers (can be hidden). |
| `STAM M:SS` | Stamina potion effect (self-tracked: 200 ticks / dose). |
| `VENOM / POISON` | Status indicator. |
| `ANTI-V / ANTI-P M:SS` | Anti-venom / anti-poison protection countdown. |
| `drain Xt` | Ticks to next expected stat drain, self-calibrated from observed drain intervals. |

### Action strip (bottom bar)

| Indicator | Notes |
|---|---|
| ATK dots | One dot per weapon speed tick; dots go dark as the delay expires. |
| `E` + 3 dots | Eat cooldown (3 ticks). |
| `P` + 3 dots | Potion cooldown (3 ticks). |
| `T1 / T2 M:SS` | Manual countdown timers, each bound to a configurable hotkey. |
| `PJ Xs` | PJ-safe timer: 20 ticks in Wilderness, 16 ticks on PvP worlds. Refreshed by every attack exchange including 0-damage splashes. |
| `IMM Xs` | Post-kill immunity timer (LMS only, 33 ticks). |
| `LOG` | Combat logout lock active (16 ticks after receiving any hit). |
| `LCK` | Under-attack lock active (20 ticks after receiving any hit — must fight back at current attacker). |
| `W#` | Current Wilderness level. |
| `MULTI` | Multi-combat zone indicator. |

### Boost row

ATK / STR / DEF / RNG / MAG skill icons with current boost level. Configurable as delta (`+16`) or boosted-over-base (`115/99`). Can be hidden.

---

## Configuration

| Setting | Default | Notes |
|---|---|---|
| HUD Mode | Manual | Manual (always show when enabled), PvP areas only (Wilderness + PvP worlds), or Auto (show only while a fight session is active). |
| HUD Layout | Chat Locked | See Layouts above. |
| Buff Display Style | Vertical Bar | Text, Vertical Bar, Icon Tray. |
| Background Opacity | 220 | 0–255. |
| HP Bar Shake | On | Shake on incoming hit. |
| Show Boost Row | On | ATK/STR/DEF/RNG/MAG row. |
| Show Boosts as Boosted/Base | Off | Displays `115/99` instead of `+16`. |
| Show Freeze Timer | On | ICE countdown in buff strip. |
| Show Divine Timers | On | DSC/DRG/DMG/BAS/BTM/MEN in buff strip. |
| HP / Prayer / Run / Spec Bar | Bars + Numbers | Per-bar: Bars + Numbers, Bars Only, Numbers Only, Hidden. |
| Toggle HUD Hotkey | Unset | Shows/hides the entire HUD. |
| Timer 1 / Timer 2 Hotkey | Unset | Start/stop manual countdown timers. |
| Timer 1 / Timer 2 Duration | 300 s | Configurable duration per timer. |

---

## Smite tracking

When the local player has Smite active, outgoing hits show `-Np` in the fight log where N = `floor(damage / 4)`. When the opponent has Smite, incoming hits show the same drain.

---

## Deferred / post-v0.1

- **Vengeance tracking** — `VENG RDY` self-indicator and `VENG!` opponent label. State infrastructure exists but event handlers are gated; feature is not displayed.
- **Special prayer-impact tracking** — Sara Strike, Clear Mind, and Sapphire bolt prayer-drain attribution. Enum values defined; no v0.1 code path sets them.
- LMS context detection for the IMM timer (currently always inactive).
- CHANCE! hit detection (requires opponent defence stats and combat formula).

---

## Experimental — Pending RuneLite Ruling

These features are **not implemented** and are **not enableable**. They appear in the plugin config panel as an informational heading only. Development will not begin until explicit RuneLite team guidance is received.

### Streamer Output — Pending RuneLite ruling

Renders the PvP HUD into a separate standalone window for OBS/window capture. Shares the exact same `PvpHudState` as the in-game overlay — no duplicate combat logic. The streamer window never queries `Client` directly; it renders immutable snapshots only. No HTTP server, WebSocket endpoint, or browser-source API in the initial design.

**Not implemented. Awaiting RuneLite review.**

### Opponent Attack Cycle — Pending RuneLite ruling

Live countdown to the opponent's next possible attack, derived from an observed combat animation. No attack-style prediction, no prayer recommendation, no target identification, no freeze tracking.

**Not implemented. Awaiting RuneLite review.**

---

## Known bugs

*No confirmed bugs at this time. Bug reports will be listed here and removed only after in-game confirmation of the fix.*
