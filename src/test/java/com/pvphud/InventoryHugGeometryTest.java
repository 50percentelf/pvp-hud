package com.pvphud;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the pure geometry of the INVENTORY_HUG L-shape overlay.
 *
 * These tests exercise {@link PvpHudOverlay#computeHug} with controlled
 * Rectangle inputs, verifying the anchor, dimensions, and tab-row offset
 * without requiring a running RuneLite client.
 *
 * Assumed constants (matching PvpHudOverlay):
 *   INVY_LEFT_W = 54 px  — width of the left arm
 *   INVY_TOP_H  = 50 px  — height of the drawn top arm
 */
public class InventoryHugGeometryTest
{
	private static final int LEFT_W = 54;
	private static final int TOP_H  = 50;

	// Typical fixed-mode inventory bounds (items only, no tab row)
	private static final Rectangle ITEMS_FIXED = new Rectangle(574, 230, 190, 261);

	// When the parent widget contains the tab row (38 px above the items)
	private static final Rectangle PANE_WITH_TABS = new Rectangle(574, 192, 190, 299);

	// ── Tab-row detection ────────────────────────────────────────────────────

	@Test
	public void tabH_isZeroWhenPaneEqualItems()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, ITEMS_FIXED, LEFT_W, TOP_H);
		assertEquals(0, h.tabH);
	}

	@Test
	public void tabH_matchesDistanceBetweenPaneTopAndItemsTop()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		// pane.y=192, items.y=230 → tabH=38
		assertEquals(38, h.tabH);
	}

	// ── Overlay dimensions ───────────────────────────────────────────────────

	@Test
	public void totalWidth_isLeftRailPlusPaneWidth()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		assertEquals(LEFT_W + PANE_WITH_TABS.width, h.totalW);  // 244
	}

	@Test
	public void totalHeight_isTopArmPlusPaneHeight()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		assertEquals(TOP_H + PANE_WITH_TABS.height, h.totalH);  // 349
	}

	@Test
	public void totalWidth_noTabRow()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, ITEMS_FIXED, LEFT_W, TOP_H);
		assertEquals(LEFT_W + ITEMS_FIXED.width, h.totalW);     // 244
	}

	@Test
	public void totalHeight_noTabRow()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, ITEMS_FIXED, LEFT_W, TOP_H);
		assertEquals(TOP_H + ITEMS_FIXED.height, h.totalH);     // 311
	}

	// ── Anchor position ──────────────────────────────────────────────────────

	@Test
	public void anchor_isOutsideTopLeftOfPane()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		Point expected = new Point(PANE_WITH_TABS.x - LEFT_W, PANE_WITH_TABS.y - TOP_H);
		assertEquals(expected, h.anchor);  // (520, 142)
	}

	@Test
	public void anchor_noTabRow_anchoredAboveItems()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, ITEMS_FIXED, LEFT_W, TOP_H);
		Point expected = new Point(ITEMS_FIXED.x - LEFT_W, ITEMS_FIXED.y - TOP_H);
		assertEquals(expected, h.anchor);  // (520, 180)
	}

	// ── paneH and itemsH ────────────────────────────────────────────────────

	@Test
	public void paneH_matchesPaneHeight()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		assertEquals(PANE_WITH_TABS.height, h.paneH);  // 299
	}

	@Test
	public void itemsH_matchesItemGridHeight()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		assertEquals(ITEMS_FIXED.height, h.itemsH);     // 261
	}

	// ── Invariant: tabH + itemsH == paneH ───────────────────────────────────

	@Test
	public void tabPlusItemsEqualsPane()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, LEFT_W, TOP_H);
		assertEquals(h.paneH, h.tabH + h.itemsH);
	}

	@Test
	public void tabPlusItemsEqualsPane_noTabRow()
	{
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, ITEMS_FIXED, LEFT_W, TOP_H);
		assertEquals(h.paneH, h.tabH + h.itemsH);
	}

	// ── Different rail sizes are respected ───────────────────────────────────

	@Test
	public void customRailSizes()
	{
		int leftW = 60;
		int topH  = 70;
		PvpHudOverlay.HugGeometry h = PvpHudOverlay.computeHug(ITEMS_FIXED, PANE_WITH_TABS, leftW, topH);
		assertEquals(leftW + PANE_WITH_TABS.width, h.totalW);
		assertEquals(topH  + PANE_WITH_TABS.height, h.totalH);
		assertEquals(PANE_WITH_TABS.x - leftW, h.anchor.x);
		assertEquals(PANE_WITH_TABS.y - topH,  h.anchor.y);
	}
}
