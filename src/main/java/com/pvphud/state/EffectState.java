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

	// ── Antifire protection — phase-based duration tracking ──────────────────
	// Two independent families; phase intervals differ between them.
	//
	// Normal family (Antifire + Extended Antifire): Varbits.ANTIFIRE, 30 ticks/phase
	//   Fresh Antifire:          varbit 20  → 600 ticks
	//   Fresh Extended Antifire: varbit 40  → 1200 ticks
	//
	// Super family (Super Antifire + Extended Super Antifire): Varbits.SUPER_ANTIFIRE, 20 ticks/phase
	//   Fresh Super Antifire:          varbit 15 → 300 ticks
	//   Fresh Extended Super Antifire: varbit 30 → 600 ticks
	//
	// nextAntifireTick / nextSuperAntifireTick:
	//   The game tick at which the next phase boundary occurs.
	//   -1 = effect not active (or phase not yet observed).
	//   Set to (currentTick + phaseInterval) when the effect starts or when the
	//   plugin joins mid-effect (conservative: assumes full interval remaining).
	//   Only updated when the phase boundary has passed (nextTick - currentTick <= 0).
	//
	// Remaining ticks formula (mirrors RuneLite TimersAndBuffsPlugin):
	//   nextXxxTick - currentTick + (varbitValue - 1) * phaseInterval
	private int nextAntifireTick        = -1;
	private int antifireVarbitValue;
	private int nextSuperAntifireTick   = -1;
	private int superAntifireVarbitValue;

	public int  getNextAntifireTick()           { return nextAntifireTick; }
	public void setNextAntifireTick(int t)       { nextAntifireTick = t; }
	public int  getAntifireVarbitValue()         { return antifireVarbitValue; }
	public void setAntifireVarbitValue(int v)    { antifireVarbitValue = v; }
	public int  getNextSuperAntifireTick()       { return nextSuperAntifireTick; }
	public void setNextSuperAntifireTick(int t)  { nextSuperAntifireTick = t; }
	public int  getSuperAntifireVarbitValue()    { return superAntifireVarbitValue; }
	public void setSuperAntifireVarbitValue(int v) { superAntifireVarbitValue = v; }

	/** Remaining normal-antifire ticks (0 if not active). */
	public int getAntifireTicks(int currentTick)
	{
		if (nextAntifireTick < 0 || antifireVarbitValue <= 0) return 0;
		return Math.max(0, nextAntifireTick - currentTick + (antifireVarbitValue - 1) * 30);
	}

	/** Remaining super-antifire ticks (0 if not active). */
	public int getSuperAntifireTicks(int currentTick)
	{
		if (nextSuperAntifireTick < 0 || superAntifireVarbitValue <= 0) return 0;
		return Math.max(0, nextSuperAntifireTick - currentTick + (superAntifireVarbitValue - 1) * 20);
	}

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
		nextAntifireTick         = -1;
		antifireVarbitValue      = 0;
		nextSuperAntifireTick    = -1;
		superAntifireVarbitValue = 0;
	}
}
