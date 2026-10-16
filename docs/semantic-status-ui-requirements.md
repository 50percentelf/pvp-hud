# PvP HUD Semantic Status UI Requirements

## Problem

PvP HUD now understands enough statuses that a flat list of independent timers will not scale. Adding every tracked effect as another row/icon will cause visual overload and clipping, especially in Inventory Hug and constrained layouts.

The engine should remain exhaustive. The UI should be selective and contextual.

## Core principle

Track everything. Render a semantic projection of the current combat state.

Do not equate one tracked value with one visual row.

## Density policies

Design three display policies:

- MINIMAL: immediate control/danger only
- TACTICAL: contextually important PvP state; intended default
- FULL: every currently active state/cooldown the plugin understands

Exact names are open to change.

## Logical effect components

Related lifecycle states should share one logical visual slot/component.

Examples:

- Freeze -> refreeze immunity -> gone
- Tele Block -> TB immunity -> gone
- Ward active -> Ward cooldown -> gone
- Mark active -> Mark cooldown -> gone

Periodic effects should support parent + secondary event in one component:

- Prayer Regen total duration + next prayer proc
- Corruption total state + next drain
- Burn accumulated damage + next hit
- Menaphite total duration + next restore

## Prioritization

Effects need explicit priority/visibility metadata.

Suggested conceptual priorities:

- CRITICAL: freeze/bind/stun, TB, prayer lock, Sacrifice, corruption
- TACTICAL: burn, poison/venom, SOTD, Ward, Entice
- SUSTAIN: Prayer Regen, Menaphite, Divines, Stamina, antifires
- READINESS: Surge, Heart, shield/spell cooldowns

Critical information must not silently disappear because lower-priority effects consumed available space.

## Resource attachment to evaluate

Explore attaching statuses to the resource they primarily affect:

HP:
- Burn
- Ancient godsword Sacrifice
- poison/venom where appropriate

Prayer:
- Corruption
- Dragon scimitar prayer lock
- Prayer Regeneration

Movement/Run:
- Freeze/bind
- refreeze immunity
- run lock
- stun/control

General tray:
- SOTD
- Ward
- Mark
- Entice
- Surge
- Heart
- Divines

This is a direction to evaluate, not a forced layout.

## All current layouts must remain supported

- CHAT_LOCKED
- HORIZONTAL_FLOAT
- VERTICAL_FLOAT
- INVENTORY_HUG

The semantic model should be independent of layout. Each layout decides how the same semantic components are projected into its available geometry.

## Inventory Hug

Inventory Hug should specifically be evaluated as two semantic rails rather than a generic timer list:

- left rail: danger/control/resource-adjacent state
- top rail: buffs/readiness/sustain

This is provisional and should be validated against geometry.

## Overflow policy

There must be an explicit overflow policy, not accidental clipping.

At minimum:

1. critical states guaranteed visibility
2. lower priority elements can collapse/aggregate/hide based on density policy
3. hidden elements should be inspectable in FULL mode
4. the render manifest should record why an element was hidden or collapsed

## Rendering abstraction

Move away from ActiveEffectView being only a single icon/label/timer row if necessary.

The semantic UI model should support:

- stable semantic effect ID
- category/group
- priority
- primary value
- optional secondary value/event
- phase/lifecycle state
- preferred resource attachment
- density visibility
- compact/full presentation
- icon with text fallback
- hidden/collapsed reason

Do not force these exact field names if a cleaner model fits the current codebase.

## QA coupling

This semantic model must be designed together with the offline visual QA harness so that every rendered semantic element exposes deterministic bounds and metadata for machine-readable audit.

The offline harness should be able to prove:

- which semantic effects were rendered
- where they were rendered
- which were hidden/collapsed
- why they were hidden/collapsed
- that critical items remained visible

## Design-only first

Before implementation, inspect the current ActiveEffectView, EffectGroup, PvpHudOverlay renderers, LayoutProfile, DebugStateBuilder, and existing geometry tests.

Return a proposed migration plan that minimizes rewrite risk.
