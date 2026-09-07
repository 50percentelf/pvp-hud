package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.HeadIcon;

/** Runtime-observable state for the current PvP opponent (left panel). */
public class OpponentState
{
	/** Opponent's display name; null when no opponent is tracked. */
	@Getter @Setter
	private String name;

	/**
	 * Opponent's estimated current HP. -1 means unknown.
	 * Updated from HitsplatApplied events and the health-bar ratio.
	 */
	@Getter @Setter
	private int estimatedHp = -1;

	/**
	 * Opponent's max HP. -1 means unknown.
	 * Populated via health-bar back-calculation; never blocks combat initiation.
	 */
	@Getter @Setter
	private int maxHp = -1;

	/**
	 * True while the opponent has active Vengeance (observed via animation 4071).
	 * Cleared when any non-zero hitsplat lands on the opponent, consuming the veng.
	 * There is no countdown — Vengeance has no duration; it waits for the next
	 * qualifying hit to trigger.
	 */
	@Getter @Setter
	private boolean vengActive;

	/** Most recent outgoing hit dealt to the opponent (shown in corner). */
	@Getter @Setter
	private int lastOutgoingHit = -1;

	/** Damage computed from XP drop, shown as an in-flight indicator before HP bar updates. */
	@Getter private int  pendingHitDamage;
	@Getter private long pendingHitTimestampMs = -1;

	public void setPendingHit(int damage)
	{
		pendingHitDamage      = damage;
		pendingHitTimestampMs = System.currentTimeMillis();
	}

	public boolean hasPendingHit()
	{
		return pendingHitTimestampMs > 0
			&& System.currentTimeMillis() - pendingHitTimestampMs < 900;
	}

	/** Active overhead prayer (null = none). Updated each game tick. */
	@Getter @Setter
	private HeadIcon overheadPrayer;

	public boolean isSmiteActive()
	{
		return overheadPrayer == HeadIcon.SMITE;
	}

	public boolean isTracked()
	{
		return name != null;
	}

	public void reset()
	{
		name                  = null;
		estimatedHp           = -1;
		maxHp                 = -1;
		vengActive            = false;
		lastOutgoingHit       = -1;
		overheadPrayer        = null;
		pendingHitDamage      = 0;
		pendingHitTimestampMs = -1;
	}
}
