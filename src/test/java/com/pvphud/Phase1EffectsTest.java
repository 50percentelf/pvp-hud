package com.pvphud;

import com.pvphud.state.EffectState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Phase 1 status effects added to EffectState:
 *   - Staff of the Dead protection buff (100-tick absolute-tick timer)
 *   - Surge potion cooldown (varbit × 10 = ticks remaining)
 *   - Prayer Regeneration potion (varbit × 12 = total ticks; proc every 12t)
 *   - Ward of Arceuus buff + 50-tick cooldown
 *   - Mark of Darkness buff + 10-tick cooldown
 *   - Corruption drain tracking (iteration-based interval detection)
 *   - Burn damage counters (pass-through varbits)
 */
public class Phase1EffectsTest
{
	// ── Staff of the Dead protection buff ────────────────────────────────────

	@Test
	public void sotd_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getStaffProtTicks(1000));
	}

	@Test
	public void sotd_apply_100ticksRemaining()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		assertEquals(100, efx.getStaffProtTicks(1000));
	}

	@Test
	public void sotd_countsDownWithCurrentTick()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		assertEquals(90, efx.getStaffProtTicks(1010));
		assertEquals(50, efx.getStaffProtTicks(1050));
		assertEquals(1,  efx.getStaffProtTicks(1099));
		assertEquals(0,  efx.getStaffProtTicks(1100));
	}

	@Test
	public void sotd_pastExpiry_returnsZero()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		assertEquals(0, efx.getStaffProtTicks(9999));
	}

	@Test
	public void sotd_clearStaffProt_removesTimer()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		efx.clearStaffProt();
		assertEquals(0, efx.getStaffProtTicks(1005));
	}

	@Test
	public void sotd_reapply_overwritesPreviousTimer()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		assertEquals(50, efx.getStaffProtTicks(1050));
		efx.applyStaffProt(1060); // reapplied at tick 1060
		assertEquals(100, efx.getStaffProtTicks(1060));
		assertEquals(90,  efx.getStaffProtTicks(1070));
	}

	@Test
	public void sotd_reset_clearsTimer()
	{
		EffectState efx = new EffectState();
		efx.applyStaffProt(1000);
		efx.reset();
		assertEquals(0, efx.getStaffProtTicks(1005));
	}

	// ── Surge potion cooldown ─────────────────────────────────────────────────

	@Test
	public void surge_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getSurgePotionTicks(1000));
	}

	@Test
	public void surge_varbit10_equals100ticks()
	{
		EffectState efx = new EffectState();
		efx.updateSurgePotionFromVarbit(10, 1000);
		assertEquals(100, efx.getSurgePotionTicks(1000));
	}

	@Test
	public void surge_varbit1_equals10ticks()
	{
		EffectState efx = new EffectState();
		efx.updateSurgePotionFromVarbit(1, 1000);
		assertEquals(10, efx.getSurgePotionTicks(1000));
	}

	@Test
	public void surge_countsDownWithCurrentTick()
	{
		// varbit 5 at tick 1000 → endTick = 1050
		EffectState efx = new EffectState();
		efx.updateSurgePotionFromVarbit(5, 1000);
		assertEquals(50, efx.getSurgePotionTicks(1000));
		assertEquals(40, efx.getSurgePotionTicks(1010));
		assertEquals(1,  efx.getSurgePotionTicks(1049));
		assertEquals(0,  efx.getSurgePotionTicks(1050));
	}

	@Test
	public void surge_varbitZero_clearsTimer()
	{
		EffectState efx = new EffectState();
		efx.updateSurgePotionFromVarbit(5, 1000);
		efx.updateSurgePotionFromVarbit(0, 1010);
		assertEquals(0, efx.getSurgePotionTicks(1010));
	}

	@Test
	public void surge_reset_clearsTimer()
	{
		EffectState efx = new EffectState();
		efx.updateSurgePotionFromVarbit(5, 1000);
		efx.reset();
		assertEquals(0, efx.getSurgePotionTicks(1005));
	}

	// ── Prayer Regeneration potion ────────────────────────────────────────────

	@Test
	public void prayerRegen_varbitZero_notActive()
	{
		EffectState efx = new EffectState();
		efx.updatePrayerRegen(0);
		assertEquals(0, efx.getPrayerRegen().getTotalTicksRemaining());
	}

	@Test
	public void prayerRegen_varbit5_equals60ticks()
	{
		// Each varbit unit = 12 ticks
		EffectState efx = new EffectState();
		efx.updatePrayerRegen(5);
		assertEquals(60, efx.getPrayerRegen().getTotalTicksRemaining());
	}

	@Test
	public void prayerRegen_varbitDecrement_resetsNextProc()
	{
		// When varbit decrements from >0, nextProcTicks should be reset to 12.
		EffectState efx = new EffectState();
		efx.updatePrayerRegen(5); // first call — prevVarbit was -1, no proc reset
		efx.updatePrayerRegen(4); // decrement: prevVarbit=5 > 0 and 4 < 5 → nextProcTicks=12
		assertEquals(48, efx.getPrayerRegen().getTotalTicksRemaining());
		assertEquals(12, efx.getPrayerRegen().getNextProcTicks());
	}

	@Test
	public void prayerRegen_varbitZero_resetsState()
	{
		EffectState efx = new EffectState();
		efx.updatePrayerRegen(5);
		efx.updatePrayerRegen(0);
		assertEquals(0, efx.getPrayerRegen().getTotalTicksRemaining());
	}

	@Test
	public void prayerRegen_reset_clearsState()
	{
		EffectState efx = new EffectState();
		efx.updatePrayerRegen(5);
		efx.reset();
		assertEquals(0, efx.getPrayerRegen().getTotalTicksRemaining());
	}

	// ── Ward of Arceuus buff + cooldown ──────────────────────────────────────

	@Test
	public void ward_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getWardOfArceuusTicks(1000));
		assertEquals(0, efx.getWardCooldownTicks(1000));
	}

	@Test
	public void ward_applyAtMagicLevel99_equals99ticks()
	{
		EffectState efx = new EffectState();
		efx.applyWardOfArceuus(1000, 99);
		assertEquals(99, efx.getWardOfArceuusTicks(1000));
	}

	@Test
	public void ward_countsDownWithCurrentTick()
	{
		EffectState efx = new EffectState();
		efx.applyWardOfArceuus(1000, 99);
		assertEquals(89, efx.getWardOfArceuusTicks(1010));
		assertEquals(1,  efx.getWardOfArceuusTicks(1098));
		assertEquals(0,  efx.getWardOfArceuusTicks(1099));
	}

	@Test
	public void ward_cooldown_50ticks()
	{
		EffectState efx = new EffectState();
		efx.applyWardCooldown(1000);
		assertEquals(50, efx.getWardCooldownTicks(1000));
		assertEquals(25, efx.getWardCooldownTicks(1025));
		assertEquals(1,  efx.getWardCooldownTicks(1049));
		assertEquals(0,  efx.getWardCooldownTicks(1050));
	}

	@Test
	public void ward_clearBuff_removesBuffOnly()
	{
		EffectState efx = new EffectState();
		efx.applyWardOfArceuus(1000, 99);
		efx.applyWardCooldown(1000);
		efx.clearWardOfArceuus();
		assertEquals(0,  efx.getWardOfArceuusTicks(1010));
		assertEquals(40, efx.getWardCooldownTicks(1010)); // cooldown still running
	}

	@Test
	public void ward_clearCooldown_removesCooldownOnly()
	{
		EffectState efx = new EffectState();
		efx.applyWardOfArceuus(1000, 99);
		efx.applyWardCooldown(1000);
		efx.clearWardCooldown();
		assertEquals(89, efx.getWardOfArceuusTicks(1010)); // buff still running
		assertEquals(0,  efx.getWardCooldownTicks(1010));
	}

	@Test
	public void ward_reset_clearsBothBuffAndCooldown()
	{
		EffectState efx = new EffectState();
		efx.applyWardOfArceuus(1000, 99);
		efx.applyWardCooldown(1000);
		efx.reset();
		assertEquals(0, efx.getWardOfArceuusTicks(1005));
		assertEquals(0, efx.getWardCooldownTicks(1005));
	}

	// ── Mark of Darkness buff + cooldown ─────────────────────────────────────

	@Test
	public void mark_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getMarkOfDarknessTicks(1000));
		assertEquals(0, efx.getMarkCooldownTicks(1000));
	}

	@Test
	public void mark_applyAtMagicLevel99_equals297ticks()
	{
		// magicLevel × 3 = 99 × 3 = 297
		EffectState efx = new EffectState();
		efx.applyMarkOfDarkness(1000, 99);
		assertEquals(297, efx.getMarkOfDarknessTicks(1000));
	}

	@Test
	public void mark_countsDownWithCurrentTick()
	{
		EffectState efx = new EffectState();
		efx.applyMarkOfDarkness(1000, 99);
		assertEquals(287, efx.getMarkOfDarknessTicks(1010));
		assertEquals(1,   efx.getMarkOfDarknessTicks(1296));
		assertEquals(0,   efx.getMarkOfDarknessTicks(1297));
	}

	@Test
	public void mark_cooldown_10ticks()
	{
		EffectState efx = new EffectState();
		efx.applyMarkCooldown(1000);
		assertEquals(10, efx.getMarkCooldownTicks(1000));
		assertEquals(5,  efx.getMarkCooldownTicks(1005));
		assertEquals(1,  efx.getMarkCooldownTicks(1009));
		assertEquals(0,  efx.getMarkCooldownTicks(1010));
	}

	@Test
	public void mark_clearBuff_removesBuffOnly()
	{
		EffectState efx = new EffectState();
		efx.applyMarkOfDarkness(1000, 99);
		efx.applyMarkCooldown(1000);
		efx.clearMarkOfDarkness();
		assertEquals(0, efx.getMarkOfDarknessTicks(1005));
		assertEquals(5, efx.getMarkCooldownTicks(1005)); // cooldown still running
	}

	@Test
	public void mark_clearCooldown_removesCooldownOnly()
	{
		EffectState efx = new EffectState();
		efx.applyMarkOfDarkness(1000, 99);
		efx.applyMarkCooldown(1000);
		efx.clearMarkCooldown();
		assertEquals(292, efx.getMarkOfDarknessTicks(1005)); // buff still running
		assertEquals(0,   efx.getMarkCooldownTicks(1005));
	}

	@Test
	public void mark_reset_clearsBothBuffAndCooldown()
	{
		EffectState efx = new EffectState();
		efx.applyMarkOfDarkness(1000, 99);
		efx.applyMarkCooldown(1000);
		efx.reset();
		assertEquals(0, efx.getMarkOfDarknessTicks(1005));
		assertEquals(0, efx.getMarkCooldownTicks(1005));
	}

	// ── Corruption drain tracking ─────────────────────────────────────────────

	@Test
	public void corruption_notActive_returnsZero()
	{
		EffectState efx = new EffectState();
		assertFalse(efx.isCorruptionActive());
		assertEquals(0, efx.getCorruptionIterationsRemaining());
		assertEquals(0, efx.getCorruptionTotalTicks(1000));
		assertEquals(0, efx.getCorruptionNextDrainTicks(1000));
	}

	@Test
	public void corruption_setTarget_isActive()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(10);
		assertTrue(efx.isCorruptionActive());
		assertEquals(10, efx.getCorruptionIterationsRemaining());
	}

	@Test
	public void corruption_allIterationsConsumed_notActive()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(3);
		efx.setCorruptionCurrentIterations(3);
		assertFalse(efx.isCorruptionActive());
		assertEquals(0, efx.getCorruptionIterationsRemaining());
	}

	@Test
	public void corruption_intervalDetectedAfterTwoIncrements()
	{
		// First increment: no interval known yet (only one data point)
		// Second increment: interval = tick2 - tick1
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(10);
		efx.setCorruptionCurrentIterations(1);
		efx.onCorruptionIterationChanged(1000); // first increment — interval unknown

		assertEquals(0, efx.getCorruptionTotalTicks(1000)); // interval not yet known

		efx.setCorruptionCurrentIterations(2);
		efx.onCorruptionIterationChanged(1015); // second increment — interval = 15

		// interval=15, nextDrainTick=1030, remaining=8 iterations
		// total = (1030 - 1015) + (8-1)*15 = 15 + 105 = 120
		assertEquals(120, efx.getCorruptionTotalTicks(1015));
	}

	@Test
	public void corruption_nextDrainTicks_afterTwoIncrements()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(10);
		efx.setCorruptionCurrentIterations(1);
		efx.onCorruptionIterationChanged(1000);

		efx.setCorruptionCurrentIterations(2);
		efx.onCorruptionIterationChanged(1015);

		// nextDrainTick = 1030; at tick 1020: 1030 - 1020 = 10
		assertEquals(10, efx.getCorruptionNextDrainTicks(1020));
		assertEquals(0,  efx.getCorruptionNextDrainTicks(1030));
	}

	@Test
	public void corruption_totalTicks_countsDownBetweenIncrements()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(5);
		efx.setCorruptionCurrentIterations(1);
		efx.onCorruptionIterationChanged(1000);
		efx.setCorruptionCurrentIterations(2);
		efx.onCorruptionIterationChanged(1020); // interval = 20

		// At tick 1020: remaining=3, nextDrainTick=1040
		// total = (1040-1020) + (3-1)*20 = 20 + 40 = 60
		assertEquals(60, efx.getCorruptionTotalTicks(1020));

		// At tick 1025: (1040-1025) + 2*20 = 15 + 40 = 55
		assertEquals(55, efx.getCorruptionTotalTicks(1025));
	}

	@Test
	public void corruption_clearCorruption_resetsAll()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(10);
		efx.setCorruptionCurrentIterations(1);
		efx.onCorruptionIterationChanged(1000);
		efx.setCorruptionCurrentIterations(2);
		efx.onCorruptionIterationChanged(1015);
		efx.setCorruptionBaseDamage(5);

		efx.clearCorruption();

		assertFalse(efx.isCorruptionActive());
		assertEquals(0, efx.getCorruptionTotalTicks(1020));
		assertEquals(0, efx.getCorruptionNextDrainTicks(1020));
		assertEquals(0, efx.getCorruptionBaseDamage());
	}

	@Test
	public void corruption_reset_clearsAll()
	{
		EffectState efx = new EffectState();
		efx.setCorruptionTargetIterations(10);
		efx.setCorruptionCurrentIterations(2);
		efx.onCorruptionIterationChanged(1000);
		efx.reset();
		assertFalse(efx.isCorruptionActive());
		assertEquals(0, efx.getCorruptionTotalTicks(1005));
	}

	// ── Burn damage counters ──────────────────────────────────────────────────

	@Test
	public void burn_defaultZero()
	{
		EffectState efx = new EffectState();
		assertEquals(0, efx.getBurnDamageTotal());
		assertEquals(0, efx.getBurnDamageNextHit());
	}

	@Test
	public void burn_setTotal_roundtrips()
	{
		EffectState efx = new EffectState();
		efx.setBurnDamageTotal(47);
		assertEquals(47, efx.getBurnDamageTotal());
	}

	@Test
	public void burn_setNextHit_roundtrips()
	{
		EffectState efx = new EffectState();
		efx.setBurnDamageNextHit(3);
		assertEquals(3, efx.getBurnDamageNextHit());
	}

	@Test
	public void burn_reset_clearsCounters()
	{
		EffectState efx = new EffectState();
		efx.setBurnDamageTotal(99);
		efx.setBurnDamageNextHit(5);
		efx.reset();
		assertEquals(0, efx.getBurnDamageTotal());
		assertEquals(0, efx.getBurnDamageNextHit());
	}
}
