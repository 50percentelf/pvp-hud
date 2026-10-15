package com.pvphud;

import com.pvphud.state.ActionClockState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the absolute-tick-deadline action clock model.
 *
 * All "remaining ticks" values are derived as (deadline - currentTick),
 * clamped to 0 for display.  The raw deadlines are never clamped — this
 * preserves idle-attack credit for food interactions.
 */
public class ActionClockTest
{
	private ActionClockState clock;

	@Before
	public void setUp()
	{
		clock = new ActionClockState();
	}

	// ── Attack delay ──────────────────────────────────────────────────────────

	@Test
	public void attack_beforeFirstHit_returnsZeroDelay()
	{
		assertFalse("has not attacked", clock.hasAttacked());
		assertEquals(0, clock.getAttackDelayTicks(100));
	}

	@Test
	public void attack_fourTickWeapon_delayCorrectOverTime()
	{
		clock.setAttack(100, 4);

		assertEquals(4, clock.getAttackDelayTicks(100));
		assertEquals(3, clock.getAttackDelayTicks(101));
		assertEquals(1, clock.getAttackDelayTicks(103));
		assertEquals(0, clock.getAttackDelayTicks(104));
		assertEquals(0, clock.getAttackDelayTicks(110)); // well past deadline — still 0
	}

	@Test
	public void attack_fiveTickWeapon_delayCorrectOverTime()
	{
		clock.setAttack(200, 5);

		assertEquals(5, clock.getAttackDelayTicks(200));
		assertEquals(0, clock.getAttackDelayTicks(205));
		assertEquals(0, clock.getAttackDelayTicks(999));
	}

	@Test
	public void attack_setAttack_doesNotShortenRunningDelay()
	{
		clock.setAttack(100, 5); // deadline = 105
		clock.setAttack(100, 4); // candidate = 104 < 105 — must be ignored

		assertEquals(5, clock.getAttackDelayTicks(100));
		assertEquals(0, clock.getAttackDelayTicks(105));
	}

	@Test
	public void attack_setAttack_advancesDeadlineForFasterWeapon_afterReady()
	{
		clock.setAttack(100, 5); // deadline = 105
		// At tick 106 we're already past the deadline, so a new setAttack advances it
		clock.setAttack(106, 4); // deadline = 110

		assertEquals(4, clock.getAttackDelayTicks(106));
		assertEquals(0, clock.getAttackDelayTicks(110));
	}

	@Test
	public void attack_lastWeaponSpeedTicks_updatesOnFirstAttack()
	{
		clock.setAttack(100, 4);
		assertEquals(4, clock.getLastWeaponSpeedTicks());
	}

	@Test
	public void attack_lastWeaponSpeedTicks_doesNotUpdateWhenDeadlineNotAdvanced()
	{
		clock.setAttack(100, 5); // deadline 105, speed=5
		clock.setAttack(100, 4); // candidate 104 < 105 — ignored, speed stays 5

		assertEquals(5, clock.getLastWeaponSpeedTicks());
	}

	@Test
	public void attack_isReady_trueWhenDelayZero()
	{
		clock.setAttack(100, 4);
		assertFalse(clock.isAttackReady(100));
		assertFalse(clock.isAttackReady(103));
		assertTrue(clock.isAttackReady(104));
		assertTrue(clock.isAttackReady(200));
	}

	// ── Food interaction — additive attack delay ──────────────────────────────

	@Test
	public void food_standardFood_addsThreeToAttackDeadlineWhenActive()
	{
		clock.setAttack(100, 5); // deadline = 105, remaining at tick 102 = 3
		clock.applyConsumable(102, ConsumableType.STANDARD_FOOD); // +3 → deadline 108

		assertEquals(6, clock.getAttackDelayTicks(102)); // was 3, now 6
		assertEquals(0, clock.getAttackDelayTicks(108));
	}

	@Test
	public void food_standardFood_idleCredit_attackRemainsReadyAfterLongIdle()
	{
		// Attack happened at tick 100 with speed 5 → deadline = 105.
		// At tick 200 (way past deadline), eating food adds 3: deadline = 108.
		// getAttackDelayTicks(200) = max(0, 108 - 200) = 0 → ATK still green.
		clock.setAttack(100, 5); // deadline = 105
		clock.applyConsumable(200, ConsumableType.STANDARD_FOOD); // deadline = 108

		assertEquals(0, clock.getAttackDelayTicks(200)); // idle credit preserved
	}

	@Test
	public void food_standardFood_noAttackYet_addFoodDelayIsNoOp()
	{
		// No attack recorded — addFoodAttackDelay must be a no-op.
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);

