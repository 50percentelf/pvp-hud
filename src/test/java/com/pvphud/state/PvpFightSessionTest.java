package com.pvphud.state;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PvpFightSessionTest
{
	// ── Totals ────────────────────────────────────────────────────────────────

	@Test
	public void totals_outgoing_accumulateAcrossTicks()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(30, 0, 1);
		s.onOutgoingHit(15, 0, 2);
		assertEquals(45, s.getTotalOutgoing());
		assertEquals(0, s.getTotalIncoming());
	}

	@Test
	public void totals_incoming_accumulateAcrossTicks()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onIncomingHit(40, 1);
		s.onIncomingHit(20, 2);
		assertEquals(0, s.getTotalOutgoing());
		assertEquals(60, s.getTotalIncoming());
	}

	@Test
	public void totals_mixed_accumulateSeparately()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(42, 0, 1);
		s.onIncomingHit(17, 1);
		s.onOutgoingHit(28, 0, 2);
		assertEquals(70, s.getTotalOutgoing());
		assertEquals(17, s.getTotalIncoming());
	}

	// ── Exchange buffer ───────────────────────────────────────────────────────

	@Test
	public void exchange_sameTick_aggregatesIntoOneRecord()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(10, 0, 5);
		s.onOutgoingHit(20, 0, 5);
		List<DamageExchange> ex = s.getRecentExchanges();
		assertEquals(1, ex.size());
		assertEquals(30, ex.get(0).getOutgoingTotal());
	}

	@Test
	public void exchange_differentTicks_createSeparateRecords()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(10, 0, 5);
		s.onOutgoingHit(20, 0, 6);
		assertEquals(2, s.getRecentExchanges().size());
	}

	@Test
	public void exchange_newestFirst()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(10, 0, 5);
		s.onOutgoingHit(20, 0, 6);
		List<DamageExchange> ex = s.getRecentExchanges();
		assertEquals(6, ex.get(0).getTick());
		assertEquals(5, ex.get(1).getTick());
	}

	@Test
	public void exchange_bufferCappedAtSix()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		for (int t = 1; t <= 8; t++)
			s.onOutgoingHit(1, 0, t);
		assertEquals(6, s.getRecentExchanges().size());
		assertEquals(8, s.getRecentExchanges().get(0).getTick());
	}

	@Test
	public void exchange_incomingAndOutgoing_sameTickShareRecord()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(30, 0, 7);
		s.onIncomingHit(15, 7);
		List<DamageExchange> ex = s.getRecentExchanges();
		assertEquals(1, ex.size());
		assertTrue(ex.get(0).hasOutgoing());
		assertTrue(ex.get(0).hasIncoming());
		assertEquals(30, ex.get(0).getOutgoingTotal());
		assertEquals(15, ex.get(0).getIncomingTotal());
	}

	@Test
	public void exchange_prayerDrain_storedInExchange()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(40, 10, 3);
		assertEquals(10, s.getRecentExchanges().get(0).getOutgoingPrayerDrain());
	}

	@Test
	public void exchange_emptyInitially()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertTrue(s.getRecentExchanges().isEmpty());
	}

	// ── touchCombat ──────────────────────────────────────────────────────────

	@Test
	public void touchCombat_updatesLastCombatTick_noTotalsOrExchanges()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.touchCombat(30);
		assertEquals(30, s.getLastCombatTick());
		assertEquals(0, s.getTotalOutgoing());
		assertEquals(0, s.getTotalIncoming());
		assertTrue(s.getRecentExchanges().isEmpty());
	}

	@Test
	public void touchCombat_preventsStale()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.touchCombat(40);
		assertFalse(s.isStale(89));
		assertTrue(s.isStale(90));
	}

	// ── Stale detection ───────────────────────────────────────────────────────

	@Test
	public void stale_afterTimeoutWithNoCombat()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		assertFalse(s.isStale(49));
		assertTrue(s.isStale(50));
	}

	@Test
	public void stale_combatResetsTimeout()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.onOutgoingHit(10, 0, 40);
		assertFalse(s.isStale(89));
		assertTrue(s.isStale(90));
	}

	@Test
	public void stale_terminatedSessionNeverStale()
	{
		PvpFightSession s = new PvpFightSession("Target", 0);
		s.terminate();
		assertFalse(s.isStale(1000));
	}
}
