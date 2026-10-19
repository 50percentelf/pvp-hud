package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;

/** Runtime-observable state for the current PvP opponent (left panel). */
public class OpponentState
{
	/** Hiscores-derived base stats; populated asynchronously on combat start. */
	@Getter
	private final OpponentStats stats = new OpponentStats();
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

	/** Most recent outgoing hit dealt to the opponent. -1 = none. */
	@Getter @Setter
	private int lastOutgoingHit = -1;

	/** Wall-clock ms when lastOutgoingHit was set; -1 = none. Used for transient splat display. */
	@Getter @Setter
	private long lastOutgoingHitMs = -1;

	/** Active overhead prayer (null = none). Updated each game tick. */
	@Getter @Setter
	private HeadIcon overheadPrayer;

	/**
	 * Cached Player reference for the tracked opponent.
	 * Set when a session starts; used to avoid scanning client.getPlayers() each tick.
	 * Cleared on reset; re-acquired lazily in pollOpponentHealth if stale.
	 */
	@Getter @Setter
	private Player cachedActor;

	public boolean isTracked()
	{
		return name != null;
	}

	public void reset()
	{
		name                  = null;
		estimatedHp           = -1;
		maxHp                 = -1;
		lastOutgoingHit       = -1;
		lastOutgoingHitMs     = -1;
		overheadPrayer        = null;
		cachedActor           = null;
		stats.reset();
	}
}
