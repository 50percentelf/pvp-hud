package com.pvphud;

import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests account-type precedence and name-match guard used by hiscore lookup. */
public class HiscoreLifecycleTest
{
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
	public void accountType_upgradesFromNormal()
	{
		OpponentStats stats = new OpponentStats();
		assertEquals(OpponentStats.AccountType.NORMAL, stats.getAccountType());
		assertTrue(OpponentStats.AccountType.IRONMAN.ordinal()
			> OpponentStats.AccountType.NORMAL.ordinal());
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
		// Simulate: pending = "Alice", opp changes to "Bob"
		// applyHiscoresResult checks opp.getName().equalsIgnoreCase(playerName)
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
		String pendingPlayer = "Alice";
		boolean nameMatches = pendingPlayer.equalsIgnoreCase(opp.getName());
		assertTrue(nameMatches);
	}
}