		assertFalse(clock.hasAttacked());
		assertEquals(0, clock.getAttackDelayTicks(100));
	}

	@Test
	public void food_karambwan_addsTwoToAttackDeadline()
	{
		clock.setAttack(100, 4); // deadline = 104
		clock.applyConsumable(102, ConsumableType.KARAMBWAN); // +2 → deadline 106

		assertEquals(4, clock.getAttackDelayTicks(102)); // was 2, now 4
		assertEquals(0, clock.getAttackDelayTicks(106));
	}

	@Test
	public void food_potion_addsZeroToAttackDeadline()
	{
		clock.setAttack(100, 4); // deadline = 104
		int before = clock.getAttackDelayTicks(102);
		clock.applyConsumable(102, ConsumableType.STANDARD_POTION); // +0

		assertEquals(before, clock.getAttackDelayTicks(102)); // unchanged
	}

	// ── Combo eat — both channels accumulate correctly ────────────────────────

	@Test
	public void combo_standardPlusBwan_totalAttackDelayFive()
	{
		clock.setAttack(100, 4); // deadline = 104
		// Eat standard food (+3) then karambwan (+2) on tick 102
		clock.applyConsumable(102, ConsumableType.STANDARD_FOOD);
		clock.applyConsumable(102, ConsumableType.KARAMBWAN);

		// was 2 remaining + 3 + 2 = 7 total from tick 102
		assertEquals(7, clock.getAttackDelayTicks(102));
	}

	@Test
	public void combo_orderIndependent_karambwanThenStandard()
	{
		clock.setAttack(100, 4); // deadline = 104
		clock.applyConsumable(102, ConsumableType.KARAMBWAN);
		clock.applyConsumable(102, ConsumableType.STANDARD_FOOD);

		assertEquals(7, clock.getAttackDelayTicks(102)); // same result regardless of order
	}

	// ── Eat cooldown channel ──────────────────────────────────────────────────

	@Test
	public void eatCooldown_standardFood_blocksEatChannelThreeTicks()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);

		assertEquals(3, clock.getEatCooldownTicks(100));
		assertEquals(2, clock.getEatCooldownTicks(101));
		assertEquals(1, clock.getEatCooldownTicks(102));
		assertEquals(0, clock.getEatCooldownTicks(103));
	}

	@Test
	public void eatCooldown_karambwan_blocksEatChannelThreeTicks()
	{
		clock.applyConsumable(100, ConsumableType.KARAMBWAN);

		assertEquals(3, clock.getEatCooldownTicks(100));
		assertEquals(0, clock.getEatCooldownTicks(103));
	}

	@Test
	public void eatCooldown_potion_doesNotBlockEatChannel()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_POTION);

		assertEquals(0, clock.getEatCooldownTicks(100));
	}

	@Test
	public void eatCooldown_sameTickCombo_doesNotDoubleStack()
	{
		// Two foods on the same tick must not stack eat lock beyond 3.
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);
		clock.applyConsumable(100, ConsumableType.KARAMBWAN);

		assertEquals(3, clock.getEatCooldownTicks(100)); // max(3, 3) = 3
	}

	// ── Combo food (karambwan) channel ────────────────────────────────────────

	@Test
	public void comboCooldown_karambwan_blocksComboChannelThreeTicks()
	{
		clock.applyConsumable(100, ConsumableType.KARAMBWAN);

		assertEquals(3, clock.getComboEatCooldownTicks(100));
		assertEquals(0, clock.getComboEatCooldownTicks(103));
	}

	@Test
	public void comboCooldown_standardFood_doesNotBlockComboChannel()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);

		assertEquals(0, clock.getComboEatCooldownTicks(100));
	}

	// ── Potion channel ────────────────────────────────────────────────────────

	@Test
	public void potCooldown_standardPotion_blocksPotChannelThreeTicks()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_POTION);

		assertEquals(3, clock.getPotCooldownTicks(100));
		assertEquals(1, clock.getPotCooldownTicks(102));
		assertEquals(0, clock.getPotCooldownTicks(103));
	}

	@Test
	public void potCooldown_food_doesNotBlockPotChannel()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);

		assertEquals(0, clock.getPotCooldownTicks(100));
	}

	// ── UNKNOWN consumable ────────────────────────────────────────────────────

	@Test
	public void unknown_noStateChange()
	{
		clock.applyConsumable(100, ConsumableType.UNKNOWN);

		assertFalse(clock.hasAttacked());
		assertEquals(0, clock.getEatCooldownTicks(100));
		assertEquals(0, clock.getPotCooldownTicks(100));
	}

	// ── Reset ─────────────────────────────────────────────────────────────────

	@Test
	public void reset_clearsAllState()
	{
		clock.setAttack(100, 4);
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD);
		clock.applyConsumable(100, ConsumableType.KARAMBWAN);
		clock.applyConsumable(100, ConsumableType.STANDARD_POTION);

		clock.reset();

		assertFalse(clock.hasAttacked());
		assertEquals(0, clock.getLastWeaponSpeedTicks());
		assertEquals(0, clock.getAttackDelayTicks(100));
		assertEquals(0, clock.getEatCooldownTicks(100));
		assertEquals(0, clock.getComboEatCooldownTicks(100));
		assertEquals(0, clock.getPotCooldownTicks(100));
	}

	// ── Force setters (debug/testing) ─────────────────────────────────────────

	@Test
	public void force_attackDelay_setsCorrectDeadlineAndSpeed()
	{
		clock.forceAttackDelay(100, 3, 5);

		assertEquals(3, clock.getAttackDelayTicks(100));
		assertEquals(5, clock.getLastWeaponSpeedTicks());
		assertTrue(clock.hasAttacked());
	}

	@Test
	public void force_eatCooldown_setsCorrectDeadline()
	{
		clock.forceEatCooldown(100, 2);

		assertEquals(2, clock.getEatCooldownTicks(100));
		assertEquals(0, clock.getEatCooldownTicks(102));
	}

	@Test
	public void force_potCooldown_setsCorrectDeadline()
	{
		clock.forcePotCooldown(100, 1);

		assertEquals(1, clock.getPotCooldownTicks(100));
		assertEquals(0, clock.getPotCooldownTicks(101));
	}

	@Test
	public void force_eatCooldown_zeroTicks_setsToNever()
	{
		clock.applyConsumable(100, ConsumableType.STANDARD_FOOD); // eat lock active
		clock.forceEatCooldown(100, 0); // explicitly clear

		assertEquals(0, clock.getEatCooldownTicks(100));
	}
}
