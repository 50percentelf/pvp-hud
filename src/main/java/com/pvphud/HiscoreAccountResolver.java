package com.pvphud;

import com.pvphud.state.OpponentStats;

/**
 * Helper for the hiscore account-type polling loop.
 *
 * Since {@code HiscoreManager.lookupAsync} returns null for both
 * "still loading" and "player not on this endpoint", we cannot track
 * resolved-negative state. Instead we track the best positive account
 * type observed across polls and commit at a bounded deadline.
 */
public final class HiscoreAccountResolver
{
	private HiscoreAccountResolver() {}

	/**
	 * Returns the higher-precedence account type between {@code current} and
	 * {@code candidate}. Precedence: HC &gt; UIM &gt; IM &gt; NORMAL.
	 */
	public static OpponentStats.AccountType best(
			OpponentStats.AccountType current,
			OpponentStats.AccountType candidate)
	{
		return candidate.ordinal() > current.ordinal() ? candidate : current;
	}

	/**
	 * Returns true when {@code type} is the highest possible classification.
	 * A HC-positive observation can terminate the polling loop immediately
	 * because nothing outranks it.
	 */
	public static boolean isTerminal(OpponentStats.AccountType type)
	{
		return type == OpponentStats.AccountType.HARDCORE_IRONMAN;
	}
}
