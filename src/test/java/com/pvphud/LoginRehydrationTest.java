package com.pvphud;

import com.pvphud.state.PvpContextState;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Regression tests for the world-hop / login rehydration path.
 *
 * fullReset() is called on logout, world hop, and plugin shutdown. These tests
 * confirm that fight state, PvP-zone flags, and wilderness level are all cleared
 * so that the first tick after login starts from a clean baseline.
 */
public class LoginRehydrationTest
{
	@Test
	public void worldHop_clearsFightSession()
	{
		PvpHudState state = new PvpHudState();
		state.beginSession("Bob", 100);
		state.getOpponent().setName("Bob");
		assertNotNull("session must exist before hop", state.getCurrentSession());

		state.fullReset();

		assertNull("session must be null after world hop", state.getCurrentSession());
		assertFalse("opponent must not be tracked after world hop",
			state.getOpponent().isTracked());
	}

	@Test
	public void worldHop_thenRehydrate_correctFinalState()
	{
		PvpHudState state = new PvpHudState();
		// Simulate: in a fight, then world-hop.
		state.beginSession("Alice", 50);
		state.getOpponent().setName("Alice");
		state.getContext().setInPvpZone(true);
		state.getContext().setWildernessLevel(20);

		state.fullReset();

		// After reset nothing from the old session should survive.
		assertNull(state.getCurrentSession());
		assertFalse(state.getOpponent().isTracked());
		assertFalse("PvP zone must be cleared after world hop",
			state.getContext().isInPvpZone());
		assertEquals("wilderness level must be 0 after world hop",
			0, state.getContext().getWildernessLevel());

		// Simulate: logged back in, game tick updates zone state.
		state.getContext().setInPvpZone(true);
		state.getContext().setWildernessLevel(15);

		assertTrue(state.getContext().isInPvpZone());
		assertEquals(15, state.getContext().getWildernessLevel());
		assertNull("no session yet — combat must be confirmed first",
			state.getCurrentSession());
	}

	@Test
	public void contextReset_clearsPvpZoneAndWildLevel()
	{
		PvpContextState ctx = new PvpContextState();
		ctx.setInPvpZone(true);
		ctx.setWildernessLevel(30);
		ctx.setMultiCombat(true);

		ctx.reset();

		assertFalse("inPvpZone must be false after reset", ctx.isInPvpZone());
		assertEquals("wildernessLevel must be 0 after reset", 0, ctx.getWildernessLevel());
		assertFalse("multiCombat must be false after reset", ctx.isMultiCombat());
	}
}
