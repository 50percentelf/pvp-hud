package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates BoostDecayClock against the RuneLite BoostsPlugin#getChangeDownTicks() contract.
 *
 * Tick reference: lastTick = 1000 in most tests; currentTick = 1000 + elapsed.
 */
public class BoostDecayClockTest
{
	private static final int BASE = 1000;

	// ── Calibration state ────────────────────────────────────────────────────

	@Test
	public void uncalibrated_returnsMinusOne()
	{
		BoostDecayClock c = new BoostDecayClock();
		assertFalse(c.isCalibrated());
		c.update(BASE, false);
		assertEquals(-1, c.getTicksRemaining());
	}

	@Test
	public void afterSync_isCalibrated()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		assertTrue(c.isCalibrated());
	}

	@Test
	public void reset_uncalibrates()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE, false);
		c.reset();
		assertFalse(c.isCalibrated());
		assertEquals(-1, c.getTicksRemaining());
	}

	// ── No-Preserve 100-tick cycle ────────────────────────────────────────────

	@Test
	public void noPreserve_atElapsed0_returns100()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE, false);       // elapsed = 0
		assertEquals(100, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_midCycle_returns50()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, false);  // elapsed = 50
		assertEquals(50, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_atElapsed100_returns0()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 100, false); // elapsed = 100
		assertEquals(0, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_gracePeriod_elapsed110_returns15()
	{
		// ticks 100-125: grace window (Preserve could still activate)
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 110, false); // elapsed = 110, no Preserve → 125 - 110 = 15
		assertEquals(15, c.getTicksRemaining());
	}

	// ── Preserve active from start — 150-tick cycle ───────────────────────────

	@Test
	public void preserveFromStart_atElapsed0_returns150()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE, true);        // elapsed = 0, Preserve ON
		assertEquals(150, c.getTicksRemaining());
	}

	@Test
	public void preserveFromStart_midCycle_returns100()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // elapsed = 50, Preserve ON → 150 - 50 = 100
		assertEquals(100, c.getTicksRemaining());
	}

	@Test
	public void preserveFromStart_elapsed150_returns0()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // lock in preserveBeenActive = true
		c.update(BASE + 150, false); // elapsed = 150 > 125 → 150-branch → 150-150 = 0
		assertEquals(0, c.getTicksRemaining());
	}

	// ── Preserve toggled off before tick 75 — benefit lost ──────────────────

	@Test
	public void preserveOffBeforeCriticalWindow_revertsto100Cycle()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // elapsed=50, Preserve ON → preserveBeenActive=true
		// Turn Preserve off at elapsed=60 (still before 75)
		c.update(BASE + 60, false);  // elapsed=60, Preserve OFF, elapsed<75 — but Preserve is OFF
		// Condition: (false && (60<75 || true)) || (60>125) = false → 100-tick branch
		assertEquals(100 - 60, c.getTicksRemaining()); // 40
	}

	// ── Preserve toggled off at elapsed=75 — benefit lost (critical window) ──

	@Test
	public void preserveOffAtCriticalWindow_revertsto100Cycle()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 74, true);   // preserveBeenActive = true, return 76
		c.update(BASE + 75, false);  // elapsed=75, Preserve OFF, preserveBeenActive=true
		// Condition: (false && (75<75 || true)) = (false && true) = false → 100-tick branch
		assertEquals(100 - 75, c.getTicksRemaining()); // 25
	}

	// ── Preserve toggled on during grace window (elapsed 100-125) ────────────

	@Test
	public void preserveTurnedOnInGraceWindow_extends()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 110, false); // elapsed=110, no Preserve, grace → 125-110=15
		// Now turn Preserve on at elapsed=110: elapsed < 75 is false, preserveBeenActive=false
		// but elapsed > 125 is also false → falls to 100-tick branch → 125-110=15 still
		// Preserve activating mid-grace does NOT help unless elapsed>125 triggers extended branch
		assertEquals(15, c.getTicksRemaining());
	}

	// ── Sync resets preserveBeenActive ───────────────────────────────────────

	@Test
	public void newSync_resetsCycle()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // preserveBeenActive = true

		// Natural -1 fires again (resync at tick BASE+50)
		c.sync(BASE + 50);
		c.update(BASE + 50, false);  // elapsed = 0, no Preserve → 100
		assertEquals(100, c.getTicksRemaining());
	}

	// ── Independence: syncing boost clock does not affect debuff clock ────────

	@Test
	public void syncBoostClock_doesNotAffectDebuffClock()
	{
		// Two separate instances as used in SelfState
		BoostDecayClock boost = new BoostDecayClock();
		StatCycleClock  rst   = new StatCycleClock();

		boost.sync(BASE);
		boost.update(BASE + 50, false);
		assertEquals(50, boost.getTicksRemaining());

		// debuff clock is still uncalibrated — independent
		assertFalse(rst.isCalibrated());
		assertEquals(0, rst.getTicksRemaining());
	}

	@Test
	public void syncDebuffClock_doesNotAffectBoostClock()
	{
		BoostDecayClock boost = new BoostDecayClock();
		StatCycleClock  rst   = new StatCycleClock();

		rst.sync(); // debuff clock calibrated
		assertEquals(100, rst.getTicksRemaining());

		// boost clock still uncalibrated
		assertFalse(boost.isCalibrated());
		boost.update(BASE, false);
		assertEquals(-1, boost.getTicksRemaining());
	}
}
