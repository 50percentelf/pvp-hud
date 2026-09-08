package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.pvphud.PvpHudPlugin.isNaturalBoostDecay;
import static com.pvphud.PvpHudPlugin.isNaturalDebuffRestore;

/**
 * Tests for the two static detection helpers in PvpHudPlugin that distinguish
 * natural stat-cycle ticks from potions, combat drains, and brews.
 *
 * Scenario legend:
 *   prev    = boosted level before this StatChanged event
 *   current = boosted level after (event.getBoostedLevel())
 *   base    = real level (event.getLevel())
 */
public class StatChangeDetectionTest
{
	// ── isNaturalBoostDecay ───────────────────────────────────────────────────

	@Test
	public void boostDecay_naturalMinusOne_detectsDecay()
	{
		// Boosted from 104 → 103, still above base 99.
		assertTrue(isNaturalBoostDecay(104, 103, 99));
	}

	@Test
	public void boostDecay_lastStep_toBase_detectsDecay()
	{
		// Boosted from 100 → 99 (exactly to base). current >= base, so still counts.
		assertTrue(isNaturalBoostDecay(100, 99, 99));
	}

	@Test
	public void boostDecay_potionBoost_ignored()
	{
		// Drinking a pot: 99 → 104 (large positive jump).
		assertFalse(isNaturalBoostDecay(99, 104, 99));
	}

	@Test
	public void boostDecay_largeDrop_combatDrain_ignored()
	{
		// Opponent uses stat-draining attack: 104 → 80 (drop > 1).
		assertFalse(isNaturalBoostDecay(104, 80, 99));
	}

	@Test
	public void boostDecay_dropsBelow_base_ignored()
	{
		// current (98) < base (99): combat drain into debuff territory, not boost decay.
		assertFalse(isNaturalBoostDecay(99, 98, 99));
	}

	@Test
	public void boostDecay_dropBelowBase_ignored()
	{
		// Going into debuff territory: 99 → 97 (two steps, combat drain).
		assertFalse(isNaturalBoostDecay(99, 97, 99));
	}

	// ── isNaturalDebuffRestore ────────────────────────────────────────────────

	@Test
	public void debuffRestore_naturalPlusOne_detectsRestore()
	{
		// Debuffed from 90 → 91, still below base 99.
		assertTrue(isNaturalDebuffRestore(90, 91, 99));
	}

	@Test
	public void debuffRestore_lastStep_toBase_detectsRestore()
	{
		// 98 → 99 (returning to base). current <= base, so still counts.
		assertTrue(isNaturalDebuffRestore(98, 99, 99));
	}

	@Test
	public void debuffRestore_crossesBase_ignored()
	{
		// current (100) > base (99): stat went above base (potion or regen), not a restore tick.
		assertFalse(isNaturalDebuffRestore(99, 100, 99));
	}

	@Test
	public void debuffRestore_brewLargeRestore_ignored()
	{
		// Saradomin brew restores multiple levels at once: 80 → 95 (large jump).
		assertFalse(isNaturalDebuffRestore(80, 95, 99));
	}

	@Test
	public void debuffRestore_statDrainBelow_ignored()
	{
		// Drain makes it worse: 90 → 85 (not a restore at all).
		assertFalse(isNaturalDebuffRestore(90, 85, 99));
	}

	// ── Divine potion interactions ────────────────────────────────────────────

	@Test
	public void divinePotion_wearOff_doesNotSyncBoostDecay()
	{
		// Divine super combat wears off: 118 → 99 (base=99, large multi-level drop).
		assertFalse(isNaturalBoostDecay(118, 99, 99));
	}

	@Test
	public void divinePotion_drink_doesNotSyncBoostDecay()
	{
		// Drinking divine super combat: 99 → 118 (large positive jump).
		assertFalse(isNaturalBoostDecay(99, 118, 99));
	}

	// ── Clock independence ────────────────────────────────────────────────────

	@Test
	public void boostDecay_doesNotTriggerDebuffRestore()
	{
		// A natural -1 above base must only fire the boost-decay clock.
		assertTrue(isNaturalBoostDecay(104, 103, 99));
		assertFalse(isNaturalDebuffRestore(104, 103, 99));
	}

	@Test
	public void debuffRestore_doesNotTriggerBoostDecay()
	{
		// A natural +1 below base must only fire the debuff-restore clock.
		assertTrue(isNaturalDebuffRestore(90, 91, 99));
		assertFalse(isNaturalBoostDecay(90, 91, 99));
	}
}
