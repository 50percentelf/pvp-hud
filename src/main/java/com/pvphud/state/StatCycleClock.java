package com.pvphud.state;

/**
 * Self-calibrating countdown for a repeating natural stat-change cycle.
 *
 * Two separate instances are used in SelfState:
 *   boostDecay    — calibrated from observed natural -1 ticks above base
 *   debuffRestore — calibrated from observed natural +1 ticks below base
 *
 * The period is estimated from elapsed time between consecutive observations.
 * The default of 40 ticks is used before calibration. Task 4 wires these
 * clocks to the correct StatChanged events.
 */
public class StatCycleClock
{
	/** Default period before first calibration: 100 ticks (60 s, the standard OSRS stat cycle). */
	static final int DEFAULT_PERIOD = 100;

	private int period;         // calibrated interval in ticks (0 = never observed)
	private int ticksRemaining; // countdown to the next expected stat change

	public boolean isCalibrated()     { return period > 0; }
	public int     getPeriod()        { return period; }
	public int     getTicksRemaining(){ return ticksRemaining; }

	/**
	 * Call when a natural stat-change tick is observed.
	 * Calibrates the period from the elapsed tick count since the previous sync
	 * and resets the countdown to the newly calibrated period.
	 * Uses {@link #DEFAULT_PERIOD} (100 t) before the first calibration.
	 */
	public void sync()
	{
		sync(DEFAULT_PERIOD);
	}

	/**
	 * Variant of {@link #sync()} that accepts an explicit {@code defaultPeriod}
	 * used only before the first calibration.  Pass 150 when Preserve is active.
	 */
	public void sync(int defaultPeriod)
	{
		if (period > 0)
		{
			int elapsed = period - ticksRemaining;
			// Accept elapsed in [10, 200]: rejects spurious duplicate events (< 10)
			// and missed observations / extreme lag (> 200).
			if (elapsed >= 10 && elapsed <= 200) period = elapsed;
		}
		else
		{
			period = defaultPeriod;
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
