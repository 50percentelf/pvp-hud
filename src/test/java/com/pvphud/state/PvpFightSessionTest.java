package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class PvpFightSessionTest
{
	private static CombatEvent outHit(int dmg)
	{
		return new CombatEvent(CombatEventType.OUTGOING_HIT, dmg,
			0, 0, PrayerEffect.NONE, false, false, System.currentTimeMillis());
	}

	private static CombatEvent inHit(int dmg)
	{
		return new CombatEvent(CombatEventType.INCOMING_HIT, dmg,
			0, 0, PrayerEffect.NONE, false, false, System.currentTimeMillis());
	}

	// ── Outgoing stack detection ───────────────────────────────────────────────

	@Test
	public void outgoingStack_firstHitOnTick_notAStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertFalse(s.recordAndCheckOutgoingStack(10));
	}

	@Test
	public void outgoingStack_secondHitSameTick_isStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.recordAndCheckOutgoingStack(10); // first
		assertTrue(s.recordAndCheckOutgoingStack(10)); // second on same tick
	}

	@Test
	public void outgoingStack_nextTickNotAStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.recordAndCheckOutgoingStack(10);
		s.recordAndCheckOutgoingStack(10); // stack at tick 10
		assertFalse(s.recordAndCheckOutgoingStack(11)); // new tick — not a stack
	}

	@Test
	public void outgoingStack_multipleHitsSameTick_allAfterFirstAreStacks()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertFalse(s.recordAndCheckOutgoingStack(5));
		assertTrue(s.recordAndCheckOutgoingStack(5));
		assertTrue(s.recordAndCheckOutgoingStack(5));
	}

	// ── Incoming stack detection ───────────────────────────────────────────────

	@Test
	public void incomingStack_firstHitOnTick_notAStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertFalse(s.recordAndCheckIncomingStack(10));
	}

	@Test
	public void incomingStack_secondHitSameTick_isStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.recordAndCheckIncomingStack(10);
		assertTrue(s.recordAndCheckIncomingStack(10));
	}

	@Test
	public void incomingStack_nextTickNotAStack()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.recordAndCheckIncomingStack(10);
		s.recordAndCheckIncomingStack(10); // stack at tick 10
		assertFalse(s.recordAndCheckIncomingStack(11));
	}

	// ── Outgoing and incoming stacks are independent ───────────────────────────

	@Test
	public void stackTrackers_areIndependent()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		// Record an outgoing hit at tick 7
		s.recordAndCheckOutgoingStack(7);
		// An incoming hit at the same tick is NOT a stack on the incoming tracker
		assertFalse(s.recordAndCheckIncomingStack(7));
	}

	// ── Totals ────────────────────────────────────────────────────────────────

	@Test
	public void totals_accumulateCorrectly()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(outHit(30), 1);
		s.onOutgoingHit(outHit(15), 2);
		s.onIncomingHit(inHit(40), 3);
		assertEquals(45, s.getTotalOutgoing());
		assertEquals(40, s.getTotalIncoming());
	}

	// ── Stale detection ───────────────────────────────────────────────────────

	@Test
	public void stale_afterTimeoutWithNoCombat()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertFalse(s.isStale(49));  // 49 ticks — still within window
		assertTrue(s.isStale(50));   // 50 ticks — stale
	}

	@Test
	public void stale_combatResetsTimeout()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(outHit(10), 40); // combat at tick 40
		assertFalse(s.isStale(89));       // 49 ticks after last combat
		assertTrue(s.isStale(90));        // 50 ticks after last combat
	}

	@Test
	public void stale_terminatedSessionNeverStale()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.terminate();
		assertFalse(s.isStale(1000));
	}
}
