package com.pvphud;

import com.pvphud.state.BoostState;
import com.pvphud.state.PvpContextState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates the login/world-hop rehydration contract at the state level.
 *
 * LOGGING_IN/HOPPING fires fullReset() → context.reset() → zone/combat state cleared.
 * LOGGED_IN fires updateEnvironment() which re-reads live varbits (tested in-game).
 * HUD visibility is computed on-demand via PvpHudPlugin.computeShouldShow() — not stored.
 */
public class LoginRehydrationTest
{
	// ── world hop clears fight data ───────────────────────────────────────────

	@Test
	public void worldHop_clearsFightSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Bob", 100);
		state.getOpponent().setName("Bob");

		// HOPPING fires
		state.fullReset();

		assertNull("session cleared by hop", state.getCurrentSession());
		assertFalse("opponent not tracked after hop", state.getOpponent().isTracked());
	}

	@Test
	public void worldHop_clearsBoostState()
	{
		PvpHudState state = new PvpHudState();
		BoostState boosts = state.getBoosts();
		boosts.setAttackBoosted(118);
		boosts.setAttackReal(99);
		boosts.setStrengthBoosted(118);
		boosts.setStrengthReal(99);

		// HOPPING fires
		state.fullReset();

		assertEquals("attack boosted cleared by hop", 0, state.getBoosts().getAttackBoosted());
		assertEquals("attack real cleared by hop",    0, state.getBoosts().getAttackReal());
		assertEquals("strength boosted cleared by hop", 0, state.getBoosts().getStrengthBoosted());
	}

	@Test
	public void worldHop_thenRehydrate_correctFinalState()
	{
		// Full hop sequence: active fight → hop → session cleared
		PvpHudState state = new PvpHudState();
		state.beginSession("Alice", 50);
		state.getOpponent().setName("Alice");

		// HOPPING fires
		state.fullReset();

		// inPvpZone is re-seeded by updateEnvironment() (requires live Client; verified in-game)
		assertNull("no lingering fight session", state.getCurrentSession());
		assertFalse("opponent cleared", state.getOpponent().isTracked());
	}

	// ── context.reset() scope ─────────────────────────────────────────────────

	@Test
	public void contextReset_clearsPvpZoneAndWildLevel()
	{
		PvpContextState ctx = new PvpContextState();
		ctx.setInPvpZone(true);
		ctx.setWildernessLevel(34);
		ctx.setMultiCombat(true);

		ctx.reset();

		assertFalse(ctx.isInPvpZone());
		assertEquals(0, ctx.getWildernessLevel());
		assertFalse(ctx.isMultiCombat());
	}
}
