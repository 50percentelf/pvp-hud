package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Miscellaneous self-effects: spec energy, weapon speed, Lightbearer, and divine potion timers. */
public class EffectState
{
	/** Special attack energy 0–100. */
	@Getter @Setter
	private int specEnergy;

	/** Ticks until the next special attack energy tick (+10%). */
	@Getter @Setter
	private int specRegenTicksRemaining;

	/** Whether the player has Lightbearer equipped (halves spec regen time). */
	@Getter @Setter
	private boolean lightbearer;

	/** Current weapon's attack speed in ticks (0 = unknown). */
	@Getter @Setter
	private int weaponSpeedTicks;

	/** Wall-clock timestamp of the last spec regen tick (+10%); -1 = not yet observed. */
	@Getter @Setter
	private long lastSpecRegenMs = -1;

	/** Stamina potion effect ticks remaining (Varbits.STAMINA_EFFECT counts down each tick). */
	@Getter @Setter private int staminaEffectTicks;

	// ── Divine potion remaining ticks (varbit counts down each game tick) ──────
	@Getter @Setter private int divineSupercombatTicks;
	@Getter @Setter private int divineRangingTicks;
	@Getter @Setter private int divineMagicTicks;
	@Getter @Setter private int divineBastionTicks;
	@Getter @Setter private int divineBattlemageTicks;
	@Getter @Setter private int menaphiteRemedyTicks;

	// ── Anti-poison / anti-venom countdown (derived from POISON varp magnitude) ─
	/** Estimated ticks remaining on anti-poison protection. Self-decremented each tick, re-synced by varp. */
	@Getter @Setter private int antiPoisonTicks;
	/** Estimated ticks remaining on anti-venom protection. Self-decremented each tick, re-synced by varp. */
	@Getter @Setter private int antiVenomTicks;

	public void reset()
	{
		specEnergy = 0;
		specRegenTicksRemaining = 0;
		lastSpecRegenMs = -1;
		lightbearer = false;
		weaponSpeedTicks = 0;
		staminaEffectTicks = 0;
		divineSupercombatTicks = 0;
		divineRangingTicks = 0;
		divineMagicTicks = 0;
		divineBastionTicks = 0;
		divineBattlemageTicks = 0;
		menaphiteRemedyTicks = 0;
		antiPoisonTicks = 0;
		antiVenomTicks = 0;
	}
}
