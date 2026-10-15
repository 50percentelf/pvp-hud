package com.pvphud.state;

import lombok.Getter;

/**
 * Local-player action timing state, using absolute game-tick deadlines.
 *
 * All "remaining ticks" values are derived on demand as (deadline - currentTick),
 * clamped to zero for display.  The deadlines themselves are never clamped —
 * this preserves "idle attack credit" so that food consumed after a long idle
 * period correctly leaves the attack clock ready.
 *
 * Three independent consumption channels mirror OSRS server mechanics:
 *   foodReadyTick       — regular food (shark, brew, dark crab …)
 *   comboFoodReadyTick  — karambwan / combo-food channel
 *   potionReadyTick     — all potions
 *
 * GameTick handlers must NOT decrement these values — that was the old model.
 * Read remaining ticks through the currentTick-parameterised getters.
 */
public class ActionClockState
{
	/** Sentinel: deadline has never been set for this channel. */
	public static final long NEVER = Long.MIN_VALUE;

	/** Absolute tick when the player may next attack. NEVER = has not attacked. */
	private long attackReadyTick = NEVER;

	/** Speed (ticks) used for the last confirmed attack; 0 = no attack recorded. */
	@Getter private int lastWeaponSpeedTicks;

	/** Absolute tick when the regular food channel unblocks. */
	private long foodReadyTick = NEVER;

	/** Absolute tick when the combo-food (karambwan) channel unblocks. */
	private long comboFoodReadyTick = NEVER;

	/** Absolute tick when the potion channel unblocks. */
	private long potionReadyTick = NEVER;

	// ── Attack ────────────────────────────────────────────────────────────────

	/** True once an attack has been confirmed (attackReadyTick is meaningful). */
	public boolean hasAttacked()
	{
		return attackReadyTick != NEVER;
	}

	/**
	 * Remaining ticks until the player may attack again.
	 * Returns 0 (ready) when the deadline is in the past — the deadline itself
	 * is NOT clamped so food interaction remains correct.
	 */
	public int getAttackDelayTicks(long currentTick)
	{
		if (attackReadyTick == NEVER) return 0;
		return (int) Math.max(0L, attackReadyTick - currentTick);
	}

	/**
	 * Records a confirmed attack.  Sets attackReadyTick = currentTick + speed.
	 * Only advances the deadline; a new attack cannot shorten an already-running
	 * delay from a previous attack on the same or earlier tick.
	 */
	public void setAttack(long currentTick, int speed)
	{
		long candidate = currentTick + speed;
		if (attackReadyTick == NEVER || candidate > attackReadyTick)
		{
			attackReadyTick       = candidate;
			lastWeaponSpeedTicks  = speed;
		}
	}

	/**
	 * ADDITIVELY adds attackDelay to the existing attack deadline.
	 *
	 * This is the critical food-interaction rule:
	 *   attackReadyTick += delay
	 * NOT:
	 *   attackReadyTick = currentTick + delay
	 *
	 * Example: attackReadyTick was tick 100, currentTick is 105 (ATK ready).
	 * Eating normal food (+3): attackReadyTick becomes 103. ATK remains ready.
	 *
	 * Example: attackReadyTick was tick 108, currentTick is 105 (3 ticks left).
	 * Eating normal food (+3): attackReadyTick becomes 111. ATK shows 6 ticks.
	 *
	 * No-op when the player has never attacked (attackReadyTick == NEVER).
	 */
	public void addFoodAttackDelay(int delay)
	{
		if (attackReadyTick != NEVER)
			attackReadyTick += delay;
	}

	// ── Food channel ──────────────────────────────────────────────────────────

	/** Remaining ticks before regular food can be eaten again (0 = ready). */
	public int getEatCooldownTicks(long currentTick)
	{
		if (foodReadyTick == NEVER) return 0;
		return (int) Math.max(0L, foodReadyTick - currentTick);
	}

	/** Remaining ticks before combo food (karambwan) can be eaten again (0 = ready). */
	public int getComboEatCooldownTicks(long currentTick)
	{
		if (comboFoodReadyTick == NEVER) return 0;
		return (int) Math.max(0L, comboFoodReadyTick - currentTick);
	}

	// ── Potion channel ────────────────────────────────────────────────────────

	/** Remaining ticks before a potion can be drunk again (0 = ready). */
	public int getPotCooldownTicks(long currentTick)
	{
		if (potionReadyTick == NEVER) return 0;
		return (int) Math.max(0L, potionReadyTick - currentTick);
	}

	// ── Consumable application ────────────────────────────────────────────────

	/**
	 * Applies all cooldown effects for a confirmed consumable.
	 *
	 * Channels are set to max(existing, currentTick + lock) so that same-tick
	 * combo eating accumulates correctly regardless of event order.
	 * attackReadyTick is modified additively via addFoodAttackDelay.
	 */
	public void applyConsumable(long currentTick, com.pvphud.ConsumableType type)
	{
		if (!type.isKnown()) return;

		addFoodAttackDelay(type.attackDelay);

		if (type.eatLock > 0)
		{
			long candidate = currentTick + type.eatLock;
			if (foodReadyTick == NEVER || candidate > foodReadyTick)
				foodReadyTick = candidate;
		}
		if (type.comboLock > 0)
		{
			long candidate = currentTick + type.comboLock;
			if (comboFoodReadyTick == NEVER || candidate > comboFoodReadyTick)
				comboFoodReadyTick = candidate;
		}
		if (type.potLock > 0)
		{
			long candidate = currentTick + type.potLock;
			if (potionReadyTick == NEVER || candidate > potionReadyTick)
				potionReadyTick = candidate;
		}
	}

	// ── Convenience readiness predicates ──────────────────────────────────────

	public boolean isAttackReady(long currentTick) { return getAttackDelayTicks(currentTick) <= 0; }
	public boolean isEatReady(long currentTick)    { return getEatCooldownTicks(currentTick) <= 0; }
	public boolean isPotReady(long currentTick)    { return getPotCooldownTicks(currentTick) <= 0; }

	// ── Debug / test initialisation ──────────────────────────────────────────────

	/**
	 * For debug/testing: sets attack state as if an attack happened with the given
	 * weapon speed and {@code ticks} ticks still remaining until ready.
	 */
	public void forceAttackDelay(long currentTick, int ticks, int weaponSpeed)
	{
		if (weaponSpeed > 0)
		{
			attackReadyTick      = currentTick + ticks;
			lastWeaponSpeedTicks = weaponSpeed;
		}
	}

	/** For debug/testing: sets eat cooldown ticks remaining directly. */
	public void forceEatCooldown(long currentTick, int ticks)
	{
		foodReadyTick = ticks > 0 ? currentTick + ticks : NEVER;
	}

	/** For debug/testing: sets pot cooldown ticks remaining directly. */
	public void forcePotCooldown(long currentTick, int ticks)
	{
		potionReadyTick = ticks > 0 ? currentTick + ticks : NEVER;
	}

	// ── Reset ──────────────────────────────────────────────────────────────────

	public void reset()
	{
		attackReadyTick     = NEVER;
		lastWeaponSpeedTicks = 0;
		foodReadyTick       = NEVER;
		comboFoodReadyTick  = NEVER;
		potionReadyTick     = NEVER;
	}
}
