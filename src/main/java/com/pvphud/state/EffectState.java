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

	// ── Divine potion remaining ticks (varbit counts down each game tick) ──────
	@Getter @Setter private int divineSupercombatTicks;
	@Getter @Setter private int divineRangingTicks;
	@Getter @Setter private int divineMagicTicks;
	@Getter @Setter private int divineBastionTicks;
	@Getter @Setter private int divineBattlemageTicks;
	@Getter @Setter private int menaphiteRemedyTicks;

	public void reset()
	{
		specEnergy = 0;
		specRegenTicksRemaining = 0;
		lightbearer = false;
		weaponSpeedTicks = 0;
		divineSupercombatTicks = 0;
		divineRangingTicks = 0;
		divineMagicTicks = 0;
		divineBastionTicks = 0;
		divineBattlemageTicks = 0;
		menaphiteRemedyTicks = 0;
	}
}
