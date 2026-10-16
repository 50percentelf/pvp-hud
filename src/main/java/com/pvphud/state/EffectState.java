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
	@Getter @Setter private int divineSuperAttackTicks;
	@Getter @Setter private int divineSuperStrengthTicks;
	@Getter @Setter private int divineSuperDefenceTicks;

	/** Imbued Heart recharge cooldown — varbit counts down from ~800 ticks (8 min). */
	@Getter @Setter private int imbuedHeartCooldownTicks;

	// ── Prayer Regeneration potion — periodic effect ──────────────────────────
	// totalTicksRemaining = Varbits.BUFF_PRAYER_REGENERATION × 12 (updated from varbit).
	// nextProcTicks       = 12 (reset each time the varbit decrements).
	@Getter private final PeriodicEffect prayerRegen = new PeriodicEffect();
	private int prevPrayerRegenVarbit = -1;

	/**
	 * Called from onVarbitChanged(BUFF_PRAYER_REGENERATION).
	 * Sets total remaining and resets next-proc on each varbit decrement.
	 */
	public void updatePrayerRegen(int varbitValue)
	{
		prayerRegen.setTotalTicksRemaining(varbitValue * 12);
		if (varbitValue == 0)
		{
			prayerRegen.reset();
		}
		else if (prevPrayerRegenVarbit > 0 && varbitValue < prevPrayerRegenVarbit)
		{
			prayerRegen.setNextProcTicks(12);
		}
		prevPrayerRegenVarbit = varbitValue;
	}

	// ── Staff of the Dead / Toxic SotD / Staff of Light / Staff of Balance ────
	// 100-tick (60-second) melee damage reduction buff applied by the weapon special.
	// Detected via GAMEMESSAGE; cleared on weapon unequip or expiry message.
	private int staffProtEndTick = -1;

	public int getStaffProtTicks(int currentTick)
	{
		return staffProtEndTick < 0 ? 0 : Math.max(0, staffProtEndTick - currentTick);
	}

	public void applyStaffProt(int currentTick) { staffProtEndTick = currentTick + 100; }
	public void clearStaffProt()                { staffProtEndTick = -1; }

	// ── Surge potion cooldown ─────────────────────────────────────────────────
	// VarbitID.SURGE_POTION_TIMER × 10 = ticks remaining on the cooldown.
	private int surgePotionEndTick = -1;

	public int getSurgePotionTicks(int currentTick)
	{
		return surgePotionEndTick < 0 ? 0 : Math.max(0, surgePotionEndTick - currentTick);
	}

	/** Called from onVarbitChanged(SURGE_POTION_TIMER). */
	public void updateSurgePotionFromVarbit(int varbitValue, int currentTick)
	{
		if (varbitValue <= 0)
			surgePotionEndTick = -1;
		else
			surgePotionEndTick = currentTick + varbitValue * 10;
	}

	// ── Burn damage counters ──────────────────────────────────────────────────
	// VarbitID.PLAYER_OWN_BURN_DAMAGE_TOTAL        = accumulated prayer drained so far.
	// VarbitID.PLAYER_OWN_BURN_DAMAGE_HITSPLAT_SIZE = next individual burn hit.
	@Getter @Setter private int burnDamageTotal;
	@Getter @Setter private int burnDamageNextHit;

	// ── Corruption ────────────────────────────────────────────────────────────
	// VarbitID.CORRUPTION_TARGET_ITERATIONS  = total number of prayer drains.
	// VarbitID.CORRUPTION_CURRENT_ITERATIONS = drains completed so far (counts up).
	// VarbitID.CORRUPTION_BASE_DAMAGE        = prayer drained per iteration.
	//
	// Interval detection: when CORRUPTION_CURRENT_ITERATIONS increments we record
	// the tick and compute interval from two successive increments.  Until two
	// increments are observed the interval is unknown (0).
	@Getter @Setter private int corruptionTargetIterations;
	@Getter @Setter private int corruptionCurrentIterations;
	@Getter @Setter private int corruptionBaseDamage;
	private int corruptionLastIterationTick = -1;
	private int corruptionDetectedInterval;
	private int corruptionNextDrainTick = -1;

	public boolean isCorruptionActive()
	{
		return corruptionTargetIterations > 0 && corruptionCurrentIterations < corruptionTargetIterations;
	}

	public int getCorruptionIterationsRemaining()
	{
		return Math.max(0, corruptionTargetIterations - corruptionCurrentIterations);
	}

	/**
	 * Remaining Corruption ticks, or 0 if the interval has not yet been observed.
	 * Formula: (nextDrainTick − currentTick) + (remainingIterations − 1) × interval
	 */
	public int getCorruptionTotalTicks(int currentTick)
	{
		if (corruptionDetectedInterval <= 0 || corruptionNextDrainTick < 0) return 0;
		int remaining = getCorruptionIterationsRemaining();
		if (remaining == 0) return 0;
		return Math.max(0, corruptionNextDrainTick - currentTick) + (remaining - 1) * corruptionDetectedInterval;
	}

	/** Ticks until the next Corruption prayer drain, or 0 if interval unknown. */
	public int getCorruptionNextDrainTicks(int currentTick)
	{
		return corruptionNextDrainTick < 0 ? 0 : Math.max(0, corruptionNextDrainTick - currentTick);
	}

	/**
	 * Called when CORRUPTION_CURRENT_ITERATIONS changes.
	 * Detects drain interval from two successive increments.
	 */
	public void onCorruptionIterationChanged(int currentTick)
	{
		if (corruptionLastIterationTick >= 0)
		{
			corruptionDetectedInterval = currentTick - corruptionLastIterationTick;
			corruptionNextDrainTick    = currentTick + corruptionDetectedInterval;
		}
		corruptionLastIterationTick = currentTick;
	}

	/** Clear all Corruption state (iterations consumed, expired, or plugin restart). */
	public void clearCorruption()
	{
		corruptionTargetIterations  = 0;
		corruptionCurrentIterations = 0;
		corruptionBaseDamage        = 0;
		corruptionLastIterationTick = -1;
		corruptionDetectedInterval  = 0;
		corruptionNextDrainTick     = -1;
	}

	// ── Ward of Arceuus ───────────────────────────────────────────────────────
	// Active buff: magicLevel ticks (detected via GAMEMESSAGE).
	// Cooldown:    Varbits.WARD_OF_ARCEUUS_COOLDOWN fires 1 when cooldown starts;
	//              fixed 50-tick (30-second) cooldown.
	private int wardOfArceuusEndTick  = -1;
	private int wardCooldownEndTick   = -1;

	public int getWardOfArceuusTicks(int currentTick)
	{
		return wardOfArceuusEndTick < 0 ? 0 : Math.max(0, wardOfArceuusEndTick - currentTick);
	}

	public void applyWardOfArceuus(int currentTick, int magicLevel)
	{
		wardOfArceuusEndTick = currentTick + magicLevel;
	}

	public void clearWardOfArceuus() { wardOfArceuusEndTick = -1; }

	public int getWardCooldownTicks(int currentTick)
	{
		return wardCooldownEndTick < 0 ? 0 : Math.max(0, wardCooldownEndTick - currentTick);
	}

	/** Fixed 50-tick (30-second) cooldown starts when the ward activates. */
	public void applyWardCooldown(int currentTick) { wardCooldownEndTick = currentTick + 50; }
	public void clearWardCooldown()                { wardCooldownEndTick = -1; }

	// ── Mark of Darkness ─────────────────────────────────────────────────────
	// Active buff: magicLevel × 3 ticks (detected via GAMEMESSAGE).
	// Cooldown:    VarbitID.BUFF_MARK_OF_DARKNESS_DISABLED fires 1 when active;
	//              fixed 10-tick cooldown.
	private int markOfDarknessEndTick = -1;
	private int markCooldownEndTick   = -1;

	public int getMarkOfDarknessTicks(int currentTick)
	{
		return markOfDarknessEndTick < 0 ? 0 : Math.max(0, markOfDarknessEndTick - currentTick);
	}

	public void applyMarkOfDarkness(int currentTick, int magicLevel)
	{
		markOfDarknessEndTick = currentTick + magicLevel * 3;
	}

	public void clearMarkOfDarkness() { markOfDarknessEndTick = -1; }

	public int getMarkCooldownTicks(int currentTick)
	{
		return markCooldownEndTick < 0 ? 0 : Math.max(0, markCooldownEndTick - currentTick);
	}

	/** Fixed 10-tick cooldown. */
	public void applyMarkCooldown(int currentTick) { markCooldownEndTick = currentTick + 10; }
	public void clearMarkCooldown()                { markCooldownEndTick = -1; }

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
		specEnergy               = 0;
		specRegenTicksRemaining  = 0;
		lastSpecRegenMs          = -1;
		lightbearer              = false;
		weaponSpeedTicks         = 0;
		staminaEffectTicks       = 0;
		divineSupercombatTicks   = 0;
		divineRangingTicks       = 0;
		divineMagicTicks         = 0;
		divineBastionTicks       = 0;
		divineBattlemageTicks    = 0;
		divineSuperAttackTicks   = 0;
		divineSuperStrengthTicks = 0;
		divineSuperDefenceTicks  = 0;
		imbuedHeartCooldownTicks = 0;
		prayerRegen.reset();
		prevPrayerRegenVarbit    = -1;
		staffProtEndTick         = -1;
		surgePotionEndTick       = -1;
		burnDamageTotal          = 0;
		burnDamageNextHit        = 0;
		clearCorruption();
		wardOfArceuusEndTick     = -1;
		wardCooldownEndTick      = -1;
		markOfDarknessEndTick    = -1;
		markCooldownEndTick      = -1;
		menaphite.reset();
		nextAntifireTick         = -1;
		antifireVarbitValue      = 0;
		nextSuperAntifireTick    = -1;
		superAntifireVarbitValue = 0;
	}
}
