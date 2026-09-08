package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class PeriodicEffectTest
{
	@Test
	public void initialState_inactive()
	{
		PeriodicEffect e = new PeriodicEffect();
		assertFalse(e.isActive());
		assertEquals(0, e.getTotalTicksRemaining());
		assertEquals(0, e.getNextProcTicks());
	}

	@Test
	public void setTotal_becomesActive()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.setTotalTicksRemaining(500);
		assertTrue(e.isActive());
		assertEquals(500, e.getTotalTicksRemaining());
	}

	@Test
	public void tick_decrementsTotal()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.setTotalTicksRemaining(10);
		e.tick();
		assertEquals(9, e.getTotalTicksRemaining());
	}

	@Test
	public void tick_decrementsNextProc()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.setTotalTicksRemaining(50);
		e.setNextProcTicks(25);
		e.tick();
		assertEquals(24, e.getNextProcTicks());
	}

	@Test
	public void tick_neitherGoesNegative()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.tick(); // all zeros
		assertEquals(0, e.getTotalTicksRemaining());
		assertEquals(0, e.getNextProcTicks());
	}

	@Test
	public void setNegative_clampedToZero()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.setTotalTicksRemaining(-5);
		e.setNextProcTicks(-1);
		assertEquals(0, e.getTotalTicksRemaining());
		assertEquals(0, e.getNextProcTicks());
	}

	@Test
	public void reset_clearsAll()
	{
		PeriodicEffect e = new PeriodicEffect();
		e.setTotalTicksRemaining(300);
		e.setNextProcTicks(25);
		e.reset();
		assertFalse(e.isActive());
		assertEquals(0, e.getTotalTicksRemaining());
		assertEquals(0, e.getNextProcTicks());
	}
}
