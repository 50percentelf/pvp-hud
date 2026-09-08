package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates BoostDecayClock against the RuneLite BoostsPlugin#getChangeDownTicks() contract.
 *
 * All tests use lastTick = BASE (1000) so elapsed = currentTick - BASE.
 * Each test either calls update() once (when preserveBeenActive is irrelevant at
 * that elapsed value) or calls it sequentially to build up the correct preserved state.
 *
 * The underlying formula (verbatim from RuneLite):
 *
 *   if ((preserve && (elapsed < 75 || preserveBeenActive)) || elapsed > 125)
 *   {
 *       preserveBeenActive = true;
 *       return 150 - elapsed;
 *   }
 *   preserveBeenActive = false;
 *   return elapsed > 100 ? 125 - elapsed : 100 - elapsed;
 *
 * Possible outcomes:
 *   100-tick — Preserve off or activated too late (standard)
 *   125-tick — grace window (100 < elapsed ≤ 125); Preserve can still activate
 *   150-tick — Preserve was on before tick 75 and held (or elapsed > 125)
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

	// ── No Preserve — 100-tick cycle ─────────────────────────────────────────

	@Test
	public void noPreserve_elapsed0_returns100()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE, false);
		assertEquals(100, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_elapsed50_returns50()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, false);
		assertEquals(50, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_elapsed74_returns26()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 74, false);
		assertEquals(26, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_elapsed100_returns0()
	{
		// Standard 100-tick expiry.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 100, false);
		assertEquals(0, c.getTicksRemaining());
	}

	// ── No Preserve — grace window (elapsed 101–125) ─────────────────────────

	@Test
	public void noPreserve_elapsed101_returns24_gracePeriod()
	{
		// Stat hasn't decayed yet; grace window: return 125 - elapsed.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 101, false);  // 125 - 101 = 24
		assertEquals(24, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_elapsed125_returns0_graceExpiry()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 125, false);  // 125 - 125 = 0
		assertEquals(0, c.getTicksRemaining());
	}

	@Test
	public void noPreserve_elapsed126_returns24_extends150Branch()
	{
		// elapsed > 125 triggers the 150-branch (RuneLite formula).
		// Represents an anomalously delayed decay; the formula handles it gracefully.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 126, false);  // elapsed > 125 → 150 - 126 = 24
		assertEquals(24, c.getTicksRemaining());
	}

	// ── Preserve continuously active — 150-tick cycle ─────────────────────────

	@Test
	public void preserveAlwaysOn_elapsed0_returns150()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE, true);
		assertEquals(150, c.getTicksRemaining());
	}

	@Test
	public void preserveAlwaysOn_elapsed74_returns76()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 74, true);  // preserveBeenActive=true, 150-74=76
		assertEquals(76, c.getTicksRemaining());
	}

	@Test
	public void preserveAlwaysOn_elapsed75_preserveBeenActiveCarries()
	{
		// preserveBeenActive was set at elapsed=74; at elapsed=75 the elapsed<75
		// guard is false but preserveBeenActive=true keeps us in the 150-branch.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 74, true);   // preserveBeenActive → true
		c.update(BASE + 75, true);   // (true && (false || true)) → 150-75=75
		assertEquals(75, c.getTicksRemaining());
	}

	@Test
	public void preserveAlwaysOn_elapsed150_returns0()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // preserveBeenActive → true
		c.update(BASE + 150, true);  // elapsed=150 → 150-150=0
		assertEquals(0, c.getTicksRemaining());
	}

	// ── Preserve toggled off — various points ─────────────────────────────────

	@Test
	public void preserve_droppedAt60_revertsto100Cycle()
	{
		// Preserve on at elapsed=50 sets preserveBeenActive=true.
		// Dropped at elapsed=60 (before 75): else-branch taken, preserveBeenActive cleared.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // preserveBeenActive → true
		c.update(BASE + 60, false);  // (false && (60<75 || true)) = false → 100-60=40
		assertEquals(40, c.getTicksRemaining());
	}

	@Test
	public void preserve_droppedAt75_revertsto100Cycle()
	{
		// At elapsed=75: elapsed<75 is false, preserve is OFF → else-branch.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 74, true);   // preserveBeenActive → true
		c.update(BASE + 75, false);  // (false && (false || true)) = false → 100-75=25
		assertEquals(25, c.getTicksRemaining());
	}

	@Test
	public void preserve_droppedAt99_returns1()
	{
		// Dropped one tick before standard expiry: returns 1, not the full 150.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // preserveBeenActive → true
		c.update(BASE + 99, false);  // (false && (false || true)) = false → 100-99=1
		assertEquals(1, c.getTicksRemaining());
	}

	@Test
	public void preserve_droppedAt100_returns0()
	{
		// Preserve dropped at exactly elapsed=100: 100-100=0 (standard expiry).
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);
		c.update(BASE + 100, false); // (false && (false || true)) = false → 100>100? 125-100:100-100 = 0
		assertEquals(0, c.getTicksRemaining());
	}

	// ── Preserve activated too late (after tick 75) — no first extension ─────

	@Test
	public void preserve_activatedAt100_noExtension()
	{
		// preserveBeenActive is still false (never had Preserve on before tick 75).
		// At elapsed=100, Preserve ON: (true && (100<75 || false)) = false → 100-100=0.
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, false);   // preserveBeenActive stays false
		c.update(BASE + 100, true);   // elapsed=100, Preserve ON but too late
		assertEquals(0, c.getTicksRemaining());
	}

	@Test
	public void preserve_activatedAt110_gracePeriodOnly()
	{
		// Activating Preserve during the grace window (101-125) does not extend
		// to 150 ticks — elapsed<75 is false, preserveBeenActive is false.
		// Returns 125 - elapsed (grace-window formula).
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, false);   // preserveBeenActive stays false
		c.update(BASE + 110, true);   // (true && (false || false)) || (110>125) = false → 125-110=15
		assertEquals(15, c.getTicksRemaining());
	}

	// ── Re-sync resets the cycle ──────────────────────────────────────────────

	@Test
	public void newSync_resetsCycleAndPreserveState()
	{
		BoostDecayClock c = new BoostDecayClock();
		c.sync(BASE);
		c.update(BASE + 50, true);   // preserveBeenActive → true

		// Natural -1 fires again at BASE+50 → new sync point
		c.sync(BASE + 50);
		c.update(BASE + 50, false);  // elapsed=0, no Preserve → 100
		assertEquals(100, c.getTicksRemaining());
	}

	// ── Independence from debuff-restore clock ────────────────────────────────

	@Test
	public void syncBoostClock_doesNotAffectDebuffClock()
	{
		BoostDecayClock boost = new BoostDecayClock();
		StatCycleClock  rst   = new StatCycleClock();

		boost.sync(BASE);
		boost.update(BASE + 50, false);
		assertEquals(50, boost.getTicksRemaining());

		// debuff clock is still uncalibrated
		assertFalse(rst.isCalibrated());
		assertEquals(0, rst.getTicksRemaining());
	}

	@Test
	public void syncDebuffClock_doesNotAffectBoostClock()
	{
		BoostDecayClock boost = new BoostDecayClock();
		StatCycleClock  rst   = new StatCycleClock();

		rst.sync();
		assertEquals(100, rst.getTicksRemaining());

		// boost clock still uncalibrated
		assertFalse(boost.isCalibrated());
		boost.update(BASE, false);
		assertEquals(-1, boost.getTicksRemaining());
	}
}
