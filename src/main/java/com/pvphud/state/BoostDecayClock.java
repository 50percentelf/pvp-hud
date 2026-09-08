package com.pvphud.state;

/**
 * Tracks the time until the next natural boost-decay tick (-1 on a boosted stat),
 * accounting for the Preserve prayer using the same logic as RuneLite's
 * BoostsPlugin#getChangeDownTicks().
 *
 * How Preserve works in OSRS:
 *   The 100-tick decay cycle is divided into sections of ~25 ticks (15 s) each.
 *   If Preserve is active during the 4th section (ticks 75-100) the cycle extends
 *   to 125 ticks; if it remains active through the 5th section (100-125) the full
 *   150-tick cycle is granted.  Turning Preserve off after tick 75 forfeits the
 *   extension.  The 100-125 range is a "grace window" where Preserve can still
 *   activate if it was not on earlier.
 *
 * Usage:
 *   sync(currentTick)          — call when a natural exact -1 decay is observed
 *   update(currentTick, pres)  — call once per game tick from onGameTick
 *   getTicksRemaining()        — read from overlay (cached, no side-effects)
 *   isCalibrated()             — true after first sync
 */
public class BoostDecayClock
{
	private int     lastTick          = -1;
	private boolean preserveBeenActive = false;
	private int     ticksRemaining    = -1;

	public boolean isCalibrated()   { return lastTick != -1; }
	public int     getTicksRemaining() { return ticksRemaining; }

	/** Record the tick at which a natural exact -1 boost-decay event was observed. */
	public void sync(int currentTick)
	{
		lastTick           = currentTick;
		preserveBeenActive = false;
		ticksRemaining     = -1; // updated on the next update() call
	}

	/**
	 * Called once per game tick.  Mirrors RuneLite BoostsPlugin#getChangeDownTicks():
	 * computes remaining ticks accounting for Preserve and caches the result.
	 */
	public void update(int currentTick, boolean isPreserveActive)
	{
		if (lastTick == -1)
		{
			ticksRemaining = -1;
			return;
		}

		int elapsed = currentTick - lastTick;

		if ((isPreserveActive && (elapsed < 75 || preserveBeenActive)) || elapsed > 125)
		{
			preserveBeenActive = true;
			ticksRemaining     = 150 - elapsed;
			return;
		}

		preserveBeenActive = false;
		ticksRemaining = elapsed > 100 ? 125 - elapsed : 100 - elapsed;
	}

	public void reset()
	{
		lastTick           = -1;
		preserveBeenActive = false;
		ticksRemaining     = -1;
	}
}
