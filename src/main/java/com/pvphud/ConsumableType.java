package com.pvphud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Classification of a consumable item for action-clock purposes.
 *
 * Each type records the delays it imposes when successfully consumed:
 *   attackDelay  — ticks added to the attack deadline (additive, may keep ATK green)
 *   eatLock      — ticks before regular food can be eaten again
 *   comboLock    — ticks before combo food (karambwan) can be eaten again
 *   potLock      — ticks before a potion can be drunk again
 */
public enum ConsumableType
{
	/**
	 * Regular food: shark, manta, dark crab, etc.
	 * +3 attack delay, blocks eat channel for 3 ticks.
	 */
	STANDARD_FOOD(3, 3, 0, 0),

	/**
	 * Karambwan (combo-food channel).
	 * +2 attack delay, blocks BOTH eat and combo-eat channels for 3 ticks.
	 * Allows same-tick use alongside standard food.
	 */
	KARAMBWAN(2, 3, 3, 0),

	/**
	 * Standard potions: combat potions, super potions, brews, etc.
	 * +0 attack delay, blocks potion channel for 3 ticks.
	 */
	STANDARD_POTION(0, 0, 0, 3),

	/**
	 * Item could not be classified.  No state is mutated when UNKNOWN is consumed
	 * (avoids silently applying wrong delays to unrecognised items).
	 */
	UNKNOWN(0, 0, 0, 0);

	// ── Known karambwan item IDs ──────────────────────────────────────────────

	/** Cooked karambwan. */
	private static final int KARAMBWAN_ID = 3142;

	private static final Set<Integer> KARAMBWAN_IDS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(KARAMBWAN_ID)));

	// ── Fields ────────────────────────────────────────────────────────────────

	/** Ticks added to the attack deadline (additive, may still leave ATK green). */
	public final int attackDelay;

	/** Ticks before regular food can be eaten again (0 = no eat lock). */
	public final int eatLock;

	/** Ticks before combo food (karambwan) can be eaten again (0 = no combo lock). */
	public final int comboLock;

	/** Ticks before a potion can be drunk again (0 = no pot lock). */
	public final int potLock;

	ConsumableType(int attackDelay, int eatLock, int comboLock, int potLock)
	{
		this.attackDelay = attackDelay;
		this.eatLock     = eatLock;
		this.comboLock   = comboLock;
		this.potLock     = potLock;
	}

	/** Returns true if this type imposes any state change (i.e. is not UNKNOWN). */
	public boolean isKnown()
	{
		return this != UNKNOWN;
	}

	// ── Classification ────────────────────────────────────────────────────────

	/**
	 * Classifies an item by menu option and item ID.
	 *
	 * @param option the menu option string ("Eat", "Drink")
	 * @param itemId the item ID resolved from the inventory slot
	 */
	public static ConsumableType classify(String option, int itemId)
	{
		switch (option)
		{
			case "Eat":
				if (KARAMBWAN_IDS.contains(itemId)) return KARAMBWAN;
				return STANDARD_FOOD;

			case "Drink":
				return STANDARD_POTION;

			default:
				return UNKNOWN;
		}
	}
}
