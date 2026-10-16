# PvP HUD Offline UI QA Requirements

## Goal

Remove the product owner as a routine UI-debugging bottleneck. The normal development loop should not require launching RuneLite merely to inspect layout, spacing, clipping, status density, or visual regressions.

RuneLite should be required only for final in-game behavioral QA and any mechanic that fundamentally depends on live client behavior.

## Required outcome

Build toward an offline HUD QA system that can:

1. Render every supported PvP HUD layout without launching RuneLite.
2. Render deterministic synthetic PvP state scenarios.
3. Produce PNG snapshots for human/agent inspection.
4. Assert layout geometry automatically.
5. Compare current snapshots against approved baselines.
6. Surface visual/layout regressions before in-game testing.
7. Let ChatGPT and Claude review artifacts and iterate without requiring the product owner to boot RuneLite.

## Layout matrix

Every harness must cover all current HudLayout values:

- CHAT_LOCKED
- HORIZONTAL_FLOAT
- VERTICAL_FLOAT
- INVENTORY_HUG

The harness should also account for gameframe/config dimensions where they materially change layout behavior.

## Density policies to design

The status UI should move toward a semantic projection of tracked combat state rather than a flat timer list.

Design these density modes:

- MINIMAL — control and immediate danger only
- TACTICAL — contextually important PvP state; intended default
- FULL — every active effect/cooldown that the engine understands

These names are provisional; Claude may propose better names but must preserve the three behavioral tiers.

## Semantic effect-slot direction

A single logical effect should not create multiple unrelated HUD rows when its states are naturally connected.

Examples:

Freeze lifecycle:

FREEZE 12t -> REFREEZE IMM 5t -> gone

Tele Block lifecycle:

TB 2:31 -> TB IMM -> gone

Ward lifecycle:

WARD 31t -> WARD CD 19t -> gone

Prayer Regeneration:

one logical visual component containing both total duration and next proc, e.g.

PRAYER REGEN
7:32
+1 in 8t

Corruption:

CORRUPTION
0:18
-3 in 5t

Burn:

BURN 20
NEXT 3

The exact rendering is open for design, but the data model should support parent + secondary/next-event state instead of forcing each value into a separate ActiveEffectView row.

## Semantic resource attachment to explore

Evaluate whether effects should visually attach to the resource they affect:

HP-related:
- Burn
- Ancient godsword Sacrifice
- Poison/Venom where appropriate

Prayer-related:
- Corruption
- Dragon scimitar prayer lock
- Prayer Regeneration

Movement-related:
- Freeze / bind
- Refreeze immunity
- run lock
- stun/control where appropriate

General status/readiness tray:
- SOTD
- Ward
- Mark
- Entice
- Surge
- Heart cooldowns
- Divines
- other buffs/readiness

This is a design direction to evaluate, not a requirement to implement literally.

## Offline rendering harness requirements

The harness should render the real production HUD renderer or shared rendering primitives, not a second mock UI that can drift away from production.

Preferred architecture:

- deterministic PvpHudState / DebugStateBuilder fixtures
- deterministic config fixture
- fixed canvas/gameframe geometry
- BufferedImage + Graphics2D render target
- production rendering path invoked in headless mode where practical
- PNG output under a predictable build directory

Possible output structure:

build/hud-snapshots/
  chat_locked/
    baseline.png
    combat-critical.png
    max-status.png
  horizontal_float/
  vertical_float/
  inventory_hug/

Do not hardcode this structure if a better Gradle/JUnit-compatible convention exists.

## Scenario matrix

At minimum define deterministic scenarios for:

