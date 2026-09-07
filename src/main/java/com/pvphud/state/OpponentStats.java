package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Hiscores-derived base stats for the current opponent (populated asynchronously). */
public class OpponentStats
{
	/** Base stat levels; -1 = not yet loaded. */
	@Getter @Setter private int attack   = -1;
	@Getter @Setter private int strength = -1;
	@Getter @Setter private int defence  = -1;
	@Getter @Setter private int ranged   = -1;
	@Getter @Setter private int magic    = -1;

	public boolean isKnown()
	{
		return attack >= 0;
	}

	public void reset()
	{
		attack = strength = defence = ranged = magic = -1;
	}
}
