package com.pvphud;

/**
 * Records the INTENT to consume an item (set on MenuOptionClicked).
 * Authoritative cooldowns are only applied once the inventory change confirms
 * that the item was actually consumed.
 *
 * Intents expire after MAX_AGE_TICKS to prevent stale matches against
 * unrelated later inventory updates.
 */
public final class PendingConsumable
{
	/** Maximum ticks before an unconfirmed intent is discarded. */
	static final int MAX_AGE_TICKS = 3;

	/** Item ID from the inventory slot at click time. */
	public final int itemId;

	/** Inventory slot index (0–27). */
	public final int slot;

	/** Client game tick when the menu option was clicked. */
	public final long intentTick;

	/** Derived classification (cached to avoid re-classifying on confirmation). */
	public final ConsumableType type;

	/** True once a confirming inventory change has been processed. */
	private boolean confirmed;

	PendingConsumable(int itemId, int slot, long intentTick, ConsumableType type)
	{
		this.itemId     = itemId;
		this.slot       = slot;
		this.intentTick = intentTick;
		this.type       = type;
	}

	boolean isConfirmed() { return confirmed; }

	void markConfirmed() { confirmed = true; }

	boolean isExpired(long currentTick)
	{
		return currentTick > intentTick + MAX_AGE_TICKS;
	}
}
