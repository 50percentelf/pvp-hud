package com.pvphud.debug;

/** Poison/immunity state options for debug state injection. */
public enum DebugPoisonType
{
	NONE("None"),
	POISONED("Poisoned"),
	VENOMED("Venomed"),
	ANTI_POISON("Anti-Poison"),
	ANTI_VENOM("Anti-Venom");

	private final String display;

	DebugPoisonType(String display)
	{
		this.display = display;
	}

	@Override
	public String toString()
	{
		return display;
	}
}
