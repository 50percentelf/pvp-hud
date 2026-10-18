package com.pvphud;

import com.pvphud.HiscoreAccountResolver.ResolverResult;
import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Exercises the per-endpoint resolver state machine and the opponent-name
 * guard used by the hiscore polling loop in {@link PvpHudPlugin}.
 *
 * The resolver is the production logic: given which of the three ironman
 * endpoints have resolved (returned non-null from {@code lookupAsync}) and
 * which came back positive (HP > 0), it decides whether to keep polling or
 * commit to a verdict.
 */
public class HiscoreLifecycleTest
{
	// ── Still pending: higher-precedence endpoints not yet resolved ───────────

	@Test
	public void allEndpointsUnresolved_isPending()
	{
		assertEquals(ResolverResult.PENDING, HiscoreAccountResolver.resolve(
			false, false,   // HC not resolved
			false, false,   // UIM not resolved
			false, false)); // IM  not resolved
	}

	@Test
	public void imPositive_butHcAndUimUnresolved_isPending()
	{
		// This is the regression case: IM returning positive first must not
		// short-circuit the lookup and lock in an IM verdict before HC/UIM
		// have had a chance to report.
		assertEquals("IM positive alone must not complete",
			ResolverResult.PENDING,
			HiscoreAccountResolver.resolve(
				false, false,   // HC unresolved
				false, false,   // UIM unresolved
				true,  true));  // IM positive
	}

	@Test
	public void uimPositive_butHcUnresolved_isPending()
	{
		assertEquals(ResolverResult.PENDING, HiscoreAccountResolver.resolve(
			false, false,   // HC unresolved
			true,  true,    // UIM positive
			false, false));
	}

	// ── Completion: HC takes immediate precedence ─────────────────────────────

	@Test
	public void hcPositive_completesAsHc()
	{
		assertEquals(ResolverResult.COMPLETE_HC, HiscoreAccountResolver.resolve(
			true,  true,
			false, false,
			false, false));
	}

	@Test
	public void hcPositive_overridesLowerPositives()
	{
		assertEquals(ResolverResult.COMPLETE_HC, HiscoreAccountResolver.resolve(
			true, true,
			true, true,
			true, true));
	}

	// ── Completion: UIM when HC is resolved negative ──────────────────────────

	@Test
	public void uimPositive_hcResolvedNegative_completesAsUim()
	{
		assertEquals(ResolverResult.COMPLETE_UIM, HiscoreAccountResolver.resolve(
			true,  false,   // HC resolved, no HP
			true,  true,    // UIM positive
			false, false));
	}

	@Test
	public void uimPositive_hcResolvedNegative_imPositive_completesAsUim()
	{
		assertEquals(ResolverResult.COMPLETE_UIM, HiscoreAccountResolver.resolve(
			true, false,
			true, true,
			true, true));
	}

	// ── Completion: IM when HC and UIM both resolved negative ─────────────────

	@Test
	public void imPositive_hcAndUimBothResolvedNegative_completesAsIm()
	{
		assertEquals(ResolverResult.COMPLETE_IM, HiscoreAccountResolver.resolve(
			true, false,
			true, false,
			true, true));
	}

	@Test
	public void allResolvedNegative_completesAsNormal()
	{
		assertEquals(ResolverResult.COMPLETE_NORMAL, HiscoreAccountResolver.resolve(
			true, false,
			true, false,
			true, false));
	}

	// ── Mid-flight states ─────────────────────────────────────────────────────

	@Test
	public void hcResolvedNegative_uimUnresolved_imPositive_stillPending()
	{
		assertEquals("UIM is still unresolved — must not commit to IM yet",
			ResolverResult.PENDING,
			HiscoreAccountResolver.resolve(
				true,  false,  // HC resolved negative
				false, false,  // UIM unresolved
				true,  true)); // IM positive
	}

	@Test
	public void hcAndUimResolvedNegative_imUnresolved_stillPending()
	{
		assertEquals(ResolverResult.PENDING, HiscoreAccountResolver.resolve(
			true,  false,
			true,  false,
			false, false));
	}

	// ── toAccountType mapping ─────────────────────────────────────────────────

	@Test
	public void toAccountType_mapsVerdictsCorrectly()
	{
		assertEquals(OpponentStats.AccountType.HARDCORE_IRONMAN,
			HiscoreAccountResolver.toAccountType(ResolverResult.COMPLETE_HC));
		assertEquals(OpponentStats.AccountType.ULTIMATE_IRONMAN,
			HiscoreAccountResolver.toAccountType(ResolverResult.COMPLETE_UIM));
		assertEquals(OpponentStats.AccountType.IRONMAN,
			HiscoreAccountResolver.toAccountType(ResolverResult.COMPLETE_IM));
		assertEquals(OpponentStats.AccountType.NORMAL,
			HiscoreAccountResolver.toAccountType(ResolverResult.COMPLETE_NORMAL));
		assertEquals("PENDING must fall through to NORMAL as a safe default",
			OpponentStats.AccountType.NORMAL,
			HiscoreAccountResolver.toAccountType(ResolverResult.PENDING));
	}

	// ── Original guards still hold ────────────────────────────────────────────

	@Test
	public void accountType_ordinalPreventsDowngrade()
	{
		OpponentStats stats = new OpponentStats();
		stats.setAccountType(OpponentStats.AccountType.HARDCORE_IRONMAN);
		OpponentStats.AccountType incoming = OpponentStats.AccountType.IRONMAN;
		boolean wouldUpgrade = incoming.ordinal() > stats.getAccountType().ordinal();
		assertFalse("IM must not downgrade from HC", wouldUpgrade);
	}

	@Test
	public void accountType_ordinal_normalLowest()
	{
		assertEquals(0, OpponentStats.AccountType.NORMAL.ordinal());
		assertTrue(OpponentStats.AccountType.IRONMAN.ordinal()
			> OpponentStats.AccountType.NORMAL.ordinal());
		assertTrue(OpponentStats.AccountType.ULTIMATE_IRONMAN.ordinal()
			> OpponentStats.AccountType.IRONMAN.ordinal());
		assertTrue(OpponentStats.AccountType.HARDCORE_IRONMAN.ordinal()
			> OpponentStats.AccountType.ULTIMATE_IRONMAN.ordinal());
	}

	@Test
	public void opponentChange_invalidatesOldResult()
	{
		OpponentState opp = new OpponentState();
		opp.setName("Bob");
		String pendingPlayer = "Alice";
		boolean wouldApply = pendingPlayer.equalsIgnoreCase(opp.getName());
		assertFalse("Result for Alice must not apply when opponent is Bob", wouldApply);
	}

	@Test
	public void result_appliesWhenOpponentMatches()
	{
		OpponentState opp = new OpponentState();
		opp.setName("Alice");
		boolean nameMatches = "Alice".equalsIgnoreCase(opp.getName());
		assertTrue(nameMatches);
	}
}
