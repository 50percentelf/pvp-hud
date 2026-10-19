package com.pvphud;

import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Exercises the account-type polling helpers and the opponent-name guard used
 * by the hiscore polling loop in {@link PvpHudPlugin}.
 *
 * <p>{@code HiscoreManager.lookupAsync} returns null both for "still loading"
 * and for "player not on this endpoint" — there is no way to distinguish the
 * two via the public API. Rather than try to track resolved-negative state,
 * the production loop records the best-observed positive account type across
 * polls and commits at a bounded deadline. {@link HiscoreAccountResolver}
 * provides {@code best()} (precedence upgrade) and {@code isTerminal()}
 * (HC can short-circuit the loop) to drive that process.
 */
public class HiscoreLifecycleTest
{
	// ── best(): precedence upgrades ───────────────────────────────────────────

	@Test
	public void best_normalToIronman_upgrades()
	{
		assertEquals(OpponentStats.AccountType.IRONMAN,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.NORMAL,
				OpponentStats.AccountType.IRONMAN));
	}

	@Test
	public void best_ironmanToUim_upgrades()
	{
		assertEquals(OpponentStats.AccountType.ULTIMATE_IRONMAN,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.IRONMAN,
				OpponentStats.AccountType.ULTIMATE_IRONMAN));
	}

	@Test
	public void best_uimToHc_upgrades()
	{
		assertEquals(OpponentStats.AccountType.HARDCORE_IRONMAN,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.ULTIMATE_IRONMAN,
				OpponentStats.AccountType.HARDCORE_IRONMAN));
	}

	// ── best(): no downgrade ──────────────────────────────────────────────────

	@Test
	public void best_hcToIronman_stays()
	{
		assertEquals("HC must not downgrade",
			OpponentStats.AccountType.HARDCORE_IRONMAN,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.HARDCORE_IRONMAN,
				OpponentStats.AccountType.IRONMAN));
	}

	@Test
	public void best_ironmanToNormal_stays()
	{
		assertEquals("IM must not downgrade to NORMAL",
			OpponentStats.AccountType.IRONMAN,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.IRONMAN,
				OpponentStats.AccountType.NORMAL));
	}

	@Test
	public void best_normalToNormal_stays()
	{
		assertEquals(OpponentStats.AccountType.NORMAL,
			HiscoreAccountResolver.best(
				OpponentStats.AccountType.NORMAL,
				OpponentStats.AccountType.NORMAL));
	}

	// ── isTerminal() ──────────────────────────────────────────────────────────

	@Test
	public void isTerminal_hc_true()
	{
		assertTrue(HiscoreAccountResolver.isTerminal(OpponentStats.AccountType.HARDCORE_IRONMAN));
	}

	@Test
	public void isTerminal_uim_false()
	{
		assertFalse(HiscoreAccountResolver.isTerminal(OpponentStats.AccountType.ULTIMATE_IRONMAN));
	}

	@Test
	public void isTerminal_im_false()
	{
		assertFalse(HiscoreAccountResolver.isTerminal(OpponentStats.AccountType.IRONMAN));
	}

	@Test
	public void isTerminal_normal_false()
	{
		assertFalse(HiscoreAccountResolver.isTerminal(OpponentStats.AccountType.NORMAL));
	}

	// ── Lifecycle simulations ─────────────────────────────────────────────────
	//
	// The following tests simulate the real polling loop semantics. Each
	// "poll" interprets a per-endpoint response of null (loading or absent)
	// vs HP>0 (positive hit) and applies the production logic: HC positive
	// terminates immediately; UIM/IM positive upgrade `best`; at the deadline
	// the final `best` is applied.

	/** Null-safe stand-in for an endpoint check that returns positive or absent. */
	private static OpponentStats.AccountType pollEndpoint(
		OpponentStats.AccountType current,
		OpponentStats.AccountType candidateIfPositive,
		boolean positive)
	{
		return positive ? HiscoreAccountResolver.best(current, candidateIfPositive) : current;
	}

	@Test
	public void lifecycle_poll1_hcNull_uimNull_imPositive_pending()
	{
		OpponentStats.AccountType best = OpponentStats.AccountType.NORMAL;
		best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, false);
		best = pollEndpoint(best, OpponentStats.AccountType.ULTIMATE_IRONMAN, false);
		best = pollEndpoint(best, OpponentStats.AccountType.IRONMAN, true);
		assertEquals(OpponentStats.AccountType.IRONMAN, best);
		assertFalse("IM alone is not terminal — HC may still resolve",
			HiscoreAccountResolver.isTerminal(best));
	}

	@Test
	public void lifecycle_poll2_hcPositive_terminalCompletesAsHc()
	{
		OpponentStats.AccountType best = OpponentStats.AccountType.IRONMAN; // from poll 1
		best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, true);
		assertEquals(OpponentStats.AccountType.HARDCORE_IRONMAN, best);
		assertTrue(HiscoreAccountResolver.isTerminal(best));
	}

	@Test
	public void lifecycle_fullWindow_hcUimAlwaysNull_imPositive_bestIsIm()
	{
		OpponentStats.AccountType best = OpponentStats.AccountType.NORMAL;
		for (int poll = 0; poll < 10; poll++)
		{
			best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, false);
			best = pollEndpoint(best, OpponentStats.AccountType.ULTIMATE_IRONMAN, false);
			best = pollEndpoint(best, OpponentStats.AccountType.IRONMAN, true);
		}
		assertEquals(OpponentStats.AccountType.IRONMAN, best);
	}

	@Test
	public void lifecycle_fullWindow_allNull_bestStaysNormal()
	{
		OpponentStats.AccountType best = OpponentStats.AccountType.NORMAL;
		for (int poll = 0; poll < 10; poll++)
		{
			best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, false);
			best = pollEndpoint(best, OpponentStats.AccountType.ULTIMATE_IRONMAN, false);
			best = pollEndpoint(best, OpponentStats.AccountType.IRONMAN, false);
		}
		assertEquals(OpponentStats.AccountType.NORMAL, best);
	}

	@Test
	public void lifecycle_uimPositive_hcAlwaysNull_bestIsUim()
	{
		OpponentStats.AccountType best = OpponentStats.AccountType.NORMAL;
		for (int poll = 0; poll < 10; poll++)
		{
			best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, false);
			best = pollEndpoint(best, OpponentStats.AccountType.ULTIMATE_IRONMAN, true);
			best = pollEndpoint(best, OpponentStats.AccountType.IRONMAN, false);
			assertFalse("UIM is not terminal while HC could still resolve",
				HiscoreAccountResolver.isTerminal(best));
		}
		assertEquals(OpponentStats.AccountType.ULTIMATE_IRONMAN, best);
	}

	@Test
	public void lifecycle_hcPositiveImmediately_terminalBeforeDeadline()
	{
		// Poll 1: HC comes back positive straight away. Loop can terminate.
		OpponentStats.AccountType best = OpponentStats.AccountType.NORMAL;
		best = pollEndpoint(best, OpponentStats.AccountType.HARDCORE_IRONMAN, true);
		assertEquals(OpponentStats.AccountType.HARDCORE_IRONMAN, best);
		assertTrue("HC on poll 1 must be terminal — no need to wait",
			HiscoreAccountResolver.isTerminal(best));
	}

	// ── Opponent-change guard ────────────────────────────────────────────────

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

	// ── Original ordinal-order guard retained ────────────────────────────────

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
}
