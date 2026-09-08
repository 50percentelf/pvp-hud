package com.pvphud;

import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.PrayerEffect;
import com.pvphud.state.PvpFightSession;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates the state transitions for every fight-termination path:
 *   A. Stale timeout — 50-tick idle in onGameTick
 *   B. Opponent death — ratio==0 in pollOpponentHealth
 *   C. Local player death — HP==0 in onStatChanged
 *   D. Null-safe endSession (no session active)
 *   E. Session totals preserved on endSession
 */
public class FightLifecycleTest
{
	// A — stale timeout
	@Test
	public void staleTimeout_endSessionAndReset_leavesNoSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Bob", 100);
		state.getOpponent().setName("Bob");

		// Simulate: session.isStale() → hudState.endSession() + opponent.reset()
		state.endSession();
		state.getOpponent().reset();

		assertNull("session must be null after stale termination", state.getCurrentSession());
		assertFalse("opponent must not be tracked after reset", state.getOpponent().isTracked());
	}

	@Test
	public void staleTimeout_isStale_firesAfter50Ticks()
	{
		PvpFightSession session = new PvpFightSession("Bob", 100);
		assertFalse("fresh session is not stale", session.isStale(100));
		assertFalse("49 ticks of idle is not stale", session.isStale(149));
		assertTrue("50 ticks of idle triggers stale", session.isStale(150));
		assertTrue("100 ticks of idle triggers stale", session.isStale(200));
	}

	@Test
	public void staleTimeout_combatRefreshes_idleClock()
	{
		PvpFightSession session = new PvpFightSession("Bob", 100);
		// A hit on tick 130 resets the idle clock to tick 130
		session.onOutgoingHit(
			new CombatEvent(CombatEventType.OUTGOING_HIT, 10, 0, 0,
				PrayerEffect.NONE, false, false, System.currentTimeMillis()),
			130);
		assertFalse("49 ticks after last combat is not stale", session.isStale(179));
		assertTrue("50 ticks after last combat is stale", session.isStale(180));
	}

	// B — opponent death
	@Test
	public void opponentDeath_endSessionAndReset_leavesNoSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Alice", 50);
		state.getOpponent().setName("Alice");
		assertTrue("session exists before death", state.getOpponent().isTracked());

		// Simulate: ratio==0 → endSession() + opponent.reset()
		state.endSession();
		state.getOpponent().reset();

		assertNull("session null after opponent death", state.getCurrentSession());
		assertFalse("opponent not tracked after death reset", state.getOpponent().isTracked());
	}

	@Test
	public void opponentDeath_endSessionWithoutReset_wouldLeaveOpponentTracked()
	{
		// Confirms WHY the fix was necessary: endSession alone is insufficient.
		PvpHudState state = new PvpHudState();
		state.beginSession("Alice", 50);
		state.getOpponent().setName("Alice");

		state.endSession(); // no opponent.reset()

		assertNull("session is null", state.getCurrentSession());
		assertTrue("opponent still tracked (no reset called)", state.getOpponent().isTracked());
	}

	// C — local player death
	@Test
	public void localDeath_endSessionAndReset_leavesNoSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Eve", 200);
		state.getOpponent().setName("Eve");

		// Simulate: HP==0 && prevHp>0 → endSession() + opponent.reset()
		state.endSession();
		state.getOpponent().reset();

		assertNull("session null after local death", state.getCurrentSession());
		assertFalse("opponent not tracked after local death", state.getOpponent().isTracked());
	}

	// D — null-safe endSession
	@Test
	public void endSession_whenNoSession_isNoop()
	{
		PvpHudState state = new PvpHudState();
		// No exception; sessionEndedMs stays at -1 (no session has ever ended).
		state.endSession();
		assertEquals(-1L, state.getSessionEndedMs());
		assertNull(state.getCurrentSession());
	}

	// E — totals preserved
	@Test
	public void sessionTotals_preservedAfterEndSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Zed", 0);
		state.getOpponent().setName("Zed");

		PvpFightSession session = state.getCurrentSession();
		session.onOutgoingHit(
			new CombatEvent(CombatEventType.OUTGOING_HIT, 42, 0, 0,
				PrayerEffect.NONE, false, false, System.currentTimeMillis()),
			1);
		session.onIncomingHit(
			new CombatEvent(CombatEventType.INCOMING_HIT, 18, 0, 0,
				PrayerEffect.NONE, false, false, System.currentTimeMillis()),
			2);

		state.endSession();
		state.getOpponent().reset();

		assertEquals(42, state.getLastTotalOutgoing());
		assertEquals(18, state.getLastTotalIncoming());
		assertNull(state.getCurrentSession());
	}

	// Extra — terminated session.isStale() is always false (already ended)
	@Test
	public void terminatedSession_isNeverStale()
	{
		// isStale() guards against re-terminating. A session that was explicitly
		// terminated is never considered stale — the stale check is only for live sessions.
		PvpFightSession session = new PvpFightSession("Bob", 100);
		session.terminate();
		assertFalse("terminated session is not stale (already ended)", session.isStale(100));
		assertFalse("terminated session is not stale far in future", session.isStale(100_000));
	}
}
