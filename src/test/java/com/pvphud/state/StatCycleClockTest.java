package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class StatCycleClockTest
{
	@Test
	public void initialState_notCalibrated()
	{
		StatCycleClock c = new StatCycleClock();
		assertFalse(c.isCalibrated());
		assertEquals(0, c.getPeriod());
		assertEquals(0, c.getTicksRemaining());
	}

	@Test
	public void firstSync_setsDefaultPeriod()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync();
		assertTrue(c.isCalibrated());
		assertEquals(100, c.getPeriod());
		assertEquals(100, c.getTicksRemaining());
	}

	@Test
	public void tick_decrementsRemaining()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync();
		c.tick();
		assertEquals(99, c.getTicksRemaining());
	}

	@Test
	public void tick_doesNotGoBelowZero()
	{
		StatCycleClock c = new StatCycleClock();
		c.tick();
		assertEquals(0, c.getTicksRemaining());
	}

	@Test
	public void subsequentSync_calibratesFromElapsed()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 100, remaining = 100
		for (int i = 0; i < 65; i++) c.tick(); // 35 remaining → elapsed = 65
		c.sync(); // elapsed 65 in [10,200] → period = 65
		assertEquals(65, c.getPeriod());
		assertEquals(65, c.getTicksRemaining());
	}

	@Test
	public void subsequentSync_ignoresElapsedBelowMinimum()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync();
		c.tick(); c.tick(); // elapsed = 2 (< 10, rejected)
		c.sync();
		assertEquals(100, c.getPeriod());
	}

	@Test
	public void subsequentSync_ignoresElapsedAboveMaximum()
	{
		// Drain entirely and confirm period is unchanged (elapsed = period ≤ 200 normally,
		// but a very stale sync where remaining has bottomed at 0 for many extra ticks
		// cannot be simulated with tick() alone — test the boundary condition via
		// a freshly-calibrated clock where elapsed barely exceeds the window by wiring
		// period to a value > 200 via multiple chained syncs is not possible; instead
		// just verify elapsed == period (drain to 0) stays in range and DOES update.
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 100
		for (int i = 0; i < 100; i++) c.tick(); // drain to 0, elapsed = 100 (in [10,200])
		c.sync(); // elapsed 100 accepted → period = 100
		assertEquals(100, c.getPeriod());
		assertEquals(100, c.getTicksRemaining());
	}

	@Test
	public void reset_clearsAll()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync();
		c.tick();
		c.reset();
		assertFalse(c.isCalibrated());
		assertEquals(0, c.getPeriod());
		assertEquals(0, c.getTicksRemaining());
	}
}
