package com.pvphud;

import com.pvphud.state.EffectState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates antifire timer state mechanics in EffectState.
 *
 * Two independent varbit families:
 *   Normal  (Varbits.ANTIFIRE      = 3981): 30 ticks/phase
 *     Antifire:          varbit 20  → 600  total ticks
 *     Extended Antifire: varbit 40  → 1200 total ticks
 *   Super   (Varbits.SUPER_ANTIFIRE = 6101): 20 ticks/phase
 *     Super Antifire:          varbit 15 → 300  total ticks
 *     Extended Super Antifire: varbit 30 → 600  total ticks
 *
 * Formula (mirrors RuneLite TimersAndBuffsPlugin):
 *   remaining = nextXxxTick - currentTick + (varbitValue - 1) * phaseInterval
 *
 * nextXxxTick is set to currentTick + interval only when the phase boundary
 * has passed (nextXxxTick - currentTick <= 0) or on plugin startup mid-effect.
 *
 * These tests operate directly on EffectState; no Client or Plugin is needed.
 * The plugin-side handler logic (when to update nextXxxTick) is simulated inline
 * so that the state computation can be verified deterministically.
 */
public class AntifireTimerTest
{
	// ── Helpers ───────────────────────────────────────────────────────────────

	/**
	 * Simulate plugin's onVarbitChanged handler for the normal antifire family.
	 * Only updates nextAntifireTick when the phase boundary has passed.
	 */
	private static void applyAntifireVarbit(EffectState efx, int value, int currentTick)
	{
		if (value == 0)
		{
			efx.setNextAntifireTick(-1);
		}
		else if (efx.getNextAntifireTick() - currentTick <= 0)
		{
			efx.setNextAntifireTick(currentTick + 30);
		}
		efx.setAntifireVarbitValue(value);
	}

	/**
	 * Simulate plugin's onVarbitChanged handler for the super antifire family.
	 */
	private static void applySuperAntifireVarbit(EffectState efx, int value, int currentTick)
	{
		if (value == 0)
		{
			efx.setNextSuperAntifireTick(-1);
		}
		else if (efx.getNextSuperAntifireTick() - currentTick <= 0)
		{
			efx.setNextSuperAntifireTick(currentTick + 20);
		}
		efx.setSuperAntifireVarbitValue(value);
	}

	// ── Normal family: activation ─────────────────────────────────────────────

	@Test
	public void normalAntifire_activates_600ticks()
	{
		// Fresh Antifire dose: varbit 0 → 20, phase starts now
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000);

