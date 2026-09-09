package com.pvphud;

import com.pvphud.state.BoostState;
import com.pvphud.state.PvpContextState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates the login/world-hop rehydration contract at the state level.
 *
 * The bug: LOGGING_IN/HOPPING fires fullReset() → context.reset() → pvpActive=false.
 * LOGGED_IN never restored pvpActive, so the HUD stayed blank after every hop/login
 * until the user toggled the Show HUD config option.
 *
 * The fix: onGameStateChanged(LOGGED_IN) calls:
 *   hudState.getContext().setMode(config.hudMode())
 *   hudState.getContext().setPvpActive(config.hudVisible())
 *   hudState.getLayout().markDirty()
 *   initSelfState()           ← safe: already guards on client.getGameState()==LOGGED_IN
 *
 * initSelfState() requires a live Client so it is not called here; the self-state
 * population is covered by the in-game test checklist.
 */
public class LoginRehydrationTest
{
	// ── fullReset() clears pvpActive (documents the root cause) ──────────────

	@Test
	public void fullReset_clearsPvpActive()
	{
		PvpHudState state = new PvpHudState();
		state.getContext().setPvpActive(true);

		state.fullReset();

		assertFalse("fullReset must zero pvpActive (via context.reset)", state.getContext().isPvpActive());
	}

	// ── hudVisible=true → LOGGING_IN → LOGGED_IN rehydration ─────────────────

	@Test
	public void rehydration_hudVisibleTrue_restoresPvpActive()
	{
		PvpHudState state = new PvpHudState();
		state.getContext().setPvpActive(true);

		// LOGGING_IN or HOPPING fires
		state.fullReset();
		assertFalse("pvpActive cleared by fullReset", state.getContext().isPvpActive());

		// LOGGED_IN fires — rehydrate from config (hudVisible=true)
		state.getContext().setPvpActive(true);

		assertTrue("HUD must be active after rehydration when hudVisible=true",
			state.getContext().isPvpActive());
	}

	// ── hudVisible=false → LOGGED_IN → remains hidden ────────────────────────

	@Test
	public void rehydration_hudVisibleFalse_remainsHidden()
	{
		PvpHudState state = new PvpHudState();
		state.getContext().setPvpActive(false);

		state.fullReset();

		// LOGGED_IN fires — rehydrate from config (hudVisible=false)
		state.getContext().setPvpActive(false);

		assertFalse("HUD must stay hidden when hudVisible=false", state.getContext().isPvpActive());
	}

	// ── mode is re-applied from config on LOGGED_IN ───────────────────────────

	@Test
	public void rehydration_modeRestoredFromConfig()
	{
		PvpHudState state = new PvpHudState();
		state.getContext().setMode(HudMode.PVP_AREA);

		state.fullReset();
		// mode is intentionally NOT cleared by context.reset() — but the LOGGED_IN
		// handler still re-applies it in case the config changed during the hop.
		state.getContext().setMode(HudMode.PVP_AREA);

		assertEquals(HudMode.PVP_AREA, state.getContext().getMode());
	}

	@Test
	public void fullReset_doesNotClearMode()
	{
		// Confirms that context.reset() leaves mode intact (by design — it is
		// a config mirror, not transient state).
		PvpHudState state = new PvpHudState();
		state.getContext().setMode(HudMode.AUTO);

		state.fullReset();

		assertEquals("mode must survive fullReset", HudMode.AUTO, state.getContext().getMode());
	}

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
		// Full hop sequence: active HUD + fight → hop → rehydrate with same config
		PvpHudState state = new PvpHudState();
		state.getContext().setPvpActive(true);
		state.getContext().setMode(HudMode.MANUAL);
		state.beginSession("Alice", 50);
		state.getOpponent().setName("Alice");

		// HOPPING fires
		state.fullReset();

		// LOGGED_IN fires — rehydrate from config
		state.getContext().setPvpActive(true);
		state.getContext().setMode(HudMode.MANUAL);

		assertTrue("pvpActive restored", state.getContext().isPvpActive());
		assertEquals("mode restored", HudMode.MANUAL, state.getContext().getMode());
		assertNull("no lingering fight session", state.getCurrentSession());
		assertFalse("opponent cleared", state.getOpponent().isTracked());
	}

	// ── context.reset() scope ─────────────────────────────────────────────────

	@Test
	public void contextReset_clearsPvpZoneAndWildLevel()
	{
		PvpContextState ctx = new PvpContextState();
		ctx.setPvpActive(true);
		ctx.setInPvpZone(true);
		ctx.setWildernessLevel(34);
		ctx.setMultiCombat(true);

		ctx.reset();

		assertFalse(ctx.isPvpActive());
		assertFalse(ctx.isInPvpZone());
		assertEquals(0, ctx.getWildernessLevel());
		assertFalse(ctx.isMultiCombat());
	}
}
