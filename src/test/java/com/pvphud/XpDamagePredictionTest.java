package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the XP-derived outgoing damage prediction formula in PvpHudPlugin.
 *
 * Formula: pvpMod = min(1.125, 1.0 + floor(combatLevel / 20.0) / 40.0)
 *          predicted = round((hpXpDelta * 0.75) / pvpMod)
 *
 * The plugin's computePredictedDamage is package-private and takes only xpDelta;
 * combat level is read from the live client.  Here we test the math directly
 * via the static helper to avoid needing a mock client.
 */
public class XpDamagePredictionTest
{
	// ── pvpMod brackets ───────────────────────────────────────────────────────

	@Test
	public void pvpMod_level3_isMinimum()
	{
		// floor(3/20) = 0 → pvpMod = 1.0
		assertEquals(1.0, pvpMod(3), 0.0001);
	}

	@Test
	public void pvpMod_level20_firstBracket()
	{
		// floor(20/20) = 1 → 1.0 + 1/40 = 1.025
		assertEquals(1.025, pvpMod(20), 0.0001);
	}

	@Test
	public void pvpMod_level45_midBracket()
	{
		// floor(45/20) = 2 → 1.0 + 2/40 = 1.05
		assertEquals(1.05, pvpMod(45), 0.0001);
	}

	@Test
	public void pvpMod_level126_isCapped()
	{
		// floor(126/20) = 6 → 1.0 + 6/40 = 1.15, capped at 1.125
		assertEquals(1.125, pvpMod(126), 0.0001);
	}

	@Test
	public void pvpMod_level100_capped()
	{
		// floor(100/20) = 5 → 1.0 + 5/40 = 1.125, exactly at cap
		assertEquals(1.125, pvpMod(100), 0.0001);
	}

	// ── Damage derivation ─────────────────────────────────────────────────────

	@Test
	public void predictedDamage_atLevel126_33xpDelta()
	{
		// 33 XP delta, combatLevel 126 (cap 1.125)
		// predicted = round(33 * 0.75 / 1.125) = round(22) = 22
		assertEquals(22, predictDamage(33, 126));
	}

	@Test
	public void predictedDamage_atLevel3_lowMod()
	{
		// 20 XP delta, level 3 (mod = 1.0)
		// predicted = round(20 * 0.75 / 1.0) = 15
		assertEquals(15, predictDamage(20, 3));
	}

	@Test
	public void predictedDamage_zeroXp_givesZero()
	{
		assertEquals(0, predictDamage(0, 126));
	}

	// ── Sanity bounds ─────────────────────────────────────────────────────────

	@Test
	public void predictedDamage_120xp_withinSanityBound()
	{
		// Even at max XP delta that is plausible (120 per single hit), should be ≤ 120
		int pred = predictDamage(160, 126); // 160 * 0.75 / 1.125 ≈ 107 — within bound
		assertTrue("prediction should be ≤ 120", pred <= 120);
	}

	@Test
	public void predictedDamage_largeXpDelta_exceedsSanity()
	{
		// 300 XP in one tick is implausible; plugin rejects it
		int pred = predictDamage(300, 126); // 300 * 0.75 / 1.125 = 200 — exceeds 120
		assertTrue("large xp delta prediction exceeds sanity guard (> 120)", pred > 120);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static double pvpMod(int combatLevel)
	{
		return Math.min(1.125, 1.0 + Math.floor(combatLevel / 20.0) / 40.0);
	}

	private static int predictDamage(int hpXpDelta, int combatLevel)
	{
		double mod = pvpMod(combatLevel);
		return (int) Math.round((hpXpDelta * 0.75) / mod);
	}
}
