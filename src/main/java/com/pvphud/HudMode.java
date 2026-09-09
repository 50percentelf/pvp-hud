package com.pvphud;

public enum HudMode
{
	/** HUD is visible while in Wilderness/PvP worlds. */
	PVP_AREA
	{
		@Override public String toString() { return "PvP Areas"; }
	},
	/** HUD activates automatically during active PvP combat. */
	AUTO
	{
		@Override public String toString() { return "Active PvP Fight"; }
	},
	/** HUD enabled; toggle via hotkey or config. */
	MANUAL
	{
		@Override public String toString() { return "Always / Manual"; }
	}
}
