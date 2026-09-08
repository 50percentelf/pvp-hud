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
	private int freezeStartTick = -1;

	/** Game tick on which the freeze expires (-1 = not frozen). */
	private int freezeEndTick = -1;

	/** SpriteID of the spell that froze the player (0 = unknown / not frozen). */
	@Getter @Setter private int freezeSpriteId;

	/** Ticks remaining on the local player's Tele Block (0 = not TB'd). */
	@Getter @Setter private int teleBlockTicksRemaining;

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

	/** Clears the freeze unconditionally (logout, world-hop, explicit cancellation). */
	public void clearFreeze()
	{
		freezeStartTick = -1;
		freezeEndTick   = -1;
		freezeSpriteId  = 0;
	}

	public void reset()
	{
		vengActive = false;
		clearFreeze();
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
