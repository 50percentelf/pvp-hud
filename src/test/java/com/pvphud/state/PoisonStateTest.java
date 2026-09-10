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
		assertEquals(0, ps.getImmunityTicksRemaining(0));
	}

	@Test
	public void reset_clearsAll()
	{
		PoisonState ps = new PoisonState();
		ps.setPoisoned(true);
		ps.setVenomed(true);
		ps.setAntiPoisonActive(true);
		ps.setAntiVenomActive(true);
		ps.setPoisonVarpValue(-5);
		ps.setNextPoisonTick(100);
		ps.reset();
		assertFalse(ps.isPoisoned());
		assertFalse(ps.isVenomed());
		assertFalse(ps.isAntiPoisonActive());
		assertFalse(ps.isAntiVenomActive());
		assertEquals(0, ps.getImmunityTicksRemaining(50));
	}

	// ── Phase-aware immunity duration ─────────────────────────────────────────

	@Test
	public void immunityTicks_atVarpChangeInstant()
	{
		// At the moment varp fires (nextPoisonTick just set to currentTick + 30):
		// remaining = (|value| - 1) * 30 + 30 = |value| * 30
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-5);
		ps.setNextPoisonTick(130); // fired at tick 100
		assertEquals(5 * 30, ps.getImmunityTicksRemaining(100)); // = 150
	}

	@Test
	public void immunityTicks_midCycle()
	{
		// Half-way through a 30-tick cycle:
		// remaining = (|value| - 1) * 30 + (nextPoisonTick - currentTick)
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-3);
		ps.setNextPoisonTick(130); // next decrement at tick 130
		// At tick 115 (15 ticks into the cycle):
		// remaining = (3-1)*30 + (130-115) = 60 + 15 = 75
		assertEquals(75, ps.getImmunityTicksRemaining(115));
	}

	@Test
	public void immunityTicks_lastDose()
	{
		// value = -1: one interval left
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-1);
		ps.setNextPoisonTick(130);
		// remaining = (1-1)*30 + (130-100) = 0 + 30 = 30
		assertEquals(30, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void immunityTicks_clampedToZero()
	{
		// If currentTick is past nextPoisonTick and value is 1, clamp to 0
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-1);
		ps.setNextPoisonTick(100);
		assertEquals(0, ps.getImmunityTicksRemaining(200));
	}

	// ── Anti-venom threshold: strictly < -38, not <= -38 ─────────────────────

	@Test
	public void antiVenomThreshold_minusThirtyNine_isAntiVenom()
	{
		// value -39 < VENOM_VALUE_CUTOFF (-38): anti-venom
		// (detection logic lives in PvpHudPlugin; this tests the formula still works)
		PoisonState ps = new PoisonState();
		ps.setAntiVenomActive(true);
		ps.setPoisonVarpValue(-39);
		ps.setNextPoisonTick(130);
		// (39-1)*30 + 30 = 38*30 + 30 = 1170
		assertEquals(1170, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void antiPoisonAt_minusThirtyEight_usesCorrectFormula()
	{
		// value -38 is anti-poison (not anti-venom per RuneLite threshold)
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-38);
		ps.setNextPoisonTick(130);
		// (38-1)*30 + 30 = 37*30 + 30 = 1140
		assertEquals(1140, ps.getImmunityTicksRemaining(100));
	}

	// ── Not tracking returns zero ─────────────────────────────────────────────

	@Test
	public void notTracking_returnsZero()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		// nextPoisonTick not set (-1 = default)
		assertEquals(0, ps.getImmunityTicksRemaining(50));
	}

	@Test
	public void positiveVarpValue_returnsZero()
	{
		PoisonState ps = new PoisonState();
		ps.setPoisonVarpValue(5); // positive = poisoned, not immune
		ps.setNextPoisonTick(100);
		assertEquals(0, ps.getImmunityTicksRemaining(90));
	}
}
