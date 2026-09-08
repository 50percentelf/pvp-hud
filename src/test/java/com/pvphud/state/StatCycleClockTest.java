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
	public void firstSync_withPreservePeriod()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(150);
		assertTrue(c.isCalibrated());
		assertEquals(150, c.getPeriod());
		assertEquals(150, c.getTicksRemaining());
	}

	@Test
	public void tick_decrementsRemaining()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 100, remaining = 100
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
		c.sync(); // elapsed = 65; in range [10,200] → period = 65
		assertEquals(65, c.getPeriod());
		assertEquals(65, c.getTicksRemaining());
	}

	@Test
	public void subsequentSync_ignoresElapsedBelowMinimum()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 100, remaining = 100
		c.tick(); c.tick(); // 98 remaining → elapsed = 2 (< 10, too small)
		c.sync(); // elapsed 2 < 10 → period unchanged at 100
		assertEquals(100, c.getPeriod());
	}

	@Test
	public void subsequentSync_ignoresElapsedAboveMaximum()
	{
		// Use a very large defaultPeriod to exercise the > 200 guard.
		StatCycleClock c = new StatCycleClock();
		c.sync(250); // period = 250, remaining = 250
		for (int i = 0; i < 250; i++) c.tick(); // drained to 0 → elapsed = 250 > 200
		c.sync(); // elapsed 250 > 200 → period unchanged at 250
		assertEquals(250, c.getPeriod());
		assertEquals(250, c.getTicksRemaining());
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
