package com.pvphud.debug;

/** Antifire protection type options for debug state injection. */
public enum DebugAntifireType
{
	NONE("None"),
	NORMAL("Antifire"),
	EXTENDED("Extended Antifire"),
	SUPER("Super Antifire"),
	SUPER_EXTENDED("Extended Super Antifire");

	private final String display;

	DebugAntifireType(String display)
	{
		this.display = display;
	}

	@Override
	public String toString()
	{
		return display;
	}
}
