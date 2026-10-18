package com.pvphud;

import com.pvphud.state.SelfState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Exercises the production {@link PvpHudPlugin#recordIncomingDamage(SelfState, int, long)}
 * helper used by {@code onHitsplatApplied}. Zero and negative damage must leave
 * both shake fields untouched; positive damage must overwrite both.
 */
public class SelfStateHpShakeTest
{
	@Test
	public void zeroDamage_doesNotUpdate()
	{
		SelfState self = new SelfState();
		long before = self.getLastIncomingDamageMs();
		PvpHudPlugin.recordIncomingDamage(self, 0, 99999L);
		assertEquals("timestamp unchanged on zero damage", before, self.getLastIncomingDamageMs());
		assertEquals("amount unchanged on zero damage", 0, self.getLastIncomingDamage());
	}

	@Test
	public void negativeDamage_doesNotUpdate()
	{
		SelfState self = new SelfState();
		long before = self.getLastIncomingDamageMs();
		PvpHudPlugin.recordIncomingDamage(self, -1, 99999L);
		assertEquals(before, self.getLastIncomingDamageMs());
		assertEquals(0, self.getLastIncomingDamage());
	}

	@Test
	public void positiveDamage_updatesTimestampAndAmount()
	{
		SelfState self = new SelfState();
		PvpHudPlugin.recordIncomingDamage(self, 18, 12345L);
		assertEquals(12345L, self.getLastIncomingDamageMs());
		assertEquals(18, self.getLastIncomingDamage());
	}

	@Test
	public void secondHit_overwritesBothFields()
	{
		SelfState self = new SelfState();
		PvpHudPlugin.recordIncomingDamage(self, 10, 1000L);
		PvpHudPlugin.recordIncomingDamage(self, 25, 2000L);
		assertEquals(2000L, self.getLastIncomingDamageMs());
		assertEquals(25, self.getLastIncomingDamage());
	}

	@Test
	public void reset_clearsShakeState()
	{
		SelfState self = new SelfState();
		PvpHudPlugin.recordIncomingDamage(self, 25, System.currentTimeMillis());
		self.reset();
		assertEquals(-1L, self.getLastIncomingDamageMs());
		assertEquals(0,   self.getLastIncomingDamage());
	}
}
