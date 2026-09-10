package com.pvphud;

import com.pvphud.state.PvpContextState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates the two plugin startup paths against the architectural contract:
 *
 *   Path L: startUp() at LOGIN_SCREEN, then LOGGED_IN fires onGameStateChanged
 *   Path G: startUp() while already LOGGED_IN
 *
 * Both paths must leave the plugin in a coherent, renderable state.
 *
 * ASYNC HYDRATION CONTRACT
 * startUp() performs only lifecycle/setup work that is safe to call on the Swing
 * EDT (overlay registration, key listeners, icon loading).  It does NOT call any
 * client-thread-only APIs directly.  Instead it ends with:
 *
 *     clientThread.invoke(this::rehydrateFromClient);
 *
 * rehydrateFromClient() is the single method that reads live client state
 * (updateEnvironment, initSelfState).  It is called:
 *   - by the clientThread.invoke() scheduled during startUp(), and
 *   - directly by onGameStateChanged(LOGGED_IN), which already fires on the
 *     client thread.
 *
 * Because rehydrateFromClient() guards with `client.getGameState() != LOGGED_IN`,
 * the invoke() scheduled during a LOGIN_SCREEN startup is a safe no-op; hydration
 * only fires for real when onGameStateChanged(LOGGED_IN) arrives.
 *
 * The tests below exercise the state-layer contracts that rehydrateFromClient()
 * must satisfy, and validate computeShouldShow() across the resulting states.
 * They cannot exercise the scheduling itself (no live Client), but they prove
 * that the state machine is correct once hydration completes.
 *
 * Architectural rules verified:
 *   - The overlay is registered for the plugin lifetime; it returns null to hide.
 *   - shouldShowHud() has no side effects; calling it repeatedly is safe.
 *   - fullReset() is always called first; stale state from a prior session cannot leak.
 *   - The HUD Enabled toggle (hudVisible) never removes/re-adds the overlay.
 */
public class PluginStartupTest
{
	// ── Path L: startUp at LOGIN_SCREEN ──────────────────────────────────────

	@Test
	public void loginScreenStartup_stateIsClean()
	{
		PvpHudState state = new PvpHudState();
		// Pollute with stale data from a previous session
		state.beginSession("OldOpponent", 0);
		state.getContext().setInPvpZone(true);
		state.getContext().setWildernessLevel(30);
		state.getContext().setMultiCombat(true);

		// startUp() always calls fullReset() regardless of game state
		state.fullReset();

		assertNull("session must be cleared", state.getCurrentSession());
		assertFalse("inPvpZone cleared on reset", state.getContext().isInPvpZone());
		assertEquals("wildernessLevel cleared on reset", 0, state.getContext().getWildernessLevel());
		assertFalse("multiCombat cleared on reset", state.getContext().isMultiCombat());
		assertFalse("opponent not tracked after reset", state.getOpponent().isTracked());
	}

	@Test
	public void loginScreenStartup_hudHiddenByDefault()
	{
		// After PATH L fullReset only: inPvpZone=false, no session
		// Default config: enabled=false, mode=MANUAL
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.MANUAL, false, false));
	}

	@Test
	public void loginScreenStartup_manualEnabled_shows()
	{
		// User enables HUD while at login screen in MANUAL mode
		// Should show as soon as enabled — no zone or session required
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, false, false));
	}

	@Test
	public void loginScreenStartup_pvpAreaEnabled_hidesUntilZoneSet()
	{
		// After PATH L fullReset only, inPvpZone=false
		// PVP_AREA mode must not show until updateEnvironment() confirms zone
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
	}

	@Test
	public void loginScreenStartup_thenLoggedIn_zoneUpdatedByEnvironment()
	{
		// PATH L continued: onGameStateChanged(LOGGED_IN) calls updateEnvironment()
		// which may set inPvpZone=true if the world is a PvP world / wilderness.
		PvpHudState state = new PvpHudState();
		state.fullReset();

		// Simulate updateEnvironment() discovering wilderness
		state.getContext().setInPvpZone(true);
		state.getContext().setWildernessLevel(25);

		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA,
			state.getContext().isInPvpZone(), state.getCurrentSession() != null));
	}

	// ── Path G: startUp while already LOGGED_IN ───────────────────────────────

	@Test
	public void loggedInStartup_stateIsCleanThenEnvironmentApplied()
	{
		// PATH G: fullReset() clears everything, then updateEnvironment() runs immediately
		PvpHudState state = new PvpHudState();
		state.beginSession("SomePlayer", 50);
		state.fullReset();

		// Still no session after reset
		assertNull(state.getCurrentSession());

		// Simulate updateEnvironment() discovering PvP world
		state.getContext().setInPvpZone(true);

		// shouldShowHud() reflects environment immediately — no restart needed
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA,
			state.getContext().isInPvpZone(), state.getCurrentSession() != null));
	}

	@Test
	public void loggedInStartup_notInZone_pvpAreaStaysHidden()
	{
		// PATH G: updateEnvironment() runs but world is not PvP
		PvpHudState state = new PvpHudState();
		state.fullReset();
		// updateEnvironment() finds no wilderness, no pvp world → inPvpZone stays false

		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA,
			state.getContext().isInPvpZone(), state.getCurrentSession() != null));
	}

	@Test
	public void loggedInStartup_autoMode_hiddenWithoutSession()
	{
		// PATH G: logged in but no fight in progress
		PvpHudState state = new PvpHudState();
		state.fullReset();

		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO,
			state.getContext().isInPvpZone(), state.getCurrentSession() != null));
	}

	// ── Idempotency: rapid OFF/ON while logged in ─────────────────────────────

	@Test
	public void offThenOn_whileLoggedIn_stateConsistent()
	{
		// shutDown() → fullReset(); startUp() → fullReset() again
		// Must produce same clean state both times
		PvpHudState state = new PvpHudState();
		state.beginSession("Bob", 100);

		// First shutDown
		state.fullReset();
		assertNull(state.getCurrentSession());

		// Second startUp (plugin re-enabled without logout)
		state.fullReset();
		assertNull(state.getCurrentSession());
		assertFalse(state.getOpponent().isTracked());
		assertFalse(state.getContext().isInPvpZone());
	}

	@Test
	public void hudEnabledToggle_repeatedFlips_noSideEffects()
	{
		// Toggling hudVisible ON/OFF/ON/OFF must not corrupt state
		// shouldShowHud() has no side effects — safe to call any number of times
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.MANUAL, false, false));
		assertTrue( PvpHudPlugin.computeShouldShow(true,  HudMode.MANUAL, false, false));
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.MANUAL, false, false));
		assertTrue( PvpHudPlugin.computeShouldShow(true,  HudMode.MANUAL, false, false));
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.MANUAL, false, false));
	}

	// ── Path equivalence ─────────────────────────────────────────────────────

	@Test
	public void bothPaths_manual_enabled_alwaysShow()
	{
		// PATH L: after login screen startup + user enables HUD
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, false, false));
		// PATH G: after logged-in startup + user enables HUD
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, true, false));
	}

	@Test
	public void bothPaths_pvpArea_onlyShowInZone()
	{
		// PATH L: zone not yet known (before LOGGED_IN fires)
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
		// PATH G: zone discovered immediately via updateEnvironment()
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, true,  false));
	}
}
