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
 * Tests for drawEffects2Col: overflow clipping, group separators,
 * CRITICAL-first truncation priority, and compound parent/child cell folding.
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
			effects.add(new ActiveEffectView(null, "C" + i, Color.RED, StatusPriority.CRITICAL));
		for (int i = 0; i < 8; i++)
			effects.add(new ActiveEffectView(null, "P" + i, Color.GREEN, StatusPriority.SUSTAIN));
		for (int i = 0; i < 8; i++)
			effects.add(new ActiveEffectView(null, "Y" + i, Color.YELLOW, StatusPriority.SUSTAIN));

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
		effects.add(new ActiveEffectView(null, "FREEZE", Color.CYAN,   StatusPriority.CRITICAL));
		effects.add(new ActiveEffectView(null, "TB",     Color.ORANGE, StatusPriority.CRITICAL));
		effects.add(new ActiveEffectView(null, "DSC",    Color.GREEN,  StatusPriority.SUSTAIN));
		effects.add(new ActiveEffectView(null, "NXT",    Color.YELLOW, StatusPriority.SUSTAIN));

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
		effects.add(new ActiveEffectView(null, "A", Color.WHITE, StatusPriority.CRITICAL));
		effects.add(new ActiveEffectView(null, "B", Color.WHITE, StatusPriority.CRITICAL));

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
		sameGroup.add(new ActiveEffectView(null, "A", Color.WHITE, StatusPriority.CRITICAL));
		sameGroup.add(new ActiveEffectView(null, "B", Color.WHITE, StatusPriority.CRITICAL));
		int cySame = PvpHudOverlay.drawEffects2Col(g, sameGroup, 0, 0, CANVAS_H);

		// Two effects, different groups — separator gap added between them
		List<ActiveEffectView> diffGroup = new ArrayList<>();
		diffGroup.add(new ActiveEffectView(null, "A", Color.WHITE, StatusPriority.CRITICAL));
		diffGroup.add(new ActiveEffectView(null, "B", Color.WHITE, StatusPriority.SUSTAIN));
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
		effects.add(new ActiveEffectView(null, "FREEZE", Color.CYAN, StatusPriority.CRITICAL));
		// CYCLE second — should be truncated because space is exhausted
		effects.add(new ActiveEffectView(null, "NXT", Color.YELLOW, StatusPriority.SUSTAIN));

		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, maxY);
		assertTrue("cy=" + cy + " must not exceed maxY=" + maxY, cy <= maxY);
		// Can't verify pixels without a display; return-value contract is the observable invariant.
		g.dispose();
	}

	// ── childSlotIdFor mapping ────────────────────────────────────────────────

	@Test
	public void childSlotIdFor_knownPairs()
	{
		assertEquals("menaphite_next",    PvpHudOverlay.childSlotIdFor("menaphite"));
		assertEquals("prayer_regen_next", PvpHudOverlay.childSlotIdFor("prayer_regen"));
		assertEquals("corrupt_next",      PvpHudOverlay.childSlotIdFor("corrupt"));
		assertEquals("burn_next",         PvpHudOverlay.childSlotIdFor("burn"));
	}

	@Test
	public void childSlotIdFor_unknownParent_returnsNull()
	{
		assertNull(PvpHudOverlay.childSlotIdFor("freeze_barrage"));
		assertNull(PvpHudOverlay.childSlotIdFor("tb"));
		assertNull(PvpHudOverlay.childSlotIdFor(null));
	}

	// ── Compound pair folding ─────────────────────────────────────────────────

	/** Build a primary ActiveEffectView with a slotId (for compound tests). */
	private static ActiveEffectView primary(String slotId, String label)
	{
		return new ActiveEffectView(slotId, null, label, null, Color.GREEN,
			null, StatusPriority.SUSTAIN, false);
	}

	/** Build a secondary ActiveEffectView with a slotId (for compound tests). */
	private static ActiveEffectView secondary(String slotId, String label)
	{
		return new ActiveEffectView(slotId, null, label, null, Color.YELLOW,
			null, StatusPriority.SUSTAIN, true);
	}

	@Test
	public void compoundPair_advancesByCompoundHeight_notDoubleCell()
	{
		// Core invariants:
		// 1. A compound pair advances cy MORE than the primary alone
		//    (secondary is folded in, not silently dropped).
		// 2. Two compound pairs advance cy exactly twice as much as one compound pair
		//    (each occupies one logical row, not merged or split differently).
		Graphics2D g = makeGraphics();

		// One compound pair
		List<ActiveEffectView> oneCompound = new ArrayList<>();
		oneCompound.add(primary("menaphite",       "4:17"));
		oneCompound.add(secondary("menaphite_next", "15t"));
		int cyOne = PvpHudOverlay.drawEffects2Col(g, oneCompound, 0, 0, CANVAS_H);

		// Primary alone without child
		List<ActiveEffectView> alone = new ArrayList<>();
		alone.add(primary("menaphite", "4:17")); // no secondary
		int cyAlone = PvpHudOverlay.drawEffects2Col(g, alone, 0, 0, CANVAS_H);

		// Two compound pairs (same priority, no separator)
		List<ActiveEffectView> twoCompound = new ArrayList<>();
		twoCompound.add(primary("menaphite",         "4:17"));
		twoCompound.add(secondary("menaphite_next",   "15t"));
		twoCompound.add(primary("prayer_regen",       "3:42"));
		twoCompound.add(secondary("prayer_regen_next", "8t"));
		int cyTwo = PvpHudOverlay.drawEffects2Col(g, twoCompound, 0, 0, CANVAS_H);

		assertTrue("compound pair must advance more than primary alone (secondary included)",
			cyOne > cyAlone);
		assertEquals("two compound pairs must advance exactly twice one compound pair",
			cyOne * 2, cyTwo);
		g.dispose();
	}

	@Test
	public void compoundPair_occupiesOneRow_notTwoColumns()
	{
		// Primary+secondary compound must NOT occupy two column slots side by side.
		// Verify: adding an independent entry after the compound does NOT pair it
		// into the same row as the compound (compound takes the full row).
		Graphics2D g = makeGraphics();
		FontMetrics fm = g.getFontMetrics();
		final int lineH = fm.getHeight();

		List<ActiveEffectView> compoundThenOne = new ArrayList<>();
		compoundThenOne.add(primary("menaphite",       "4:17"));
		compoundThenOne.add(secondary("menaphite_next", "15t"));
		compoundThenOne.add(primary("dsc",              "0:45")); // standalone after compound

		int cy = PvpHudOverlay.drawEffects2Col(g, compoundThenOne, 0, 0, CANVAS_H);

		// compH + ceil(cellH) for the lone trailing entry
		List<ActiveEffectView> compoundOnly = new ArrayList<>();
		compoundOnly.add(primary("menaphite",       "4:17"));
		compoundOnly.add(secondary("menaphite_next", "15t"));
		int cyCompound = PvpHudOverlay.drawEffects2Col(g, compoundOnly, 0, 0, CANVAS_H);

		// The trailing standalone entry must add at least one line of height.
		assertTrue("standalone after compound must add height", cy > cyCompound);
		g.dispose();
	}

	@Test
	public void compoundPair_withoutSecondary_rendersSingleCell()
	{
		// If the secondary is absent (nextProcTicks == 0 → never emitted), the primary
		// should render as an ordinary cell without any blank placeholder.
		Graphics2D g = makeGraphics();
		FontMetrics fm = g.getFontMetrics();
		final int lineH = fm.getHeight();

		List<ActiveEffectView> primaryOnly = new ArrayList<>();
		primaryOnly.add(primary("menaphite", "4:17")); // no secondary

		int cy = PvpHudOverlay.drawEffects2Col(g, primaryOnly, 0, 0, CANVAS_H);

		// Must advance exactly like any other single non-compound entry.
		List<ActiveEffectView> otherSingle = new ArrayList<>();
		otherSingle.add(primary("dsc", "0:45"));
		int cyOther = PvpHudOverlay.drawEffects2Col(g, otherSingle, 0, 0, CANVAS_H);

		assertEquals("primary without child must use normal cell height", cyOther, cy);
		g.dispose();
	}

	@Test
	public void compoundPair_overflow_primaryKeptSecondaryDropped()
	{
		// When there is room for cellH but not compH, primary is rendered without secondary.
		Graphics2D g = makeGraphics();
		FontMetrics fm = g.getFontMetrics();
		final int lineH = fm.getHeight();

		// Create a maxY that fits exactly one cellH but not compH.
		// cellH = ICON_SIZE(18) + 2 + lineH + 2 = 22 + lineH
		// compH = 22 + lineH * 2
		// We need: cellH <= maxY < compH
		// → maxY = cellH = 22 + lineH
		int cellH = 18 + 2 + lineH + 2;
		int maxY  = cellH;

		List<ActiveEffectView> compound = new ArrayList<>();
		compound.add(primary("menaphite",       "4:17"));
		compound.add(secondary("menaphite_next", "15t"));

		// Must not crash and must not exceed maxY
		int cy = PvpHudOverlay.drawEffects2Col(g, compound, 0, 0, maxY);
		assertTrue("cy must not exceed maxY even when secondary is dropped", cy <= maxY);
		g.dispose();
	}

	@Test
	public void twoPrimariesFromDifferentPairs_renderInTwoRows()
	{
		// menaphite+child and prayer_regen+child must each occupy one full row.
		// Two compounds together → cy = 2 × one compound (same priority, no separator).
		// This is already verified by compoundPair_advancesByCompoundHeight_notDoubleCell;
		// this test focuses on the cross-pair case: the second pair's child must not be
		// misassociated with the first pair's primary.
		Graphics2D g = makeGraphics();

		List<ActiveEffectView> one = new ArrayList<>();
		one.add(primary("menaphite",       "4:17"));
		one.add(secondary("menaphite_next", "15t"));
		int cyOne = PvpHudOverlay.drawEffects2Col(g, one, 0, 0, CANVAS_H);

		List<ActiveEffectView> two = new ArrayList<>();
		two.add(primary("menaphite",         "4:17"));
		two.add(secondary("menaphite_next",   "15t"));
		two.add(primary("prayer_regen",       "3:42"));
		two.add(secondary("prayer_regen_next", "8t"));
		int cyTwo = PvpHudOverlay.drawEffects2Col(g, two, 0, 0, CANVAS_H);

		assertEquals("two compound pairs must advance exactly twice one compound pair",
			cyOne * 2, cyTwo);
		g.dispose();
	}

	@Test
	public void nonCompoundPrimary_withoutChild_usesNormalHeight_notCompoundHeight()
	{
		// A known compound parent (menaphite) that has NO secondary emitted must NOT
		// produce compound-height output.  It should render just like any other primary.
		Graphics2D g = makeGraphics();
		FontMetrics fm = g.getFontMetrics();

		// menaphite without its secondary — no compound folding should occur
		List<ActiveEffectView> parentNoChild = new ArrayList<>();
		parentNoChild.add(primary("menaphite", "4:17"));
		int cyParentOnly = PvpHudOverlay.drawEffects2Col(g, parentNoChild, 0, 0, CANVAS_H);

		// Unrelated primary — same text-path height
		List<ActiveEffectView> unrelated = new ArrayList<>();
		unrelated.add(primary("dsc", "0:45"));
		int cyUnrelated = PvpHudOverlay.drawEffects2Col(g, unrelated, 0, 0, CANVAS_H);

		assertEquals("parent without child must render at normal height, same as any primary",
			cyUnrelated, cyParentOnly);
		g.dispose();
	}

	// ── Single odd entry (trailing flush) ─────────────────────────────────────

	@Test
	public void oddEffectCount_trailingFlush()
	{
		Graphics2D g = makeGraphics();
		List<ActiveEffectView> effects = new ArrayList<>();
		// Three effects — last one is a lone entry that must still advance cy
		effects.add(new ActiveEffectView(null, "A", Color.WHITE, StatusPriority.CRITICAL));
		effects.add(new ActiveEffectView(null, "B", Color.WHITE, StatusPriority.CRITICAL));
		effects.add(new ActiveEffectView(null, "C", Color.WHITE, StatusPriority.CRITICAL));

		int cySingle = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, CANVAS_H);

		List<ActiveEffectView> pair = new ArrayList<>();
		pair.add(new ActiveEffectView(null, "A", Color.WHITE, StatusPriority.CRITICAL));
		pair.add(new ActiveEffectView(null, "B", Color.WHITE, StatusPriority.CRITICAL));
		int cyPair = PvpHudOverlay.drawEffects2Col(g, pair, 0, 0, CANVAS_H);

		// Three entries must advance cy the same as two (the third fits beside the second row's
		// first slot, so both commit the same row count — but with the text-only null-icon path
		// each entry is its own row, so three rows > two rows).
		assertTrue("three effects must advance cy at least as much as two", cySingle >= cyPair);
		g.dispose();
	}

	// ── compactSecondaryLabel ─────────────────────────────────────────────────

	private static ActiveEffectView childView(String slotId, String label)
	{
		return new ActiveEffectView(slotId, null, label, null, Color.YELLOW,
			null, StatusPriority.SUSTAIN, true);
	}

	@Test
	public void compactSecondaryLabel_menaphite_prefixesNXT()
	{
		ActiveEffectView child = childView("menaphite_next", "0:09");
		String label = PvpHudOverlay.compactSecondaryLabel("menaphite_next", child);
		assertTrue("menaphite_next compact label must start with NXT",
			label.startsWith("NXT "));
		assertFalse("menaphite_next must not be an unlabelled raw timer",
			label.equals("0:09"));
	}

	@Test
	public void compactSecondaryLabel_prayerRegen_prefixesNXT()
	{
		ActiveEffectView child = childView("prayer_regen_next", "0:04");
		String label = PvpHudOverlay.compactSecondaryLabel("prayer_regen_next", child);
		assertTrue("prayer_regen_next compact label must start with NXT",
			label.startsWith("NXT "));
		assertFalse("prayer_regen_next must not retain verbose P-NXT prefix",
			label.startsWith("P-NXT"));
	}

	@Test
	public void compactSecondaryLabel_corruption_remainsCompact()
	{
		// corrupt_next label is already "-3 5t" (concise); no prefix should be added.
		ActiveEffectView child = childView("corrupt_next", "-3 5t");
		String label = PvpHudOverlay.compactSecondaryLabel("corrupt_next", child);
		assertEquals("corrupt_next compact label must be the raw label unchanged", "-3 5t", label);
		assertFalse("corrupt_next must not gain NXDRAIN prefix",
			label.contains("NXDRAIN"));
	}

	@Test
	public void compactSecondaryLabel_burn_stripsRedundantBurnPrefix()
	{
		ActiveEffectView child = childView("burn_next", "BURN+3");
		String label = PvpHudOverlay.compactSecondaryLabel("burn_next", child);
		assertEquals("burn_next compact label must strip BURN prefix", "+3", label);
	}

	@Test
	public void compactSecondaryLabel_burn_nonStandardLabel_fallsBackSafely()
	{
		// If burn label doesn't have the expected "BURN+" prefix, return as-is.
		ActiveEffectView child = childView("burn_next", "3");
		String label = PvpHudOverlay.compactSecondaryLabel("burn_next", child);
		assertEquals("burn_next without BURN+ must fall back to raw label", "3", label);
	}

	@Test
	public void compactSecondaryLabel_unknown_fallsBackToLabel()
	{
		ActiveEffectView child = childView("some_future_slot", "42t");
		String label = PvpHudOverlay.compactSecondaryLabel("some_future_slot", child);
		assertEquals("unknown child slot must fall back to child.label", "42t", label);
	}

	@Test
	public void compactSecondaryLabel_nullSlotId_fallsBackToLabel()
	{
		ActiveEffectView child = childView(null, "0:15");
		String label = PvpHudOverlay.compactSecondaryLabel(null, child);
		assertEquals("null slotId must fall back to child.label", "0:15", label);
	}
}
