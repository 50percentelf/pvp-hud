package com.pvphud.state;

/**
 * Poison, venom, and protection state for the local player.
 *
 * antiPoisonTicks and antiVenomTicks are self-decremented each game tick and
 * re-synchronised whenever the POISON VarPlayer fires.
 */
public class PoisonState
{
	private boolean poisoned;
	private boolean venomed;
	private boolean antiPoisonActive;
	private boolean antiVenomActive;
	private int     antiPoisonTicks;
	private int     antiVenomTicks;

	public boolean isPoisoned()         { return poisoned; }
	public boolean isVenomed()          { return venomed; }
	public boolean isAntiPoisonActive() { return antiPoisonActive; }
	public boolean isAntiVenomActive()  { return antiVenomActive; }
	public int     getAntiPoisonTicks() { return antiPoisonTicks; }
	public int     getAntiVenomTicks()  { return antiVenomTicks; }

	public void setPoisoned(boolean v)         { poisoned = v; }
	public void setVenomed(boolean v)          { venomed = v; }
	public void setAntiPoisonActive(boolean v) { antiPoisonActive = v; }
	public void setAntiVenomActive(boolean v)  { antiVenomActive = v; }
	public void setAntiPoisonTicks(int t)      { antiPoisonTicks = Math.max(0, t); }
	public void setAntiVenomTicks(int t)       { antiVenomTicks  = Math.max(0, t); }

	public void tick()
	{
		if (antiPoisonTicks > 0) antiPoisonTicks--;
		if (antiVenomTicks  > 0) antiVenomTicks--;
	}

	public void reset()
	{
		poisoned         = false;
		venomed          = false;
		antiPoisonActive = false;
		antiVenomActive  = false;
		antiPoisonTicks  = 0;
		antiVenomTicks   = 0;
	}
}
