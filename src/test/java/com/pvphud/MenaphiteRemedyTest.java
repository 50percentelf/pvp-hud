package com.pvphud;

import com.pvphud.state.PeriodicEffect;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates Menaphite Remedy two-icon state mechanics:
 *   - totalTicksRemaining: derived from varbit (value * 25); self-decrements each tick
 *   - nextProcTicks: reset to 25 on each varbit decrement; self-decrements each tick
 *   - Startup/mid-effect: nextProcTicks stays 0 until first observed proc
 *   - Effect expiry: both fields clear on varbit → 0
 *
 * Menaphite Remedy varbit:
 *   Starts at 20 on first drink. Decrements by 1 every 25 ticks (each prayer restore proc).
 *   Total duration = 20 * 25 = 500 ticks (~5 min). Proc period = 25 ticks (~15 s).
 */
public class MenaphiteRemedyTest
{
	// ── Fresh effect (player drinks Menaphite Remedy) ─────────────────────────

	@Test
	public void firstDrink_setsTotal_doesNotSetProc()
	{
		// Varbit 0 → 20 (first activation): total = 500 ticks, nextProcTicks stays 0
		// because we don't know where in the 25-tick cycle we are until a proc fires.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(20 * 25); // simulates varbit handler on 0→20
		// nextProcTicks intentionally NOT set (no decrement observed yet)

		assertEquals(500, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
		assertTrue(men.isActive());
	}

	// ── Proc fires (varbit decrements) ───────────────────────────────────────

	@Test
	public void proc_varbitDecrement_syncsNextProcTo25()
	{
		// Simulates varbit 20 → 19: a proc just fired, next restore is 25 ticks away.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(19 * 25); // 475 ticks
		men.setNextProcTicks(25);

		assertEquals(475, men.getTotalTicksRemaining());
		assertEquals(25, men.getNextProcTicks());
	}

	// ── Self-decrement between proc events ───────────────────────────────────

	@Test
	public void tick_decrementsTotal_andNextProc()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(475);
		men.setNextProcTicks(25);

		men.tick();

		assertEquals(474, men.getTotalTicksRemaining());
		assertEquals(24, men.getNextProcTicks());
	}

	@Test
	public void tick_24times_nextProcReachesOne()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(475);
		men.setNextProcTicks(25);

		for (int i = 0; i < 24; i++) men.tick();

		assertEquals(451, men.getTotalTicksRemaining());
		assertEquals(1, men.getNextProcTicks());
	}

	@Test
	public void tick_25times_nextProcReachesZero()
	{
		// After 25 ticks the proc fires and the varbit would decrement again.
		// nextProcTicks reaches 0; the varbit handler will reset it to 25.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(475);
		men.setNextProcTicks(25);

		for (int i = 0; i < 25; i++) men.tick();

		assertEquals(450, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
	}

	// ── Varbit resync corrects self-decrement drift ───────────────────────────

	@Test
	public void varbitResync_after25Ticks_alignsTotal()
	{
		// After 25 ticks of self-decrement, the varbit fires again and resets.
		// Both values should be consistent.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(19 * 25); // 475, varbit=19
		men.setNextProcTicks(25);

		for (int i = 0; i < 25; i++) men.tick(); // 25 self-decrements → 450, nextProc=0

		// varbit fires: 19 → 18; simulates what onVarbitChanged does
		men.setTotalTicksRemaining(18 * 25); // 450
		men.setNextProcTicks(25);

		assertEquals(450, men.getTotalTicksRemaining());
		assertEquals(25, men.getNextProcTicks());
	}

	// ── Startup mid-effect (nextProcTicks unknown) ────────────────────────────

	@Test
	public void midEffect_startup_nextProcTicksZero()
	{
		// Plugin starts while Menaphite Remedy is already active (varbit = 14).
		// We don't know where in the 25-tick cycle we are.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(14 * 25); // 350 ticks; nextProcTicks stays 0

		assertEquals(350, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
		// NXT entry should NOT be displayed (0 ticks → addTimedEffect skips it)
	}

	@Test
	public void midEffect_firstObservedProc_syncsNextProc()
	{
		// First observed varbit decrement (14 → 13) reveals proc fired; sync nextProcTicks.
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(14 * 25);

		// onVarbitChanged fires 14 → 13 (decrement observed while prevMenaphiteVarbit > 0)
		men.setTotalTicksRemaining(13 * 25);
		men.setNextProcTicks(25);

		assertEquals(325, men.getTotalTicksRemaining());
		assertEquals(25, men.getNextProcTicks());
	}

	// ── Effect expiry ─────────────────────────────────────────────────────────

	@Test
	public void effectExpiry_varbitToZero_clearsBothFields()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(25); // varbit = 1, one proc left
		men.setNextProcTicks(3);

		// varbit fires: 1 → 0 (effect ends)
		men.setTotalTicksRemaining(0);
		men.setNextProcTicks(0);

		assertEquals(0, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
		assertFalse("effect must not be active after varbit reaches 0", men.isActive());
	}

	@Test
	public void effectExpiry_naturalTickdown_doesNotGoNegative()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(1);
		men.setNextProcTicks(1);

		men.tick(); // both reach 0
		men.tick(); // already 0, must not go negative

		assertEquals(0, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
	}

	// ── isActive gate ─────────────────────────────────────────────────────────

	@Test
	public void isActive_trueWhenTotalPositive()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(1);
		assertTrue(men.isActive());
	}

	@Test
	public void isActive_falseWhenTotalZero()
	{
		PeriodicEffect men = new PeriodicEffect();
		assertFalse(men.isActive());
	}

	// ── reset() ───────────────────────────────────────────────────────────────

	@Test
	public void reset_clearsBothFields()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(300);
		men.setNextProcTicks(18);
		men.reset();

		assertEquals(0, men.getTotalTicksRemaining());
		assertEquals(0, men.getNextProcTicks());
		assertFalse(men.isActive());
	}

	// ── setTotalTicksRemaining clamps to zero ─────────────────────────────────

	@Test
	public void setTotal_negativeValue_clampedToZero()
	{
		PeriodicEffect men = new PeriodicEffect();
		men.setTotalTicksRemaining(-5);
		assertEquals(0, men.getTotalTicksRemaining());
	}
}
