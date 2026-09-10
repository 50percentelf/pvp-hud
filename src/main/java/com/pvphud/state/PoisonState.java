package com.pvphud.state;

/**
 * Poison, venom, and protection state for the local player.
 *
 * Anti-poison/anti-venom immunity duration uses RuneLite's phase-aware formula:
 * remaining = (|poisonVarpValue| - 1) * POISON_TICK_LENGTH + (nextPoisonTick - currentTick)
 * nextPoisonTick is reset to currentTick + POISON_TICK_LENGTH on every varp change.
 */
public class PoisonState
{
	/** Game ticks between each POISON VarPlayer decrement (poison and immunity alike). */
	public static final int POISON_TICK_LENGTH = 30;

	private boolean poisoned;
	private boolean venomed;
	private boolean antiPoisonActive;
	private boolean antiVenomActive;

	/** Raw VarPlayer.POISON value when immunity is active (negative). 0 = not tracking. */
	private int poisonVarpValue = 0;

	/** Game tick when the next POISON varp decrement is expected. -1 = not set. */
	private int nextPoisonTick = -1;

	public boolean isPoisoned()         { return poisoned; }
	public boolean isVenomed()          { return venomed; }
	public boolean isAntiPoisonActive() { return antiPoisonActive; }
	public boolean isAntiVenomActive()  { return antiVenomActive; }

	public void setPoisoned(boolean v)         { poisoned = v; }
	public void setVenomed(boolean v)          { venomed = v; }
	public void setAntiPoisonActive(boolean v) { antiPoisonActive = v; }
	public void setAntiVenomActive(boolean v)  { antiVenomActive = v; }
	public void setPoisonVarpValue(int v)      { poisonVarpValue = v; }
	public void setNextPoisonTick(int t)       { nextPoisonTick = t; }

	/**
	 * Returns immunity ticks remaining using RuneLite's phase-aware formula.
	 * Returns 0 when not tracking.
	 */
	public int getImmunityTicksRemaining(int currentTick)
	{
		if (nextPoisonTick < 0 || poisonVarpValue >= 0) return 0;
		int remaining = (Math.abs(poisonVarpValue) - 1) * POISON_TICK_LENGTH
			+ (nextPoisonTick - currentTick);
		return Math.max(0, remaining);
	}

	public void tick()
	{
		// Immunity timing is phase-aware; no self-decrement needed.
	}

	public void reset()
	{
		poisoned         = false;
		venomed          = false;
		antiPoisonActive = false;
		antiVenomActive  = false;
		poisonVarpValue  = 0;
		nextPoisonTick   = -1;
	}
}
