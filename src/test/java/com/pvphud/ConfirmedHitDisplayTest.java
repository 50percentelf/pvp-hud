package com.pvphud;

import com.pvphud.state.DamageExchange;
import com.pvphud.state.PvpFightSession;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Regression tests for factual confirmed-damage display.
 * Verifies same-tick total, different-tick separation, isMine() filtering.
 * No prediction logic — factual only.
 */
public class ConfirmedHitDisplayTest
{
	// Tests use PvpFightSession + DamageExchange directly — no plugin needed.

	@Test
	public void singleHit_confirmedValueIsDamage()
	{
		PvpFightSession session = new PvpFightSession("bob", 100);
		session.onOutgoingHit(34, 0, 100);
		DamageExchange ex = session.getExchangeForTick(100);
		assertNotNull(ex);
		assertEquals(34, ex.getOutgoingTotal());
	}

	@Test
	public void sameTickMultiHit_confirmedValueIsTotal()
	{
		PvpFightSession session = new PvpFightSession("bob", 100);
		session.onOutgoingHit(22, 0, 100);
		session.onOutgoingHit(16, 0, 100);
		DamageExchange ex = session.getExchangeForTick(100);
		assertNotNull(ex);
		assertEquals(38, ex.getOutgoingTotal());
		assertEquals(2,  ex.getOutgoingHitCount());
	}

	@Test
	public void differentTick_eachTickHasOwnExchange()
	{
		PvpFightSession session = new PvpFightSession("bob", 100);
		session.onOutgoingHit(22, 0, 100);
		session.onOutgoingHit(16, 0, 102);
		DamageExchange ex100 = session.getExchangeForTick(100);
		DamageExchange ex102 = session.getExchangeForTick(102);
		assertNotNull(ex100);
		assertNotNull(ex102);
		assertEquals(22, ex100.getOutgoingTotal());
		assertEquals(16, ex102.getOutgoingTotal());
	}

	@Test
	public void thirdPartyHit_notInOutgoing()
	{
		// isMine() filtering is in the plugin event handler.
		// Here we verify that if only a third-party hit is recorded as incoming
		// (not outgoing), the outgoing total stays 0.
		PvpFightSession session = new PvpFightSession("bob", 100);
		session.onIncomingHit(30, 100); // not ours
		DamageExchange ex = session.getExchangeForTick(100);
		assertNotNull(ex);
		assertEquals(0, ex.getOutgoingTotal());
		assertEquals(30, ex.getIncomingTotal());
	}
}
