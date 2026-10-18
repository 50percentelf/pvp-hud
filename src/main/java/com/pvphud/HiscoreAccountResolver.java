package com.pvphud;

import com.pvphud.state.OpponentStats;

/**
 * Pure state machine for account-type resolution across the three ironman
 * hiscore endpoints. Decoupled from the live {@code HiscoreManager} so the
 * polling plugin can report "per-endpoint resolved + positive" state and get
 * a single verdict back.
 *
 * Precedence (highest to lowest): HC-IM, UIM, IM, NORMAL. The resolver only
 * reports {@link ResolverResult#PENDING} while a higher-precedence endpoint
 * has not yet returned a cached response ({@code lookupAsync} returned null).
 * Once every endpoint has resolved, the best match becomes the final answer.
 *
 * "Resolved" means {@code HiscoreManager.lookupAsync} returned a non-null
 * {@code HiscoreResult} — the cache has an authoritative answer, positive
 * or empty. "Positive" means that resolved result reported a hitpoints
 * level &gt; 0 for the endpoint.
 */
public final class HiscoreAccountResolver
{
	private HiscoreAccountResolver() {}

	public enum ResolverResult
	{
		PENDING,
		COMPLETE_HC,
		COMPLETE_UIM,
		COMPLETE_IM,
		COMPLETE_NORMAL
	}

	/**
	 * Decides whether to keep polling or stop, given the current per-endpoint
	 * state. Call this each poll after refreshing the resolved/positive flags
	 * from {@link net.runelite.client.hiscore.HiscoreManager#lookupAsync}.
	 *
	 * @param hcResolved   true once the HC-IM endpoint has returned non-null
	 * @param hcPositive   true if hcResolved AND the HC-IM result had HP > 0
	 * @param uimResolved  true once the UIM endpoint has returned non-null
	 * @param uimPositive  true if uimResolved AND the UIM result had HP > 0
	 * @param imResolved   true once the IM endpoint has returned non-null
	 * @param imPositive   true if imResolved AND the IM result had HP > 0
	 */
	public static ResolverResult resolve(
		boolean hcResolved, boolean hcPositive,
		boolean uimResolved, boolean uimPositive,
		boolean imResolved, boolean imPositive)
	{
		// Highest precedence first. HC must resolve before we can accept a UIM
		// or IM verdict — otherwise an opponent that is HC+listed on all three
		// endpoints could be classified as IM if IM resolves first.
		if (hcResolved && hcPositive) return ResolverResult.COMPLETE_HC;
		if (!hcResolved)              return ResolverResult.PENDING;

		// HC resolved negative — UIM may still elevate.
		if (uimResolved && uimPositive) return ResolverResult.COMPLETE_UIM;
		if (!uimResolved)               return ResolverResult.PENDING;

		// HC and UIM both resolved negative — IM is the last upgrade path.
		if (imResolved && imPositive) return ResolverResult.COMPLETE_IM;
		if (!imResolved)              return ResolverResult.PENDING;

		return ResolverResult.COMPLETE_NORMAL;
	}

	/**
	 * Convenience: translate a resolver verdict into the matching
	 * {@link OpponentStats.AccountType}. {@code PENDING} maps to NORMAL —
	 * callers must inspect the verdict before applying.
	 */
	public static OpponentStats.AccountType toAccountType(ResolverResult r)
	{
		switch (r)
		{
			case COMPLETE_HC:  return OpponentStats.AccountType.HARDCORE_IRONMAN;
			case COMPLETE_UIM: return OpponentStats.AccountType.ULTIMATE_IRONMAN;
			case COMPLETE_IM:  return OpponentStats.AccountType.IRONMAN;
			default:           return OpponentStats.AccountType.NORMAL;
		}
	}
}
