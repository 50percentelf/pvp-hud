package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** One manual countdown timer (T1 or T2). Managed entirely by user input. */
public class ManualTimerState
{
	/** Wall-clock deadline in ms; -1 when not running. */
	private long deadlineMs = -1;

	/** Configured duration for start/restart. */
	@Getter @Setter
	private long durationMs = 5 * 60 * 1000L;

	public boolean isRunning()
	{
		return deadlineMs >= 0;
	}

	public long getRemainingMs()
	{
		if (deadlineMs < 0)
		{
			return 0;
		}
		return Math.max(0, deadlineMs - System.currentTimeMillis());
	}

	/** Start (or restart) using the configured duration. */
	public void start()
	{
		deadlineMs = System.currentTimeMillis() + durationMs;
	}

	/** Start with a specific duration and store it as the new default. */
	public void start(long durationMs)
	{
		this.durationMs = durationMs;
		deadlineMs = System.currentTimeMillis() + durationMs;
	}

	public void clear()
	{
		deadlineMs = -1;
	}
}
