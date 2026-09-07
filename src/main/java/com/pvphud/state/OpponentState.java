package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** State for the current PvP opponent (left panel). */
public class OpponentState
{
	/** Opponent's display name; null when no opponent is tracked. */
	@Getter @Setter
	private String name;

	/**
	 * Opponent's estimated current HP. -1 means unknown.
	 * Updated from HitsplatApplied events and any available HP metadata.
	 */
	@Getter @Setter
	private int estimatedHp = -1;

	/**
	 * Opponent's max HP. -1 means unknown.
	 * Populated asynchronously; never blocks combat initiation.
	 */
	@Getter @Setter
	private int maxHp = -1;

	/** Wall-clock deadline for opponent's Vengeance; -1 = no veng. */
	private long vengExpiryMs = -1;

	/** Most recent outgoing hit dealt to the opponent (from XP drop / hitsplat). */
	@Getter @Setter
	private int lastOutgoingHit = -1;

	/** Accumulated damage dealt this fight, used for max HP back-calculation. */
	@Getter @Setter
	private int totalDamageDealt = 0;

	public boolean isVengActive()
	{
		return vengExpiryMs > 0 && System.currentTimeMillis() < vengExpiryMs;
	}

	/** Mark the opponent's Vengeance as active; expires after 30 s. */
	public void markVengActive()
	{
		vengExpiryMs = System.currentTimeMillis() + 30_000L;
	}

	public void clearVeng()
	{
		vengExpiryMs = -1;
	}

	public boolean isTracked()
	{
		return name != null;
	}

	public void reset()
	{
		name = null;
		estimatedHp = -1;
		maxHp = -1;
		vengExpiryMs = -1;
		lastOutgoingHit = -1;
		totalDamageDealt = 0;
	}
}
