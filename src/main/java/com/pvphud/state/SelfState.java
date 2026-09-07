package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Self-effects displayed in the right panel: veng, freeze, TB, poisons. */
public class SelfState
{
	/** Vengeance has been cast and is ready to trigger on next incoming hit. */
	@Getter @Setter
	private boolean vengActive;

	/** Ticks remaining on the local player's freeze (0 = not frozen). */
	@Getter @Setter
	private int freezeTicksRemaining;

	/** Ticks remaining on the local player's Tele Block (0 = not TB'd). */
	@Getter @Setter
	private int teleBlockTicksRemaining;

	@Getter @Setter
	private boolean poisoned;

	@Getter @Setter
	private boolean venomed;

	/** True while an anti-poison potion is protecting against regular poison (POISON < 0). */
	@Getter @Setter
	private boolean antiPoisonActive;

	/** True while an anti-venom potion is protecting against venom (POISON very negative). */
	@Getter @Setter
	private boolean antiVenomActive;

	/** SpriteID of the ice spell that froze the player (0 when not frozen). */
	@Getter @Setter
	private int freezeSpriteId;

	@Getter @Setter private int currentHp;
	@Getter @Setter private int maxHp;
	@Getter @Setter private int currentPrayer;
	@Getter @Setter private int maxPrayer;

	/**
	 * Counts down from 100 to 0 each game tick. Reset to 100 whenever a
	 * natural HP regen tick is detected (HP increased by exactly 1). Used to
	 * show "regen Xt" in the HUD. 0 = timer not yet calibrated.
	 */
	@Getter @Setter private int hpRegenTicksRemaining;

	/** Wall-clock ms of the last incoming hitsplat; -1 = none yet. Used for HP bar shake. */
	@Getter @Setter private long lastIncomingDamageMs = -1;

	/** Run energy 0–100 (polled each game tick from client.getEnergy()). */
	@Getter @Setter private int runEnergy;

	/**
	 * Self-calibrating drain period: the observed tick interval between
	 * consecutive 1-point combat-stat drains back toward base. 0 = never
	 * observed a drain yet (timer hidden).
	 */
	@Getter @Setter private int statDrainPeriod;

	/** Countdown to next expected stat drain tick. */
	@Getter @Setter private int statDrainTicksRemaining;

	public void reset()
	{
		vengActive = false;
		freezeTicksRemaining = 0;
		teleBlockTicksRemaining = 0;
		poisoned = false;
		venomed = false;
		antiPoisonActive = false;
		antiVenomActive = false;
		freezeSpriteId = 0;
		currentHp = 0;
		maxHp = 0;
		currentPrayer = 0;
		maxPrayer = 0;
		hpRegenTicksRemaining = 0;
		statDrainPeriod = 0;
		statDrainTicksRemaining = 0;
		lastIncomingDamageMs = -1;
		runEnergy = 0;
	}
}
