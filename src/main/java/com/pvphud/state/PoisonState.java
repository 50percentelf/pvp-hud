package com.pvphud.state;

/**
 * Poison, venom, and protection state for the local player.
 *
 * Immunity duration mirrors RuneLite's TimersAndBuffsPlugin phase-aware formula.
 * nextPoisonTick is preserved across varp decrements; only reset when expired.
 *
 * Anti-poison (-38 <= value < 0):
 *   remaining = phase + Math.abs((value + 1) * POISON_TICK_LENGTH)
 *
 * Anti-venom (value < VENOM_VALUE_CUTOFF = -38):
 *   remaining = phase + Math.abs((value + 1 - VENOM_VALUE_CUTOFF) * POISON_TICK_LENGTH)
 *
 * where phase = nextPoisonTick - currentTick.
 */
public class PoisonState
{
	/** Game ticks between each POISON VarPlayer decrement (poison and immunity alike). */
	public static final int POISON_TICK_LENGTH = 30;

	/** Anti-venom threshold: values strictly below this are anti-venom; at or above are anti-poison. */
	public static final int VENOM_VALUE_CUTOFF = -38;

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
	public int     getNextPoisonTick()  { return nextPoisonTick; }

	public void setPoisoned(boolean v)         { poisoned = v; }
	public void setVenomed(boolean v)          { venomed = v; }
	public void setAntiPoisonActive(boolean v) { antiPoisonActive = v; }
	public void setAntiVenomActive(boolean v)  { antiVenomActive = v; }
	public void setPoisonVarpValue(int v)      { poisonVarpValue = v; }
	public void setNextPoisonTick(int t)       { nextPoisonTick = t; }

	/**
	 * Returns immunity ticks remaining using RuneLite's per-type phase-aware formula.
	 * Returns 0 when not tracking.
	 */
	public int getImmunityTicksRemaining(int currentTick)
	{
		if (nextPoisonTick < 0 || poisonVarpValue >= 0) return 0;
		int phase = nextPoisonTick - currentTick;
		int remaining;
		if (poisonVarpValue < VENOM_VALUE_CUTOFF)
			remaining = phase + Math.abs((poisonVarpValue + 1 - VENOM_VALUE_CUTOFF) * POISON_TICK_LENGTH);
		else
			remaining = phase + Math.abs((poisonVarpValue + 1) * POISON_TICK_LENGTH);
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
