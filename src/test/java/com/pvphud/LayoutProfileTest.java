package com.pvphud;

import net.runelite.client.config.Keybind;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies that {@link LayoutProfile#forLayout} reads from the correct
 * per-layout config methods and that profiles are fully independent.
 */
public class LayoutProfileTest
{
	/**
	 * Minimal PvpHudConfig that returns a unique, recognisable value for every
	 * per-layout presentation method so each profile can be fingerprinted.
	 */
	private static final PvpHudConfig CONFIG = new PvpHudConfig()
	{
		// ── Chat Locked (old key names, backward-compat) ─────────────────────────
		@Override public BuffStyle       buffStyle()       { return BuffStyle.TEXT; }
		@Override public int             backgroundOpacity(){ return 100; }
		@Override public boolean         showBoostRow()    { return false; }
		@Override public boolean         boostXOverX()     { return true; }
		@Override public BarDisplayStyle hpBarStyle()      { return BarDisplayStyle.BARS_ONLY; }
		@Override public BarDisplayStyle prayerBarStyle()  { return BarDisplayStyle.NUMBERS_ONLY; }
		@Override public BarDisplayStyle runBarStyle()     { return BarDisplayStyle.HIDDEN; }
		@Override public BarDisplayStyle specBarStyle()    { return BarDisplayStyle.BARS_AND_NUMBERS; }

		// ── Horizontal Float ─────────────────────────────────────────────────────
		@Override public BuffStyle       hfBuffStyle()                    { return BuffStyle.ICON_TRAY; }
		@Override public int             hfBackgroundOpacity()            { return 50; }
		@Override public boolean         hfShowBoostRow()                 { return true; }
		@Override public boolean         hfBoostXOverX()                  { return false; }
		@Override public boolean         hfReserveBoostDockWhenHidden()   { return true; }
		@Override public BarDisplayStyle hfHpBarStyle()                   { return BarDisplayStyle.NUMBERS_ONLY; }
		@Override public BarDisplayStyle hfPrayerBarStyle()               { return BarDisplayStyle.BARS_ONLY; }
		@Override public BarDisplayStyle hfRunBarStyle()                  { return BarDisplayStyle.BARS_AND_NUMBERS; }
		@Override public BarDisplayStyle hfSpecBarStyle()                 { return BarDisplayStyle.HIDDEN; }

		// ── Vertical Float ───────────────────────────────────────────────────────
		@Override public BuffStyle       vfBuffStyle()                    { return BuffStyle.VERTICAL_BAR; }
		@Override public int             vfBackgroundOpacity()            { return 200; }
		@Override public boolean         vfShowBoostRow()                 { return true; }
		@Override public boolean         vfBoostXOverX()                  { return false; }
		@Override public boolean         vfReserveBoostDockWhenHidden()   { return false; }
		@Override public BarDisplayStyle vfHpBarStyle()                   { return BarDisplayStyle.HIDDEN; }
		@Override public BarDisplayStyle vfPrayerBarStyle()               { return BarDisplayStyle.BARS_AND_NUMBERS; }
		@Override public BarDisplayStyle vfRunBarStyle()                  { return BarDisplayStyle.NUMBERS_ONLY; }
		@Override public BarDisplayStyle vfSpecBarStyle()                 { return BarDisplayStyle.BARS_ONLY; }

		// ── Inventory Hug ────────────────────────────────────────────────────────
		@Override public BuffStyle       ihBuffStyle()       { return BuffStyle.TEXT; }
		@Override public int             ihBackgroundOpacity(){ return 150; }
		@Override public boolean         ihShowBoostRow()    { return false; }
		@Override public boolean         ihBoostXOverX()     { return true; }
		@Override public BarDisplayStyle ihHpBarStyle()      { return BarDisplayStyle.BARS_AND_NUMBERS; }
		@Override public BarDisplayStyle ihPrayerBarStyle()  { return BarDisplayStyle.HIDDEN; }
		@Override public BarDisplayStyle ihRunBarStyle()     { return BarDisplayStyle.BARS_ONLY; }
		@Override public BarDisplayStyle ihSpecBarStyle()    { return BarDisplayStyle.NUMBERS_ONLY; }

		// ── Global (unused by LayoutProfile, defaults are fine) ──────────────────
		@Override public HudMode  hudMode()       { return HudMode.MANUAL; }
		@Override public boolean  hudVisible()    { return false; }
		@Override public HudLayout hudLayout()   { return HudLayout.CHAT_LOCKED; }
		@Override public boolean  showHpShake()  { return true; }
		@Override public boolean  showFreezeTimer()  { return true; }
		@Override public boolean  showDivineTimers() { return true; }
		@Override public Keybind  hudToggleKey() { return Keybind.NOT_SET; }
		@Override public Keybind  timer1Key()    { return Keybind.NOT_SET; }
		@Override public int      timer1Duration(){ return 300; }
		@Override public Keybind  timer2Key()    { return Keybind.NOT_SET; }
		@Override public int      timer2Duration(){ return 300; }
	};

	@Test
	public void chatLocked_readsFromChatLockedKeys()
	{
		LayoutProfile p = LayoutProfile.forLayout(HudLayout.CHAT_LOCKED, CONFIG);
		assertEquals(BuffStyle.TEXT, p.buffStyle);
		assertEquals(100, p.backgroundOpacity);
		assertFalse(p.showBoostRow);
		assertTrue(p.boostXOverX);
		assertFalse(p.reserveBoostDockWhenHidden); // not applicable
		assertEquals(BarDisplayStyle.BARS_ONLY, p.hpBarStyle);
		assertEquals(BarDisplayStyle.NUMBERS_ONLY, p.prayerBarStyle);
		assertEquals(BarDisplayStyle.HIDDEN, p.runBarStyle);
		assertEquals(BarDisplayStyle.BARS_AND_NUMBERS, p.specBarStyle);
	}

	@Test
	public void horizFloat_readsFromHfKeys()
	{
		LayoutProfile p = LayoutProfile.forLayout(HudLayout.HORIZONTAL_FLOAT, CONFIG);
		assertEquals(BuffStyle.ICON_TRAY, p.buffStyle);
		assertEquals(50, p.backgroundOpacity);
		assertTrue(p.showBoostRow);
		assertFalse(p.boostXOverX);
		assertTrue(p.reserveBoostDockWhenHidden);
		assertEquals(BarDisplayStyle.NUMBERS_ONLY, p.hpBarStyle);
		assertEquals(BarDisplayStyle.BARS_ONLY, p.prayerBarStyle);
		assertEquals(BarDisplayStyle.BARS_AND_NUMBERS, p.runBarStyle);
		assertEquals(BarDisplayStyle.HIDDEN, p.specBarStyle);
	}

	@Test
	public void vertFloat_readsFromVfKeys()
	{
		LayoutProfile p = LayoutProfile.forLayout(HudLayout.VERTICAL_FLOAT, CONFIG);
		assertEquals(BuffStyle.VERTICAL_BAR, p.buffStyle);
		assertEquals(200, p.backgroundOpacity);
		assertTrue(p.showBoostRow);
		assertFalse(p.boostXOverX);
		assertFalse(p.reserveBoostDockWhenHidden);
		assertEquals(BarDisplayStyle.HIDDEN, p.hpBarStyle);
		assertEquals(BarDisplayStyle.BARS_AND_NUMBERS, p.prayerBarStyle);
		assertEquals(BarDisplayStyle.NUMBERS_ONLY, p.runBarStyle);
		assertEquals(BarDisplayStyle.BARS_ONLY, p.specBarStyle);
	}

	@Test
	public void invyHug_readsFromIhKeys()
	{
		LayoutProfile p = LayoutProfile.forLayout(HudLayout.INVENTORY_HUG, CONFIG);
		assertEquals(BuffStyle.TEXT, p.buffStyle);
		assertEquals(150, p.backgroundOpacity);
		assertFalse(p.showBoostRow);
		assertTrue(p.boostXOverX);
		assertFalse(p.reserveBoostDockWhenHidden); // not applicable
		assertEquals(BarDisplayStyle.BARS_AND_NUMBERS, p.hpBarStyle);
		assertEquals(BarDisplayStyle.HIDDEN, p.prayerBarStyle);
		assertEquals(BarDisplayStyle.BARS_ONLY, p.runBarStyle);
		assertEquals(BarDisplayStyle.NUMBERS_ONLY, p.specBarStyle);
	}

	@Test
	public void layoutsAreIndependent_buffStyle()
	{
		LayoutProfile cl = LayoutProfile.forLayout(HudLayout.CHAT_LOCKED, CONFIG);
		LayoutProfile hf = LayoutProfile.forLayout(HudLayout.HORIZONTAL_FLOAT, CONFIG);
		LayoutProfile vf = LayoutProfile.forLayout(HudLayout.VERTICAL_FLOAT, CONFIG);

		// All three have distinct buff styles confirming no cross-contamination.
		assertNotEquals(cl.buffStyle, hf.buffStyle);
		assertNotEquals(hf.buffStyle, vf.buffStyle);
	}

	@Test
	public void layoutsAreIndependent_opacity()
	{
		LayoutProfile cl = LayoutProfile.forLayout(HudLayout.CHAT_LOCKED, CONFIG);
		LayoutProfile hf = LayoutProfile.forLayout(HudLayout.HORIZONTAL_FLOAT, CONFIG);
		LayoutProfile vf = LayoutProfile.forLayout(HudLayout.VERTICAL_FLOAT, CONFIG);
		LayoutProfile ih = LayoutProfile.forLayout(HudLayout.INVENTORY_HUG, CONFIG);

		// Every layout has a unique opacity in our test config.
		assertNotEquals(cl.backgroundOpacity, hf.backgroundOpacity);
		assertNotEquals(hf.backgroundOpacity, vf.backgroundOpacity);
		assertNotEquals(vf.backgroundOpacity, ih.backgroundOpacity);
	}

	@Test
	public void reserveBoostDock_falseForChatLockedAndInvyHug()
	{
		assertFalse(LayoutProfile.forLayout(HudLayout.CHAT_LOCKED,    CONFIG).reserveBoostDockWhenHidden);
		assertFalse(LayoutProfile.forLayout(HudLayout.INVENTORY_HUG,  CONFIG).reserveBoostDockWhenHidden);
		// Float layouts read the real config value (which is true for HF in our stub).
		assertTrue(LayoutProfile.forLayout(HudLayout.HORIZONTAL_FLOAT, CONFIG).reserveBoostDockWhenHidden);
	}
}
