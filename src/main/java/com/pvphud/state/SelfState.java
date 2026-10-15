package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Local-player self state: veng, freeze, teleblock, HP/Prayer, stat-cycle clocks. */
public class SelfState
{
	/** Vengeance has been cast and is ready to trigger on next incoming hit. */
	@Getter @Setter private boolean vengActive;

	/**
	 * Game tick on which the freeze started (-1 = not frozen).
	 * Stored as a start/end tick pair so repeated ice impacts cannot extend it.
	 */
	@Getter private int freezeStartTick = -1;

	/** Game tick on which the freeze expires (-1 = not frozen). */
	private int freezeEndTick = -1;

	/** SpriteID of the spell that froze the player (0 = unknown / not frozen). */
	@Getter @Setter private int freezeSpriteId;

	/**
	 * Game tick when post-thaw refreeze immunity expires (-1 = not immune).
	 * Set to (thawTick + freezeDuration/2) by {@link #thaw(int)}.
	 */
	private int refreezeImmunityEndTick = -1;

	/** Ticks remaining on the local player's Tele Block (0 = not TB'd). */
	@Getter @Setter private int teleBlockTicksRemaining;

	/** True when in the post-TB reapplication immunity window (raw varbit 1–100). */
	@Getter @Setter private boolean tbImmunityActive;

	/**
	 * Game tick when Dragon scimitar Sever's protection-prayer lock expires (-1 = not locked).
	 * Duration: 8 ticks.
	 */
	private int prayerLockEndTick = -1;

	@Getter @Setter private int currentHp;
	@Getter @Setter private int maxHp;
	@Getter @Setter private int currentPrayer;
	@Getter @Setter private int maxPrayer;

	/** Counts down from 100 to 0 each game tick; reset when a natural HP regen tick is detected. */
	@Getter @Setter private int hpRegenTicksRemaining;

	/** Wall-clock ms of the last incoming hitsplat; -1 = none yet. Used for HP bar shake. */
	@Getter @Setter private long lastIncomingDamageMs = -1;

	/** Run energy 0–100 (polled each game tick from client.getEnergy()). */
	@Getter @Setter private int runEnergy;

	/** Clock calibrated from observed natural -1 stat ticks above base (boost decay). */
	@Getter private final BoostDecayClock boostDecay   = new BoostDecayClock();

	/** Clock calibrated from observed natural +1 stat ticks below base (debuff restoration). */
	@Getter private final StatCycleClock debuffRestore = new StatCycleClock();

	// ── Freeze helpers ────────────────────────────────────────────────────────

	public boolean isFrozen(int currentTick)
	{
		return freezeEndTick > currentTick;
	}

	public int getFreezeTicksRemaining(int currentTick)
	{
		if (freezeEndTick < 0) return 0;
		return Math.max(0, freezeEndTick - currentTick);
	}

	/**
	 * Applies a freeze starting at {@code startTick} for {@code durationTicks}.
	 * No-op if the player is already frozen — repeated impacts must not extend it.
	 */
	public void applyFreeze(int startTick, int durationTicks, int spriteId)
	{
		if (isFrozen(startTick)) return;
		freezeStartTick = startTick;
		freezeEndTick   = startTick + durationTicks;
		freezeSpriteId  = spriteId;
	}

	/**
	 * Thaws the freeze naturally (timer expiry) or by movement detection.
	 * Grants refreeze immunity = half the original freeze duration, starting now.
	 */
	public void thaw(int currentTick)
	{
		if (freezeEndTick > 0 && freezeStartTick >= 0)
		{
			int duration = freezeEndTick - freezeStartTick;
			if (duration > 0)
				refreezeImmunityEndTick = currentTick + duration / 2;
		}
		freezeStartTick = -1;
		freezeEndTick   = -1;
		freezeSpriteId  = 0;
	}

	/** Clears the freeze without granting immunity (logout, death, world-hop). */
	public void clearFreeze()
	{
		freezeStartTick         = -1;
		freezeEndTick           = -1;
		freezeSpriteId          = 0;
		refreezeImmunityEndTick = -1;
	}

	// ── Refreeze immunity ─────────────────────────────────────────────────────

	public boolean isRefreezeImmune(int currentTick)
	{
		return refreezeImmunityEndTick > currentTick;
	}

	public int getRefreezeImmunityTicks(int currentTick)
	{
		if (refreezeImmunityEndTick < 0) return 0;
		return Math.max(0, refreezeImmunityEndTick - currentTick);
	}

	// ── Prayer lock (Dragon scimitar Sever) ───────────────────────────────────

	/** Applies an 8-tick protection-prayer lock starting at {@code currentTick}. */
	public void applyPrayerLock(int currentTick)
	{
		prayerLockEndTick = currentTick + 8;
	}

	public boolean isPrayerLocked(int currentTick)
	{
		return prayerLockEndTick > currentTick;
	}

	public int getPrayerLockTicksRemaining(int currentTick)
	{
		if (prayerLockEndTick < 0) return 0;
		return Math.max(0, prayerLockEndTick - currentTick);
	}

	public void reset()
	{
		vengActive = false;
		clearFreeze();
		tbImmunityActive        = false;
		prayerLockEndTick       = -1;
		teleBlockTicksRemaining = 0;
		currentHp               = 0;
		maxHp                   = 0;
		currentPrayer           = 0;
		maxPrayer               = 0;
		hpRegenTicksRemaining   = 0;
		boostDecay.reset();
		debuffRestore.reset();
		lastIncomingDamageMs    = -1;
		runEnergy               = 0;
	}
}