1. EMPTY / baseline
2. NORMAL_PVP — representative common fight state
3. CRITICAL_CONTROL — freeze, TB, prayer lock, corruption etc.
4. SUSTAIN_HEAVY — divines, stamina, antifire, prayer regen, Menaphite, SOTD, Ward, Mark
5. CYCLE_HEAVY — parent effects plus all next-proc/next-hit states
6. MAX_STATUS / WORST_CASE — roughly 20+ simultaneous statuses/effects/readiness states
7. LOW_HP_PRESSURE — HP danger plus harmful statuses
8. PRAYER_PRESSURE — corruption/prayer lock/prayer regen interactions
9. MOVEMENT_PRESSURE — freeze/refreeze/control stack
10. LONG_LABEL_STRESS — longest reasonable labels and timer widths
11. ICON_MISSING_FALLBACK — verify text fallback when sprite/item images are unavailable
12. EDGE_GEOMETRY — smallest supported bounds / constrained Inventory Hug dimensions

Claude should inspect the current DebugStateBuilder and existing debug overlay/tests and reuse them rather than duplicating scenario infrastructure.

## Automated geometry assertions

For every rendered view where applicable, tests should detect:

- content outside HUD bounds
- content overlap/collision
- negative dimensions
- clipped critical state without explicit overflow policy
- text crossing configured column/rail boundaries
- action strip collision
- HP/Prayer/Run/Spec bar collision
- opponent/self panel collision
- inventory rail overflow
- group separator overflow
- effect tile overlap
- hidden elements still consuming layout space
- duplicate placement of the same semantic status

Critical effects should never silently disappear because lower-priority effects consumed available space.

## Snapshot / visual regression strategy

Design a practical snapshot-regression strategy for Java/Gradle.

Requirements:

- deterministic rendering as far as possible
- approved reference PNGs or another deterministic representation
- current render output
- diff output highlighting changed pixels or changed regions
- configurable tolerance for antialiasing/font differences if necessary
- failure should identify layout + scenario

Do not blindly assert exact pixels if Java2D/font platform differences make that brittle. If exact PNG comparison is unsuitable, propose a layered approach such as:

1. geometry assertions as mandatory
2. structural render metadata as mandatory
3. perceptual/pixel snapshot comparison with controlled tolerance

## Structural render manifest

Strongly consider generating a machine-readable manifest alongside each screenshot, e.g. JSON containing:

- canvas size
- layout
- scenario
- density policy
- rendered semantic elements
- element bounds
- priority
- group/category
- parent/secondary relationships
- clipped/hidden reason

Example concept:

{
  "layout": "INVENTORY_HUG",
  "scenario": "MAX_STATUS",
  "elements": [
    {
      "id": "FREEZE",
      "bounds": [12, 44, 52, 18],
      "priority": "CRITICAL",
      "visible": true
    }
  ]
}

This manifest is important because ChatGPT/Claude can audit layout behavior programmatically without relying only on image interpretation.

## Artifact review workflow

The eventual bridge workflow should be:

1. ChatGPT gathers requirement from product owner.
2. ChatGPT creates/updates Claude issue.
3. Claude implements in isolated task branch.
4. Claude runs unit + HUD snapshot suite.
5. Claude produces snapshot artifacts + manifests + summary.
6. ChatGPT reviews code diff, geometry results, and visual artifacts.
7. If issues exist, ChatGPT sends corrections back to the same Claude task/session.
8. Product owner is only pulled in for subjective product decisions or final in-game QA.

## Design constraints

- Do not create a separate fake HUD implementation solely for screenshots.
- Prefer extraction of pure/shared rendering components if current PvpHudOverlay is too coupled to RuneLite Client services.
- Do not rewrite the entire plugin merely to enable screenshots.
- Preserve current layouts while designing the new semantic effect model.
- Keep Vengeance disabled/deferred.
- Offline QA should work in normal Gradle/JUnit/headless CI where feasible.
- The harness must be useful locally on Windows and in CI.

## Final design principle

The product owner should not be the screenshot generator or routine layout tester.

The system should be able to answer, automatically:

- What does every HUD layout look like right now?
- What happens with 20 simultaneous effects?
- Did this change create clipping or overlap?
- Which critical effects were hidden?
- Which views changed compared with the approved baseline?

RuneLite should be reserved for validating real game/client integration and final QA, not basic UI iteration.
