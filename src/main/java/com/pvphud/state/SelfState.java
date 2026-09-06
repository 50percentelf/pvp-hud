package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Self-effects displayed in the right panel: veng, freeze, TB, poisons. */
public class SelfState
{
	/** Vengeance has been cast and is ready to trigger on next incoming hit. */
	@Getter @Setter
	private boolean vengActive;

	/** Ticks remaining on the local player's freeze (0 = not frozen). */
	@Getter @Setter
	private int freezeTicksRemaining;

	/** Ticks remaining on the local player's Tele Block (0 = not TB'd). */
	@Getter @Setter
	private int teleBlockTicksRemaining;

	@Getter @Setter
	private boolean poisoned;

	@Getter @Setter
	private boolean venomed;

	public void reset()
	{
		vengActive = false;
		freezeTicksRemaining = 0;
		teleBlockTicksRemaining = 0;
		poisoned = false;
		venomed = false;
	}
}
