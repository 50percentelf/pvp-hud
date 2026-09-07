package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Local player action cooldowns for the bottom action strip. */
public class ActionClockState
{
	/** Ticks until the player may attack again (0 = ready). */
	@Getter @Setter
	private int attackDelayTicks;

	/** Weapon speed (ticks) at the time of the last attack; 0 = never attacked. */
	@Getter @Setter
	private int lastWeaponSpeedTicks;

	/** Ticks until the player may eat food again (0 = ready). */
	@Getter @Setter
	private int eatCooldownTicks;

	/** Ticks until the player may drink a potion again (0 = ready). */
	@Getter @Setter
	private int potCooldownTicks;

	public boolean isAttackReady()
	{
		return attackDelayTicks <= 0;
	}

	public boolean isEatReady()
	{
		return eatCooldownTicks <= 0;
	}

	public boolean isPotReady()
	{
		return potCooldownTicks <= 0;
	}

	public void reset()
	{
		attackDelayTicks = 0;
		eatCooldownTicks = 0;
		potCooldownTicks = 0;
		lastWeaponSpeedTicks = 0;
	}
}
