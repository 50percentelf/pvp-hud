package com.pvphud.state;

import java.util.List;
import lombok.Getter;

/**
 * Lifecycle-managed snapshot of a single PvP encounter.
 *
 * A session begins when the local player first interacts with a named player.
 * It survives brief null interacting (movement, targeting flickers) and only
 * terminates on:
 *   - Opponent death (HP ratio → 0 via pollOpponentHealth)
 *   - Meaningful replacement by a different opponent after TIMEOUT_TICKS idle
 *   - Logout or world hop
 *   - Explicit eviction (plugin shutdown)
 *
 * Fight totals and the event log live here so they are not wiped by transient
 * InteractingChanged events.
 */
public class PvpFightSession
{
	/** Ticks with no combat in either direction before the session self-expires. ~30 s. */
	private static final int TIMEOUT_TICKS = 50;

	@Getter private final String opponentName;
	@Getter private final int    startTick;
	@Getter private int          lastCombatTick;

	private final CombatEventState events = new CombatEventState();

	/** Total damage this player dealt to the opponent this session. */
	@Getter private int totalOutgoing;
	/** Total damage this player received this session. */
	@Getter private int totalIncoming;

	/** True once terminate() has been called; immutable after that. */
	@Getter private boolean terminated;

	// ── CHANCE! detection — rolling max per direction ──────────────────────────
	private int     maxOutgoingHit;
	private int     maxIncomingHit;
	private boolean hasOutgoingHit;
	private boolean hasIncomingHit;

	// ── Same-tick STACK detection ──────────────────────────────────────────────
	private int lastOutgoingHitTick = -1;
	private int lastIncomingHitTick = -1;

	public PvpFightSession(String opponentName, int startTick)
	{
		this.opponentName  = opponentName;
		this.startTick     = startTick;
		this.lastCombatTick = startTick;
	}

	/** Record an outgoing hit. Updates total, event log, and tracking state. */
	public void onOutgoingHit(CombatEvent event, int currentTick)
	{
		lastCombatTick      = currentTick;
		lastOutgoingHitTick = currentTick;
		totalOutgoing       += event.getDamage();
		events.post(event);
	}

	/** Record an incoming hit. Updates total, event log, and tracking state. */
	public void onIncomingHit(CombatEvent event, int currentTick)
	{
		lastCombatTick      = currentTick;
		lastIncomingHitTick = currentTick;
		totalIncoming       += event.getDamage();
		events.post(event);
	}

	/**
	 * Returns true if this outgoing hit ties or beats the rolling per-session max,
	 * meaning it qualifies as a CHANCE! event. Also updates the max.
	 * The first outgoing hit is never flagged (no baseline to compare against).
	 */
	public boolean checkOutgoingChance(int damage)
	{
		boolean chance = hasOutgoingHit && damage > 0 && damage >= maxOutgoingHit;
		if (damage > 0 && damage >= maxOutgoingHit) maxOutgoingHit = damage;
		hasOutgoingHit = true;
		return chance;
	}

	/**
	 * Returns true if this incoming hit ties or beats the rolling per-session max.
	 * The first incoming hit is never flagged.
	 */
	public boolean checkIncomingChance(int damage)
	{
		boolean chance = hasIncomingHit && damage > 0 && damage >= maxIncomingHit;
		if (damage > 0 && damage >= maxIncomingHit) maxIncomingHit = damage;
		hasIncomingHit = true;
		return chance;
	}

	/** True if an outgoing hit on {@code tick} lands the same tick as the last incoming hit. */
	public boolean isOutgoingStack(int tick)
	{
		return lastIncomingHitTick >= 0 && tick == lastIncomingHitTick;
	}

	/** True if an incoming hit on {@code tick} lands the same tick as the last outgoing hit. */
	public boolean isIncomingStack(int tick)
	{
		return lastOutgoingHitTick >= 0 && tick == lastOutgoingHitTick;
	}

	/** Recent events newest-first; never null. */
	public List<CombatEvent> getRecentEvents()
	{
		return events.getRecentEvents();
	}

	/**
	 * True when no combat has occurred for TIMEOUT_TICKS and the session has
	 * not been explicitly terminated. Checked each game tick by the plugin.
	 */
	public boolean isStale(int currentTick)
	{
		return !terminated && (currentTick - lastCombatTick) >= TIMEOUT_TICKS;
	}

	/** Permanently close this session. Safe to call multiple times. */
	public void terminate()
	{
		terminated = true;
	}
}
