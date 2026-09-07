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

	// ── STACK detection — multiple damage sources on the same actor in one tick ─
	// Outgoing stack: e.g. weapon hit + recoil ring on the opponent.
	// Incoming stack: e.g. opponent weapon hit + their vengeance on us.
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
	 * Records an outgoing hit tick and returns true if a second (or later) outgoing
	 * hitsplat landed on the opponent in the same game tick — indicating a damage
	 * stack (e.g. weapon hit + recoil ring both resolving at once).
	 */
	public boolean recordAndCheckOutgoingStack(int tick)
	{
		boolean stack = (tick == lastOutgoingHitTick);
		lastOutgoingHitTick = tick;
		return stack;
	}

	/**
	 * Records an incoming hit tick and returns true if a second (or later) incoming
	 * hitsplat landed on the local player in the same game tick — indicating a
	 * damage stack (e.g. opponent weapon hit + their vengeance both resolving at once).
	 */
	public boolean recordAndCheckIncomingStack(int tick)
	{
		boolean stack = (tick == lastIncomingHitTick);
		lastIncomingHitTick = tick;
		return stack;
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
