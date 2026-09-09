package com.pvphud;

import com.pvphud.state.SelfState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates Tele Block timer decoding against Varbits.TELEBLOCK semantics.
 *
 * Raw varbit layout (mirrors RuneLite TimersAndBuffsPlugin):
 *   0        = TB not active, no immunity
 *   1..100   = post-TB reapplication immunity only; TB itself has expired
 *   101+     = TB active; remaining ticks = raw - 100
 *
 * Normal PvP TB:           raw 600 → 500 ticks (5:00)
 * Protect from Magic TB:   raw 350 → 250 ticks (2:30)
 * Boundary:                raw 101 → 1 tick
 * Expiry boundary:         raw 100 → 0 (timer disappears)
 * Post-TB immunity:        raw 1..99 → 0 (NOT shown as active TB)
 */
public class TeleBlockTimerTest
{
	// ── Decode function: raw → active TB ticks ────────────────────────────────

	@Test
	public void decode_raw600_returns500()
	{
		assertEquals(500, PvpHudPlugin.decodeTeleblockTicks(600));
	}

	@Test
	public void decode_raw350_returns250()
	{
		assertEquals(250, PvpHudPlugin.decodeTeleblockTicks(350));
	}

	@Test
	public void decode_raw101_returns1()
	{
		assertEquals(1, PvpHudPlugin.decodeTeleblockTicks(101));
	}

	@Test
	public void decode_raw100_returns0()
	{
		// Boundary: TB has just expired; immunity window starts
		assertEquals(0, PvpHudPlugin.decodeTeleblockTicks(100));
	}

	@Test
	public void decode_raw99_returns0()
	{
		// Deep in immunity window — must NOT be presented as active TB
		assertEquals(0, PvpHudPlugin.decodeTeleblockTicks(99));
	}

	@Test
	public void decode_raw1_returns0()
	{
		// Tail of immunity window
		assertEquals(0, PvpHudPlugin.decodeTeleblockTicks(1));
	}

	@Test
	public void decode_raw0_returns0()
	{
		// Fully inactive
		assertEquals(0, PvpHudPlugin.decodeTeleblockTicks(0));
	}

	// ── Login hydration uses decoded value ────────────────────────────────────

	@Test
	public void loginHydration_activeBlock_storesDecodedTicks()
	{
		// Simulates initSelfState() storing decodeTeleblockTicks(rawVarbit)
		SelfState self = new SelfState();
		int raw = 600;
		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(raw));

		assertEquals(500, self.getTeleBlockTicksRemaining());
	}

	@Test
	public void loginHydration_immunityOnly_storesZero()
	{
		// Raw 50 = post-TB immunity; must not show as active TB on login
		SelfState self = new SelfState();
		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(50));

		assertEquals(0, self.getTeleBlockTicksRemaining());
	}

	// ── VarbitChanged uses decoded value ──────────────────────────────────────

	@Test
	public void varbitChanged_activeBlock_storesDecodedTicks()
	{
		SelfState self = new SelfState();
		int incomingVarbit = 350; // protect-from-magic TB
		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(incomingVarbit));

		assertEquals(250, self.getTeleBlockTicksRemaining());
	}

	@Test
	public void varbitChanged_reachesExpiry_timerDisappears()
	{
		// Raw varbit counts down to 100 → decoded = 0 → timer clears
		SelfState self = new SelfState();
		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(101)); // 1 tick left
		assertEquals(1, self.getTeleBlockTicksRemaining());

		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(100)); // hits 0
		assertEquals(0, self.getTeleBlockTicksRemaining());
	}

	@Test
	public void varbitChanged_immunityPhase_timerStaysZero()
	{
		// After TB expires, varbit stays 1..100 (immunity). Decoded stays 0 throughout.
		SelfState self = new SelfState();
		for (int raw = 100; raw >= 1; raw--)
		{
			self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(raw));
			assertEquals("raw=" + raw + " is immunity, must decode to 0",
				0, self.getTeleBlockTicksRemaining());
		}
	}

	// ── No raw-value bleed-through ────────────────────────────────────────────

	@Test
	public void rawValueIsNeverStoredDirectly()
	{
		// If the bug existed, raw 600 would be stored and show ~6 min instead of ~5 min.
		SelfState self = new SelfState();
		self.setTeleBlockTicksRemaining(PvpHudPlugin.decodeTeleblockTicks(600));

		assertNotEquals("Raw value must not be stored directly",
			600, self.getTeleBlockTicksRemaining());
		assertEquals(500, self.getTeleBlockTicksRemaining());
	}
}
