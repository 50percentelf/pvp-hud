package com.pvphud.state;

/**
 * Tracks protection windows after PvP events:
 *   - PJ safe:      ticks after a kill before another player can attack you
 *   - Combat lock:  ticks until the combat flag clears (prevents logout)
 *   - Immune:       post-kill immunity period (LMS)
 *   - Switch lock:  brief delay after switching targets (tracks re-targeting flicker)
 */
public class ProtectionState
{
	private int pjSafeTicksRemaining;
	private int combatLogoutTicksRemaining;
	private int immuneTicksRemaining;
	private int targetSwitchLockTicksRemaining;

	public int  getPjSafeTicksRemaining()           { return pjSafeTicksRemaining; }
	public int  getCombatLogoutTicksRemaining()      { return combatLogoutTicksRemaining; }
	public int  getImmuneTicksRemaining()            { return immuneTicksRemaining; }
	public int  getTargetSwitchLockTicksRemaining()  { return targetSwitchLockTicksRemaining; }

	public boolean isPjSafe()             { return pjSafeTicksRemaining > 0; }
	public boolean isInCombatLogoutLock() { return combatLogoutTicksRemaining > 0; }
	public boolean isImmune()             { return immuneTicksRemaining > 0; }
	public boolean isTargetSwitchLocked() { return targetSwitchLockTicksRemaining > 0; }

	/**
	 * Called when the local player's current opponent dies (HP ratio → 0).
	 *
	 * @param pjSafeTicks  ticks of PJ protection (e.g. 59 = ~36 s, wilderness standard)
	 * @param immuneTicks  ticks of post-kill immunity (e.g. 100 = 60 s, LMS)
	 */
	public void onKill(int pjSafeTicks, int immuneTicks)
	{
		pjSafeTicksRemaining = pjSafeTicks;
		immuneTicksRemaining = immuneTicks;
	}

	/** Called when any incoming hitsplat lands on the local player. */
	public void onIncomingDamage()
	{
		combatLogoutTicksRemaining = 10;
	}

	/** Called when the local player deals an outgoing hit. */
	public void onOutgoingDamage()
	{
		if (combatLogoutTicksRemaining < 10)
			combatLogoutTicksRemaining = 10;
	}

	/** Called when the local player switches to a genuinely new opponent. */
	public void onTargetSwitch()
	{
		targetSwitchLockTicksRemaining = 3;
	}

	/** Decrement all timers by one game tick. */
	public void tick()
	{
		if (pjSafeTicksRemaining           > 0) pjSafeTicksRemaining--;
		if (combatLogoutTicksRemaining     > 0) combatLogoutTicksRemaining--;
		if (immuneTicksRemaining           > 0) immuneTicksRemaining--;
		if (targetSwitchLockTicksRemaining > 0) targetSwitchLockTicksRemaining--;
	}

	public void reset()
	{
		pjSafeTicksRemaining           = 0;
		combatLogoutTicksRemaining     = 0;
		immuneTicksRemaining           = 0;
		targetSwitchLockTicksRemaining = 0;
	}
}