		// 30 + (20-1)*30 = 30 + 570 = 600
		assertEquals(600, efx.getAntifireTicks(1000));
	}

	@Test
	public void extendedAntifire_activates_1200ticks()
	{
		// Extended Antifire: varbit 0 → 40 → 1200 ticks through same family
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 40, 1000);

		// 30 + (40-1)*30 = 30 + 1170 = 1200
		assertEquals(1200, efx.getAntifireTicks(1000));
	}

	// ── Normal family: per-tick countdown within a phase ─────────────────────

	@Test
	public void normalAntifire_midPhase_countsDown()
	{
		// 15 ticks into a phase — nextAntifireTick is 15 ticks ahead
		EffectState efx = new EffectState();
		efx.setNextAntifireTick(1030);
		efx.setAntifireVarbitValue(20);

		// At tick 1015: 1030 - 1015 + 19*30 = 15 + 570 = 585
		assertEquals(585, efx.getAntifireTicks(1015));
	}

	@Test
	public void normalAntifire_phaseEnd_countsDown()
	{
		// One tick before the phase boundary
		EffectState efx = new EffectState();
		efx.setNextAntifireTick(1030);
		efx.setAntifireVarbitValue(20);

		// At tick 1029: 1030 - 1029 + 19*30 = 1 + 570 = 571
		assertEquals(571, efx.getAntifireTicks(1029));
	}

	// ── Normal family: phase boundary / varbit decrement ─────────────────────

	@Test
	public void normalAntifire_varbitDecrement_preservesCountdown()
	{
		// At tick 1030 the phase boundary fires: varbit decrements 20 → 19.
		// nextAntifireTick - currentTick = 1030 - 1030 = 0 → boundary passed → reset.
		EffectState efx = new EffectState();
		efx.setNextAntifireTick(1030);
		efx.setAntifireVarbitValue(20);

		// Simulate varbit decrement at the boundary tick
		applyAntifireVarbit(efx, 19, 1030);

		// nextAntifireTick is now 1030 + 30 = 1060
		// remaining = 1060 - 1030 + 18*30 = 30 + 540 = 570
		assertEquals(570, efx.getAntifireTicks(1030));
	}

	@Test
	public void normalAntifire_varbitDecrement_continuityAroundBoundary()
	{
		// Tick 1029 (before phase update): 1030 - 1029 + 19*30 = 571
		// Tick 1030 (after phase update):  30 + 18*30 = 570
		// One-tick drop at the boundary is expected and correct.
		EffectState efx = new EffectState();
		efx.setNextAntifireTick(1030);
		efx.setAntifireVarbitValue(20);

		int beforeBoundary = efx.getAntifireTicks(1029);

		applyAntifireVarbit(efx, 19, 1030);
		int afterBoundary = efx.getAntifireTicks(1030);

		assertEquals(571, beforeBoundary);
		assertEquals(570, afterBoundary);
		assertEquals(1, beforeBoundary - afterBoundary);
	}

	// ── Normal family: redose mid-phase ──────────────────────────────────────

	@Test
	public void normalAntifire_redoseMidPhase_updatesTotal()
	{
		// Player redoses with 15 ticks remaining in the current phase (nextAntifireTick not yet passed).
		// Handler condition: nextAntifireTick - tick = 1015 - 1000 = 15 > 0 → do NOT reset phase pointer.
		// Only the varbit value is updated.
		EffectState efx = new EffectState();
		efx.setNextAntifireTick(1015);
		efx.setAntifireVarbitValue(5); // nearly expired

		int beforeRedose = efx.getAntifireTicks(1000); // 15 + 4*30 = 135

		applyAntifireVarbit(efx, 20, 1000); // redose bumps varbit to 20

		int afterRedose = efx.getAntifireTicks(1000); // 15 + 19*30 = 585

		assertEquals(135, beforeRedose);
		assertEquals(585, afterRedose);
	}

	// ── Normal family: expiry ─────────────────────────────────────────────────

	@Test
	public void normalAntifire_zero_removesTimer()
	{
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000); // activate
		assertEquals(600, efx.getAntifireTicks(1000));

		applyAntifireVarbit(efx, 0, 1030); // effect expires
		assertEquals(0, efx.getAntifireTicks(1030));
	}

	@Test
	public void normalAntifire_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getAntifireTicks(5000));
	}

	// ── Super family: activation ──────────────────────────────────────────────

	@Test
	public void superAntifire_activates_300ticks()
	{
		// Super Antifire: varbit 15, 20 ticks/phase → 20 + 14*20 = 300
		EffectState efx = new EffectState();
		applySuperAntifireVarbit(efx, 15, 2000);

		assertEquals(300, efx.getSuperAntifireTicks(2000));
	}

	@Test
	public void extendedSuperAntifire_activates_600ticks()
	{
		// Extended Super Antifire: varbit 30 → 20 + 29*20 = 600
		EffectState efx = new EffectState();
		applySuperAntifireVarbit(efx, 30, 2000);

		assertEquals(600, efx.getSuperAntifireTicks(2000));
	}

	// ── Super family: per-tick countdown ─────────────────────────────────────

	@Test
	public void superAntifire_midPhase_countsDown()
	{
		// 10 ticks into a 20-tick phase
		EffectState efx = new EffectState();
		efx.setNextSuperAntifireTick(2020);
		efx.setSuperAntifireVarbitValue(15);

		// At tick 2010: 2020 - 2010 + 14*20 = 10 + 280 = 290
		assertEquals(290, efx.getSuperAntifireTicks(2010));
	}

	// ── Super family: phase boundary / varbit decrement ──────────────────────

	@Test
	public void superAntifire_varbitDecrement_preservesCountdown()
	{
		// Phase boundary at tick 2020: varbit 15 → 14
		EffectState efx = new EffectState();
		efx.setNextSuperAntifireTick(2020);
		efx.setSuperAntifireVarbitValue(15);

		applySuperAntifireVarbit(efx, 14, 2020);

		// nextSuperAntifireTick = 2020 + 20 = 2040
		// remaining = 2040 - 2020 + 13*20 = 20 + 260 = 280
		assertEquals(280, efx.getSuperAntifireTicks(2020));
	}

	@Test
	public void superAntifire_varbitDecrement_continuityAroundBoundary()
	{
		EffectState efx = new EffectState();
		efx.setNextSuperAntifireTick(2020);
		efx.setSuperAntifireVarbitValue(15);

		int beforeBoundary = efx.getSuperAntifireTicks(2019); // 1 + 14*20 = 281

		applySuperAntifireVarbit(efx, 14, 2020);
		int afterBoundary = efx.getSuperAntifireTicks(2020); // 20 + 13*20 = 280

		assertEquals(281, beforeBoundary);
		assertEquals(280, afterBoundary);
		assertEquals(1, beforeBoundary - afterBoundary);
	}

	// ── Super family: expiry ──────────────────────────────────────────────────

	@Test
	public void superAntifire_zero_removesTimer()
	{
		EffectState efx = new EffectState();
		applySuperAntifireVarbit(efx, 15, 2000);
		assertEquals(300, efx.getSuperAntifireTicks(2000));

		applySuperAntifireVarbit(efx, 0, 2020);
		assertEquals(0, efx.getSuperAntifireTicks(2020));
	}

	@Test
	public void superAntifire_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getSuperAntifireTicks(5000));
	}

	// ── Lifecycle: reset ──────────────────────────────────────────────────────

	@Test
	public void reset_clearsBothAntifireTimers()
	{
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000);
		applySuperAntifireVarbit(efx, 15, 1000);

		efx.reset();

		assertEquals(0, efx.getAntifireTicks(1000));
		assertEquals(0, efx.getSuperAntifireTicks(1000));
	}

	// ── Lifecycle: login mid-effect rehydration ───────────────────────────────

	@Test
	public void loginMidEffect_seedsConservatively_normalFamily()
	{
		// Simulate initSelfState() when antifire is already active (varbit = 10).
		// Plugin sets nextAntifireTick = currentTick + 30 conservatively.
		EffectState efx = new EffectState();
		int loginTick = 500;
		efx.setNextAntifireTick(loginTick + 30);
		efx.setAntifireVarbitValue(10);

		// remaining = 30 + (10-1)*30 = 30 + 270 = 300 — at most one full phase remaining
		assertEquals(300, efx.getAntifireTicks(loginTick));
		// Active (> 0)
		assertTrue(efx.getAntifireTicks(loginTick) > 0);
	}

	@Test
	public void loginMidEffect_seedsConservatively_superFamily()
	{
		// Simulate initSelfState() for super antifire mid-effect (varbit = 7).
		EffectState efx = new EffectState();
		int loginTick = 500;
		efx.setNextSuperAntifireTick(loginTick + 20);
		efx.setSuperAntifireVarbitValue(7);

		// remaining = 20 + (7-1)*20 = 20 + 120 = 140
		assertEquals(140, efx.getSuperAntifireTicks(loginTick));
	}

	// ── Lifecycle: normal and super cannot contaminate each other ─────────────

	@Test
	public void normalAndSuper_doNotContaminate_normalOnlyActive()
	{
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000);

		assertEquals(600, efx.getAntifireTicks(1000));
		assertEquals(0,   efx.getSuperAntifireTicks(1000));
	}

	@Test
	public void normalAndSuper_doNotContaminate_superOnlyActive()
	{
		EffectState efx = new EffectState();
		applySuperAntifireVarbit(efx, 15, 1000);

		assertEquals(0,   efx.getAntifireTicks(1000));
		assertEquals(300, efx.getSuperAntifireTicks(1000));
	}

	@Test
	public void normalAndSuper_bothActive_independentValues()
	{
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000);
		applySuperAntifireVarbit(efx, 15, 1000);

		assertEquals(600, efx.getAntifireTicks(1000));
		assertEquals(300, efx.getSuperAntifireTicks(1000));
	}

	@Test
	public void normalAndSuper_expiringNormal_doesNotClearSuper()
	{
		EffectState efx = new EffectState();
		applyAntifireVarbit(efx, 20, 1000);
		applySuperAntifireVarbit(efx, 15, 1000);

		applyAntifireVarbit(efx, 0, 1020); // normal expires

		assertEquals(0,   efx.getAntifireTicks(1020));
		assertTrue("super antifire must remain active", efx.getSuperAntifireTicks(1020) > 0);
	}
}
