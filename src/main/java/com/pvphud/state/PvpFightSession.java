package com.pvphud.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
 * Fight totals and the exchange buffer live here so they are not wiped by
 * transient InteractingChanged events.
 */
public class PvpFightSession
{
	/** Ticks with no combat in either direction before the session self-expires. ~30 s. */
	private static final int TIMEOUT_TICKS = 50;
	private static final int MAX_EXCHANGES = 6;

	@Getter private final String opponentName;
	@Getter private final int    startTick;
	@Getter private int          lastCombatTick;

	/** Total damage this player dealt to the opponent this session. */
	@Getter private int totalOutgoing;
	/** Total damage this player received this session. */
	@Getter private int totalIncoming;

	/** True once terminate() has been called; immutable after that. */
	@Getter private boolean terminated;

	/** Tick-keyed exchange records, newest first, capped at MAX_EXCHANGES. */
	private final ArrayDeque<DamageExchange> exchanges = new ArrayDeque<>(MAX_EXCHANGES);

	public PvpFightSession(String opponentName, int startTick)
	{
		this.opponentName   = opponentName;
		this.startTick      = startTick;
		this.lastCombatTick = startTick;
	}

	/**
	 * Records an owned outgoing hit. Only call when hitsplat.isMine() is true.
	 * Updates session totals and the exchange buffer for {@code currentTick}.
	 */
	public void onOutgoingHit(int damage, int prayerDrain, int currentTick)
	{
		lastCombatTick = currentTick;
		totalOutgoing  += damage;
		getOrCreate(currentTick).addOutgoing(damage, prayerDrain);
	}

	/**
	 * Records an incoming hit on the local player during an active session.
	 */
	public void onIncomingHit(int damage, int currentTick)
	{
		lastCombatTick = currentTick;
		totalIncoming  += damage;
		getOrCreate(currentTick).addIncoming(damage);
	}

	/**
	 * Marks combat activity without recording damage — used for 0-splats that
	 * confirm ongoing engagement without contributing to session totals.
	 */
	public void touchCombat(int currentTick)
	{
		lastCombatTick = currentTick;
	}

	private DamageExchange getOrCreate(int tick)
	{
		DamageExchange head = exchanges.peekFirst();
		if (head != null && head.getTick() == tick) return head;
		if (exchanges.size() >= MAX_EXCHANGES) exchanges.pollLast();
		DamageExchange ex = new DamageExchange(tick);
		exchanges.addFirst(ex);
		return ex;
	}

	/** Recent exchanges newest-first; never null. */
	public List<DamageExchange> getRecentExchanges()
	{
		return Collections.unmodifiableList(new ArrayList<>(exchanges));
	}

	/** Returns the exchange record for {@code tick}, or null if none exists. */
	public DamageExchange getExchangeForTick(int tick)
	{
		for (DamageExchange ex : exchanges)
			if (ex.getTick() == tick) return ex;
		return null;
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
