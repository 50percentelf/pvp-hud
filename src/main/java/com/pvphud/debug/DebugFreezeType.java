package com.pvphud.debug;

import net.runelite.api.SpriteID;

/** Freeze type options for debug state injection. */
public enum DebugFreezeType
{
	NONE("None",           0,                        0),
	ICE_RUSH("Ice Rush",   SpriteID.SPELL_ICE_RUSH,  8),
	ICE_BURST("Ice Burst", SpriteID.SPELL_ICE_BURST, 16),
	ICE_BLITZ("Ice Blitz", SpriteID.SPELL_ICE_BLITZ, 24),
	ICE_BARRAGE("Ice Barrage", SpriteID.SPELL_ICE_BARRAGE, 32),
	BIND("Bind",           SpriteID.SPELL_ICE_RUSH,  8),
	SNARE("Snare",         SpriteID.SPELL_ICE_BURST, 16),
	ENTANGLE("Entangle",   SpriteID.SPELL_ICE_BLITZ, 24);

	private final String display;
	public  final int    spriteId;
	public  final int    defaultTicks;

	DebugFreezeType(String display, int spriteId, int defaultTicks)
	{
		this.display      = display;
		this.spriteId     = spriteId;
		this.defaultTicks = defaultTicks;
	}

	@Override
	public String toString()
	{
		return display;
	}
}
