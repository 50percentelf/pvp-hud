package com.pvphud.state;

/**
 * Self-calibrating countdown for the debuff-restoration cycle (+1 per 100 ticks).
 * Boost decay uses BoostDecayClock instead (Preserve-aware, tick-count-based).
 *
 * The period is estimated from elapsed time between consecutive observations.
 * DEFAULT_PERIOD (100 ticks) is used before calibration.
 */
public class StatCycleClock
{
	/** Default period before first calibration: 100 ticks (60 s, standard OSRS stat cycle). */
	static final int DEFAULT_PERIOD = 100;

	private int period;         // calibrated interval in ticks (0 = never observed)
	private int ticksRemaining; // countdown to the next expected stat change

	public boolean isCalibrated()      { return period > 0; }
	public int     getPeriod()         { return period; }
	public int     getTicksRemaining() { return ticksRemaining; }

	/**
	 * Call when a natural stat-change tick is observed.
	 * Calibrates the period from elapsed ticks since the previous sync
	 * and resets the countdown to the newly calibrated period.
	 */
	public void sync()
	{
		if (period > 0)
		{
			int elapsed = period - ticksRemaining;
			// Accept elapsed in [10, 200]: rejects duplicate events and extreme lag.
			if (elapsed >= 10 && elapsed <= 200) period = elapsed;
		}
		else
		{
			period = DEFAULT_PERIOD;
		}
		ticksRemaining = period;
	}

	public void tick()
	{
		if (ticksRemaining > 0) ticksRemaining--;
	}

	public void reset()
	{
		period         = 0;
		ticksRemaining = 0;
	}
}
