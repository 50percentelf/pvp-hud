package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Miscellaneous self-effects: spec energy, weapon speed, and Lightbearer. */
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

	public void reset()
	{
		specEnergy = 0;
		specRegenTicksRemaining = 0;
		lightbearer = false;
		weaponSpeedTicks = 0;
	}
}
