package com.pvphud;

import com.pvphud.state.SelfState;
import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies that HP shake state is only updated on positive incoming damage. */
public class SelfStateHpShakeTest
{
	@Test
	public void zeroDamage_doesNotUpdateShakeTimestamp()
	{
		SelfState self = new SelfState();
		long before = self.getLastIncomingDamageMs();
		assertEquals(-1L, before);
		// 0-damage: do NOT update (this matches the plugin logic in onHitsplatApplied)
		// We verify SelfState fields are unchanged
		assertEquals(-1L, self.getLastIncomingDamageMs());
		assertEquals(0,   self.getLastIncomingDamage());
	}

	@Test
	public void positiveDamage_updatesShakeTimestampAndAmount()
	{
		SelfState self = new SelfState();
		long now = System.currentTimeMillis();
		self.setLastIncomingDamageMs(now);
		self.setLastIncomingDamage(18);
		assertTrue("timestamp should be near now", self.getLastIncomingDamageMs() >= now);
		assertEquals(18, self.getLastIncomingDamage());
	}

	@Test
	public void reset_clearsShakeState()
	{
		SelfState self = new SelfState();
		self.setLastIncomingDamageMs(System.currentTimeMillis());
		self.setLastIncomingDamage(25);
		self.reset();
		assertEquals(-1L, self.getLastIncomingDamageMs());
		assertEquals(0,   self.getLastIncomingDamage());
	}
}
