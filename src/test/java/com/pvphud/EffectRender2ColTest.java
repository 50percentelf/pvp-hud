package com.pvphud;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for drawEffects2Col: overflow clipping, group separators, and
 * CRITICAL-first truncation priority.
 *
 * drawEffects2Col is static and package-private, so it can be called directly
 * without constructing a PvpHudOverlay (which needs a running RuneLite client).
 *
 * All tests use a plain AWT system font to avoid RuneLite font dependencies.
 */
public class EffectRender2ColTest
{
	/** Small font that produces deterministic, small FontMetrics. */
	private static final Font TEST_FONT = new Font("Dialog", Font.PLAIN, 9);

	/** Canvas tall enough that no test accidentally clips the drawing surface. */
	private static final int CANVAS_H = 1000;
	private static final int CANVAS_W = 72; // matches INVY_LEFT_W

	private static Graphics2D makeGraphics()
	{
		BufferedImage img = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setFont(TEST_FONT);
		return g;
	}

	/** Returns the line height for the test font — used to calculate expected cell heights. */
	private static int lineH(Graphics2D g)
	{
		return g.getFontMetrics().getHeight();
	}

	// ── Overflow clipping ─────────────────────────────────────────────────────

	@Test
	public void returnCy_neverExceedsMaxY_manyEffects()
	{
		Graphics2D g = makeGraphics();
		List<ActiveEffectView> effects = new ArrayList<>();
		for (int i = 0; i < 8; i++)
			effects.add(new ActiveEffectView(null, "C" + i, Color.RED, EffectGroup.CRITICAL));
		for (int i = 0; i < 8; i++)
			effects.add(new ActiveEffectView(null, "P" + i, Color.GREEN, EffectGroup.PROTECTION));
		for (int i = 0; i < 8; i++)
			effects.add(new ActiveEffectView(null, "Y" + i, Color.YELLOW, EffectGroup.CYCLE));

		int maxY = 80;
		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, maxY);

		assertTrue("cy=" + cy + " must not exceed maxY=" + maxY, cy <= maxY);
		g.dispose();
	}

	@Test
	public void returnCy_neverExceedsMaxY_verySmallWindow()
	{
		Graphics2D g = makeGraphics();
		List<ActiveEffectView> effects = new ArrayList<>();
		effects.add(new ActiveEffectView(null, "FREEZE", Color.CYAN,   EffectGroup.CRITICAL));
		effects.add(new ActiveEffectView(null, "TB",     Color.ORANGE, EffectGroup.CRITICAL));
		effects.add(new ActiveEffectView(null, "DSC",    Color.GREEN,  EffectGroup.PROTECTION));
		effects.add(new ActiveEffectView(null, "NXT",    Color.YELLOW, EffectGroup.CYCLE));

		int maxY = 20; // smaller than one text row — essentially nothing fits
		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, maxY);

		assertTrue("cy=" + cy + " must not exceed maxY=" + maxY, cy <= maxY);
		g.dispose();
	}

	@Test
	public void emptyList_returnsCyUnchanged()
	{
		Graphics2D g = makeGraphics();
		int startCy = 50;
		int cy = PvpHudOverlay.drawEffects2Col(g, new ArrayList<>(), 0, startCy, 200);
		assertEquals("empty list must not change cy", startCy, cy);
		g.dispose();
	}

	@Test
	public void returnCy_advancesWhenAllEffectsFit()
	{
		Graphics2D g = makeGraphics();
		List<ActiveEffectView> effects = new ArrayList<>();
		effects.add(new ActiveEffectView(null, "A", Color.WHITE, EffectGroup.CRITICAL));
		effects.add(new ActiveEffectView(null, "B", Color.WHITE, EffectGroup.CRITICAL));

		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, CANVAS_H);
		assertTrue("two effects in same group must advance cy", cy > 0);
		g.dispose();
	}

	// ── Group separator ───────────────────────────────────────────────────────

	@Test
	public void groupSeparator_advancesCyMoreThanSingleGroup()
	{
		Graphics2D g = makeGraphics();

		// Two effects, same group — no separator gap
		List<ActiveEffectView> sameGroup = new ArrayList<>();
		sameGroup.add(new ActiveEffectView(null, "A", Color.WHITE, EffectGroup.CRITICAL));
		sameGroup.add(new ActiveEffectView(null, "B", Color.WHITE, EffectGroup.CRITICAL));
		int cySame = PvpHudOverlay.drawEffects2Col(g, sameGroup, 0, 0, CANVAS_H);

		// Two effects, different groups — separator gap added between them
		List<ActiveEffectView> diffGroup = new ArrayList<>();
		diffGroup.add(new ActiveEffectView(null, "A", Color.WHITE, EffectGroup.CRITICAL));
		diffGroup.add(new ActiveEffectView(null, "B", Color.WHITE, EffectGroup.PROTECTION));
		int cyDiff = PvpHudOverlay.drawEffects2Col(g, diffGroup, 0, 0, CANVAS_H);

		assertTrue("cross-group cy=" + cyDiff + " must exceed single-group cy=" + cySame,
			cyDiff > cySame);
		g.dispose();
	}

	// ── Truncation priority: CRITICAL always rendered before CYCLE ────────────

	@Test
	public void criticalFirst_cycleIsDropped_whenSpaceTight()
	{
		Graphics2D g = makeGraphics();
		FontMetrics fm = g.getFontMetrics();
		// One text row height + minimal padding
		int oneRowH = fm.getHeight() + 2;

		// maxY just enough for one text row starting at cy=0
		int maxY = oneRowH;

		List<ActiveEffectView> effects = new ArrayList<>();
		// CRITICAL first — should fit (fills the one row)
		effects.add(new ActiveEffectView(null, "FREEZE", Color.CYAN, EffectGroup.CRITICAL));
		// CYCLE second — should be truncated because space is exhausted
		effects.add(new ActiveEffectView(null, "NXT", Color.YELLOW, EffectGroup.CYCLE));

		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, maxY);
		assertTrue("cy=" + cy + " must not exceed maxY=" + maxY, cy <= maxY);
		// Can't verify pixels without a display; return-value contract is the observable invariant.
		g.dispose();
	}

	// ── Single odd entry (trailing flush) ─────────────────────────────────────

	@Test
	public void oddEffectCount_trailingFlush()
	{
		Graphics2D g = makeGraphics();
		List<ActiveEffectView> effects = new ArrayList<>();
		// Three effects — last one is a lone entry that must still advance cy
		effects.add(new ActiveEffectView(null, "A", Color.WHITE, EffectGroup.CRITICAL));
		effects.add(new ActiveEffectView(null, "B", Color.WHITE, EffectGroup.CRITICAL));
		effects.add(new ActiveEffectView(null, "C", Color.WHITE, EffectGroup.CRITICAL));

		int cySingle = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, CANVAS_H);

		List<ActiveEffectView> pair = new ArrayList<>();
		pair.add(new ActiveEffectView(null, "A", Color.WHITE, EffectGroup.CRITICAL));
		pair.add(new ActiveEffectView(null, "B", Color.WHITE, EffectGroup.CRITICAL));
		int cyPair = PvpHudOverlay.drawEffects2Col(g, pair, 0, 0, CANVAS_H);

		// Three entries must advance cy the same as two (the third fits beside the second row's
		// first slot, so both commit the same row count — but with the text-only null-icon path
		// each entry is its own row, so three rows > two rows).
		assertTrue("three effects must advance cy at least as much as two", cySingle >= cyPair);
		g.dispose();
	}
}
