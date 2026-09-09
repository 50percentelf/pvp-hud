package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the single HUD visibility predicate: PvpHudPlugin.computeShouldShow().
 *
 * Truth table:
 *   enabled=false              → false always
 *   enabled=true, MANUAL       → true always
 *   enabled=true, PVP_AREA     → true iff inPvpZone
 *   enabled=true, AUTO         → true iff hasSession
 */
public class HudVisibilityTest
{
	// ── Disabled flag gates all modes ─────────────────────────────────────────

	@Test
	public void disabled_manual_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.MANUAL, false, false));
	}

	@Test
	public void disabled_pvpArea_inZone_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.PVP_AREA, true, false));
	}

	@Test
	public void disabled_auto_hasSession_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(false, HudMode.AUTO, false, true));
	}

	// ── MANUAL mode ───────────────────────────────────────────────────────────

	@Test
	public void manual_enabled_noZone_noSession_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, false, false));
	}

	@Test
	public void manual_enabled_inZone_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, true, false));
	}

	@Test
	public void manual_enabled_hasSession_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, false, true));
	}

	// ── PVP_AREA mode ─────────────────────────────────────────────────────────

	@Test
	public void pvpArea_enabled_inZone_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, true, false));
	}

	@Test
	public void pvpArea_enabled_notInZone_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
	}

	@Test
	public void pvpArea_enabled_inZone_sessionIrrelevant_shows()
	{
		// hasSession does not affect PVP_AREA
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, true, true));
	}

	@Test
	public void pvpArea_enabled_notInZone_sessionCannotOverride_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, true));
	}

	// ── AUTO mode ─────────────────────────────────────────────────────────────

	@Test
	public void auto_enabled_hasSession_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, true));
	}

	@Test
	public void auto_enabled_noSession_hides()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, false));
	}

	@Test
	public void auto_enabled_inZone_noSession_hides()
	{
		// zone alone does not trigger AUTO
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, true, false));
	}

	@Test
	public void auto_enabled_inZone_hasSession_shows()
	{
		assertTrue(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, true, true));
	}

	// ── Mode switching re-evaluates immediately ───────────────────────────────

	@Test
	public void modeSwitchAutoToManual_showsWithoutSession()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO,   false, false));
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL, false, false));
	}

	@Test
	public void modeSwitchManualToPvpArea_hidesOutsideZone()
	{
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.MANUAL,   false, false));
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
	}

	// ── Session lifecycle (AUTO mode) ─────────────────────────────────────────

	@Test
	public void auto_sessionStart_revealsHud()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, false));
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, true));
	}

	@Test
	public void auto_sessionEnd_hidesHud()
	{
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, true));
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.AUTO, false, false));
	}

	// ── World hop zone change (PVP_AREA mode) ─────────────────────────────────

	@Test
	public void pvpArea_hopToNormalWorld_hides()
	{
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, true,  false));
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
	}

	@Test
	public void pvpArea_hopToPvpWorld_shows()
	{
		assertFalse(PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, false, false));
		assertTrue( PvpHudPlugin.computeShouldShow(true, HudMode.PVP_AREA, true,  false));
	}
}
