package com.pvphud;

import com.pvphud.state.OpponentState;
import com.pvphud.state.SelfState;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransientHitStateTest
{
	@Test
	public void outgoing_positive_setsHitAndTimestamp()
	{
		OpponentState opp = new OpponentState();
		long before = System.currentTimeMillis();
		opp.setLastOutgoingHit(34);
		opp.setLastOutgoingHitMs(System.currentTimeMillis());
		assertEquals(34, opp.getLastOutgoingHit());
		assertTrue(opp.getLastOutgoingHitMs() >= before);
	}

	@Test
	public void outgoing_reset_clearsAll()
	{
		OpponentState opp = new OpponentState();
		opp.setLastOutgoingHit(34);
		opp.setLastOutgoingHitMs(1000L);
		opp.reset();
		assertEquals(-1, opp.getLastOutgoingHit());
		assertEquals(-1L, opp.getLastOutgoingHitMs());
	}

	@Test
	public void incoming_zeroDamage_defaultsToZero()
	{
		SelfState self = new SelfState();
		assertEquals(0, self.getLastIncomingDamage());
		assertEquals(-1L, self.getLastIncomingDamageMs());
	}

	@Test
	public void incoming_positive_setsHitAndTimestamp()
	{
		SelfState self = new SelfState();
		long before = System.currentTimeMillis();
		self.setLastIncomingDamage(52);
		self.setLastIncomingDamageMs(System.currentTimeMillis());
		assertEquals(52, self.getLastIncomingDamage());
		assertTrue(self.getLastIncomingDamageMs() >= before);
	}

	@Test
	public void incoming_reset_clearsAll()
	{
		SelfState self = new SelfState();
		self.setLastIncomingDamage(52);
		self.setLastIncomingDamageMs(2000L);
		self.reset();
		assertEquals(0, self.getLastIncomingDamage());
		assertEquals(-1L, self.getLastIncomingDamageMs());
	}
}
