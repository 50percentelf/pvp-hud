package com.pvphud.state;

/**
 * PvP protection timers with correct OSRS values.
 *
 * PJ safe timer
 *   Wilderness (singles/multi): 20 ticks / 12 s.
 *   PvP worlds (since 25 Mar 2026): 16 ticks / 9.6 s.
 *   Bounty Hunter does not use this system.
 *   Refreshed on EVERY attack between the two players, including 0-damage.
 *
 * Combat-logout restriction
 *   16 ticks / 9.6 s after the last INCOMING hit.
 *   Outgoing attacks do NOT refresh this timer.
 *
 * Under-attack lock
 *   20 ticks / 12 s after receiving an incoming hit.
 *   During this window the player can only attack back at their current attacker.
 *   Reset each time another incoming hit lands.
 *
 * LMS post-kill immunity
 *   33 ticks / ~20 s — only set when the plugin detects the player is in LMS.
 */
public class ProtectionState
{
	private int pjSafeTicksRemaining;
	private int combatLogoutTicksRemaining;
	private int underAttackLockTicksRemaining;
	private int lmsImmuneTicksRemaining;

	public int  getPjSafeTicksRemaining()            { return pjSafeTicksRemaining; }
	public int  getCombatLogoutTicksRemaining()       { return combatLogoutTicksRemaining; }
	public int  getUnderAttackLockTicksRemaining()    { return underAttackLockTicksRemaining; }
	public int  getLmsImmuneTicksRemaining()          { return lmsImmuneTicksRemaining; }

	public boolean isPjSafe()            { return pjSafeTicksRemaining > 0; }
	public boolean isInCombatLogoutLock(){ return combatLogoutTicksRemaining > 0; }
	public boolean isUnderAttackLocked() { return underAttackLockTicksRemaining > 0; }
	public boolean isLmsImmune()         { return lmsImmuneTicksRemaining > 0; }

	/**
	 * Called on every attack exchange between the local player and their opponent,
	 * including 0-damage (splash) hits — OSRS PJ protection activates on any attack.
	 *
	 * @param pjTicks 20 for normal Wilderness, 16 for PvP worlds.
	 */
	public void onAttackExchanged(int pjTicks)
	{
		pjSafeTicksRemaining = pjTicks;
	}

	/**
	 * Called when any incoming hit (including 0-damage) lands on the local player.
	 * Sets the 16-tick logout restriction and the 20-tick under-attack lock.
	 */
	public void onIncomingHit()
	{
		combatLogoutTicksRemaining    = 16;
		underAttackLockTicksRemaining = 20;
	}

	/**
	 * Called when the local player kills an opponent while inside LMS.
	 * Must NOT be called for normal Wilderness or PvP-world kills.
	 */
	public void onKillInLms()
	{
		lmsImmuneTicksRemaining = 33;
	}

	/** Decrement all timers by one game tick. */
	public void tick()
	{
		if (pjSafeTicksRemaining           > 0) pjSafeTicksRemaining--;
		if (combatLogoutTicksRemaining     > 0) combatLogoutTicksRemaining--;
		if (underAttackLockTicksRemaining  > 0) underAttackLockTicksRemaining--;
		if (lmsImmuneTicksRemaining        > 0) lmsImmuneTicksRemaining--;
	}

	public void reset()
	{
		pjSafeTicksRemaining           = 0;
		combatLogoutTicksRemaining     = 0;
		underAttackLockTicksRemaining  = 0;
		lmsImmuneTicksRemaining        = 0;
	}
}
