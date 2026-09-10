package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class PoisonStateTest
{
	// Convenience: phase-only call with nextPoisonTick = currentTick + 30 (phase = 30).
	private static PoisonState stateWithPhase(int varpValue, int currentTick)
	{
		PoisonState ps = new PoisonState();
		if (varpValue < PoisonState.VENOM_VALUE_CUTOFF)
			ps.setAntiVenomActive(true);
		else
			ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(varpValue);
		ps.setNextPoisonTick(currentTick + PoisonState.POISON_TICK_LENGTH); // phase = 30
		return ps;
	}

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

	// ── Anti-poison: -38 <= value < 0 ────────────────────────────────────────
	// Formula: phase + Math.abs((value + 1) * 30)

	@Test
	public void antiPoison_minusOne_phaseOnly()
	{
		// value=-1: Math.abs((-1+1)*30) = 0 → only current phase
		PoisonState ps = stateWithPhase(-1, 100);
		assertEquals(30, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void antiPoison_minusTwo_phaseAndOneInterval()
	{
		// value=-2: Math.abs((-2+1)*30) = 30 → phase + 30
		PoisonState ps = stateWithPhase(-2, 100);
		assertEquals(60, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void antiPoison_minusThirtyEight_maxAntiPoison()
	{
		// value=-38: Math.abs((-38+1)*30) = 37*30=1110 → phase + 1110
		PoisonState ps = stateWithPhase(-38, 100);
		assertEquals(30 + 37 * 30, ps.getImmunityTicksRemaining(100));
	}

	// ── Anti-venom: value < -38 ───────────────────────────────────────────────
	// Formula: phase + Math.abs((value + 1 - VENOM_VALUE_CUTOFF) * 30)

	@Test
	public void antiVenom_minusThirtyNine_phaseOnly()
	{
		// value=-39: (-39+1-(-38))*30 = 0 → only current phase (NOT 1170 ticks)
		PoisonState ps = stateWithPhase(-39, 100);
		assertEquals(30, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void antiVenom_minusForty_phaseAndOneInterval()
	{
		// value=-40: (-40+1-(-38))*30 = -1*30=-30 → Math.abs(-30)=30 → phase+30
		PoisonState ps = stateWithPhase(-40, 100);
		assertEquals(60, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void antiVenom_minusFortyOne_phaseAndTwoIntervals()
	{
		// value=-41: (-41+1-(-38))*30 = -2*30=-60 → 60 → phase+60
		PoisonState ps = stateWithPhase(-41, 100);
		assertEquals(90, ps.getImmunityTicksRemaining(100));
	}

	// ── Boundary: -38 is anti-poison, -39 is anti-venom ─────────────────────

	@Test
	public void boundary_minusThirtyEight_isAntiPoison()
	{
		assertEquals(PoisonState.VENOM_VALUE_CUTOFF, -38);
		// -38 is NOT < VENOM_VALUE_CUTOFF, so anti-poison formula applies
		PoisonState ps = stateWithPhase(-38, 100);
		// anti-poison: 30 + Math.abs((-38+1)*30) = 30 + 37*30 = 1140
		assertEquals(1140, ps.getImmunityTicksRemaining(100));
	}

	@Test
	public void boundary_minusThirtyNine_isAntiVenom()
	{
		// -39 < VENOM_VALUE_CUTOFF (-38), so anti-venom formula applies
		PoisonState ps = stateWithPhase(-39, 100);
		// anti-venom: 30 + Math.abs((-39+1-(-38))*30) = 30 + 0 = 30
		assertEquals(30, ps.getImmunityTicksRemaining(100));
	}

	// ── Phase mid-cycle ───────────────────────────────────────────────────────

	@Test
	public void immunityTicks_midCycle_usesRealPhase()
	{
		// nextPoisonTick=130, currentTick=115 → phase=15
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-3);
		ps.setNextPoisonTick(130);
		// remaining = 15 + Math.abs((-3+1)*30) = 15 + 60 = 75
		assertEquals(75, ps.getImmunityTicksRemaining(115));
	}

	// ── Phase preservation: varp update while tick still in future ────────────

	@Test
	public void phasePreservation_futureTickNotReset()
	{
		// Simulates applyPoisonVarp not resetting when nextPoisonTick is still valid.
		// At currentTick=100, nextPoisonTick=120 (still 20 ticks away → don't reset).
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-5);
		ps.setNextPoisonTick(120); // phase=20 at tick 100

		// Simulate varp fire at tick 100: since 120-100=20 > 0, preserve nextPoisonTick.
		// (The PvpHudPlugin check: if (ps.getNextPoisonTick() - now <= 0) → false → skip reset)
		// So nextPoisonTick stays 120, phase=20:
		// remaining = 20 + Math.abs((-5+1)*30) = 20 + 120 = 140
		assertEquals(140, ps.getImmunityTicksRemaining(100));
		assertEquals(120, ps.getNextPoisonTick()); // untouched
	}

	// ── Not tracking / zero returns zero ─────────────────────────────────────

	@Test
	public void notTracking_noNextTick_returnsZero()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		// nextPoisonTick stays -1
		assertEquals(0, ps.getImmunityTicksRemaining(50));
	}

	@Test
	public void positiveVarpValue_returnsZero()
	{
		PoisonState ps = new PoisonState();
		ps.setPoisonVarpValue(5);
		ps.setNextPoisonTick(100);
		assertEquals(0, ps.getImmunityTicksRemaining(90));
	}

	@Test
	public void clampedToZero_whenExpired()
	{
		PoisonState ps = new PoisonState();
		ps.setAntiPoisonActive(true);
		ps.setPoisonVarpValue(-1);
		ps.setNextPoisonTick(100);
		assertEquals(0, ps.getImmunityTicksRemaining(200));
	}
}
