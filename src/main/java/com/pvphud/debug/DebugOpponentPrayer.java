package com.pvphud.debug;

import net.runelite.api.HeadIcon;

/** Opponent overhead prayer options for debug state injection. */
public enum DebugOpponentPrayer
{
	NONE("None"),
	PROTECT_MELEE("Protect Melee"),
	PROTECT_RANGED("Protect Ranged"),
	PROTECT_MAGIC("Protect Magic"),
	SMITE("Smite");

	private final String display;

	DebugOpponentPrayer(String display)
	{
		this.display = display;
	}

	public HeadIcon toHeadIcon()
	{
		switch (this)
		{
			case PROTECT_MELEE:  return HeadIcon.MELEE;
			case PROTECT_RANGED: return HeadIcon.RANGED;
			case PROTECT_MAGIC:  return HeadIcon.MAGIC;
			case SMITE:          return HeadIcon.SMITE;
			default:             return null;
		}
	}

	@Override
	public String toString()
	{
		return display;
	}
}
