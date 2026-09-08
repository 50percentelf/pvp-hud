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
		assertEquals(40, c.getPeriod());
		assertEquals(40, c.getTicksRemaining());
	}

	@Test
	public void tick_decrementsRemaining()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 40, remaining = 40
		c.tick();
		assertEquals(39, c.getTicksRemaining());
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
		c.sync(); // period = 40, remaining = 40
		for (int i = 0; i < 35; i++) c.tick(); // 5 remaining → elapsed = 35
		c.sync(); // elapsed = 40 - 5 = 35; in range [10,120] → period = 35
		assertEquals(35, c.getPeriod());
		assertEquals(35, c.getTicksRemaining());
	}

	@Test
	public void subsequentSync_ignoresElapsedBelowMinimum()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 40, remaining = 40
		c.tick(); c.tick(); // 38 remaining → elapsed = 2 (too small)
		c.sync(); // elapsed 2 < 10 → period unchanged at 40
		assertEquals(40, c.getPeriod());
	}

	@Test
	public void subsequentSync_ignoresElapsedAboveMaximum()
	{
		StatCycleClock c = new StatCycleClock();
		c.sync(); // period = 40, remaining = 40
		for (int i = 0; i < 40; i++) c.tick(); // drained to 0 → elapsed > 120
		c.sync(); // elapsed > 120 → period unchanged, ticksRemaining reset to 40
		assertEquals(40, c.getPeriod());
		assertEquals(40, c.getTicksRemaining());
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
