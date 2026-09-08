package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class PoisonStateTest
{
	@Test
	public void initialState_allClear()
	{
		PoisonState ps = new PoisonState();
		assertFalse(ps.isPoisoned());
		assertFalse(ps.isVenomed());
		assertFalse(ps.isAntiPoisonActive());
		assertFalse(ps.isAntiVenomActive());
		assertEquals(0, ps.getAntiPoisonTicks());
		assertEquals(0, ps.getAntiVenomTicks());
	}

	@Test
	public void tick_decrementsAntiPoisonTicks()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonTicks(10);
		ps.tick();
		assertEquals(9, ps.getAntiPoisonTicks());
	}

	@Test
	public void tick_decrementsAntiVenomTicks()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiVenomTicks(5);
		ps.tick();
		assertEquals(4, ps.getAntiVenomTicks());
	}

	@Test
	public void tick_doesNotGoBelowZero()
	{
		PoisonState ps = new PoisonState();
		ps.tick();
		assertEquals(0, ps.getAntiPoisonTicks());
		assertEquals(0, ps.getAntiVenomTicks());
	}

	@Test
	public void setNegative_clampedToZero()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonTicks(-1);
		ps.setAntiVenomTicks(-100);
		assertEquals(0, ps.getAntiPoisonTicks());
		assertEquals(0, ps.getAntiVenomTicks());
	}

	@Test
	public void reset_clearsAll()
	{
		PoisonState ps = new PoisonState();
		ps.setPoisoned(true);
		ps.setVenomed(true);
		ps.setAntiPoisonActive(true);
		ps.setAntiVenomActive(true);
		ps.setAntiPoisonTicks(100);
		ps.setAntiVenomTicks(200);
		ps.reset();
		assertFalse(ps.isPoisoned());
		assertFalse(ps.isVenomed());
		assertFalse(ps.isAntiPoisonActive());
		assertFalse(ps.isAntiVenomActive());
		assertEquals(0, ps.getAntiPoisonTicks());
		assertEquals(0, ps.getAntiVenomTicks());
	}
}
