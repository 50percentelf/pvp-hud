package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Hiscores-derived base stats for the current opponent (populated asynchronously). */
public class OpponentStats
{
	/** Account type inferred from hiscores endpoint matches (ordered by specificity). */
	public enum AccountType { NORMAL, IRONMAN, ULTIMATE_IRONMAN, HARDCORE_IRONMAN }

	/** Base stat levels; -1 = not yet loaded. */
	@Getter @Setter private int attack    = -1;
	@Getter @Setter private int strength  = -1;
	@Getter @Setter private int defence   = -1;
	@Getter @Setter private int ranged    = -1;
	@Getter @Setter private int magic     = -1;
	@Getter @Setter private int hitpoints = -1;

	@Getter @Setter private AccountType accountType = AccountType.NORMAL;

	public void reset()
	{
		attack = strength = defence = ranged = magic = hitpoints = -1;
		accountType = AccountType.NORMAL;
	}
}
