package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Miscellaneous self-effects: spec energy, weapon speed, Lightbearer, and timed potion effects. */
public class EffectState
{
	/** Special attack energy 0–100. */
	@Getter @Setter private int specEnergy;

	/** Ticks until the next special attack energy tick (+10%). */
	@Getter @Setter private int specRegenTicksRemaining;

	/** Whether the player has Lightbearer equipped (halves spec regen time). */
	@Getter @Setter private boolean lightbearer;

	/** Current weapon's attack speed in ticks (0 = unknown). */
	@Getter @Setter private int weaponSpeedTicks;

	/** Wall-clock timestamp of the last spec regen tick (+10%); -1 = not yet observed. */
	@Getter @Setter private long lastSpecRegenMs = -1;

	/** Stamina potion effect ticks remaining (self-tracked; server flag is binary). */
	@Getter @Setter private int staminaEffectTicks;

	// ── Divine potion remaining ticks (varbit counts down each game tick) ──────
	@Getter @Setter private int divineSupercombatTicks;
	@Getter @Setter private int divineRangingTicks;
	@Getter @Setter private int divineMagicTicks;
	@Getter @Setter private int divineBastionTicks;
	@Getter @Setter private int divineBattlemageTicks;

	// ── Menaphite Remedy — periodic effect ───────────────────────────────────
	// totalTicksRemaining is updated from the server varbit each tick.
	// nextProcTicks will be wired in Task 5.
	@Getter private final PeriodicEffect menaphite = new PeriodicEffect();

	public void reset()
	{
		specEnergy              = 0;
		specRegenTicksRemaining = 0;
		lastSpecRegenMs         = -1;
		lightbearer             = false;
		weaponSpeedTicks        = 0;
		staminaEffectTicks      = 0;
		divineSupercombatTicks  = 0;
		divineRangingTicks      = 0;
		divineMagicTicks        = 0;
		divineBastionTicks      = 0;
		divineBattlemageTicks   = 0;
		menaphite.reset();
	}
}
