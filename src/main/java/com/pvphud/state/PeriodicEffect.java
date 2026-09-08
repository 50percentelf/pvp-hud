package com.pvphud.state;

/**
 * State for a periodic timed effect (Menaphite Remedy, Prayer Regeneration potion).
 *
 * Two separate durations are tracked:
 *   totalTicksRemaining — full effect duration left (shown as "MEN 4:42")
 *   nextProcTicks       — countdown to the next individual proc (shown as "NEXT 0:12")
 *
 * nextProcTicks is self-decremented each tick. totalTicksRemaining is updated
 * from the server varbit each tick. Tasks 5 and 8 wire the specific mechanics.
 */
public class PeriodicEffect
{
	private int totalTicksRemaining;
	private int nextProcTicks;

	public boolean isActive()           { return totalTicksRemaining > 0; }
	public int getTotalTicksRemaining() { return totalTicksRemaining; }
	public int getNextProcTicks()       { return nextProcTicks; }

	public void setTotalTicksRemaining(int t) { totalTicksRemaining = Math.max(0, t); }
	public void setNextProcTicks(int t)       { nextProcTicks = Math.max(0, t); }

	public void tick()
	{
		if (totalTicksRemaining > 0) totalTicksRemaining--;
		if (nextProcTicks        > 0) nextProcTicks--;
	}

	public void reset()
	{
		totalTicksRemaining = 0;
		nextProcTicks       = 0;
	}
}
