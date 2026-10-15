package com.pvphud;

import com.pvphud.state.HudLayoutState;
import java.awt.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies layout geometry for HORIZONTAL and VERTICAL HUD layouts.
 *
 * computeHorizLayout / computeVertLayout are package-private statics, callable
 * without a RuneLite client.  Tests cover:
 *   - every panel fits inside the outer bounds
 *   - no two panels overlap
 *   - action strip is the very last row (bottom of bounds)
 *   - total height exactly equals the outer bounds height
 *   - no panel has negative width or height
 */
public class HudLayoutBoundsTest
{
	// ── Helpers ───────────────────────────────────────────────────────────────

	private static void assertWithin(Rectangle panel, Rectangle bounds, String name)
	{
		assertTrue(name + ".x >= bounds.x",
			panel.x >= bounds.x);
		assertTrue(name + ".y >= bounds.y",
			panel.y >= bounds.y);
		assertTrue(name + ".right <= bounds.right",
			panel.x + panel.width  <= bounds.x + bounds.width);
		assertTrue(name + ".bottom <= bounds.bottom",
			panel.y + panel.height <= bounds.y + bounds.height);
	}

	private static void assertNoVertOverlap(Rectangle upper, Rectangle lower, String msg)
	{
		assertTrue(msg + ": upper.bottom=" + (upper.y + upper.height) + " must be <= lower.top=" + lower.y,
			upper.y + upper.height <= lower.y);
	}

	private static void assertNoHorizOverlap(Rectangle left, Rectangle right, String msg)
	{
		assertTrue(msg + ": left.right=" + (left.x + left.width) + " must be <= right.left=" + right.x,
			left.x + left.width <= right.x);
	}

	private static void assertNonNegativeSize(Rectangle r, String name)
	{
		if (r == null) return;
		assertTrue(name + ".width >= 0", r.width  >= 0);
		assertTrue(name + ".height >= 0", r.height >= 0);
	}

	// ── HORIZONTAL layout — chatbox-sized (519 × 143) ─────────────────────────

	@Test
	public void horiz_withBoostRow_allPanelsFitWithinBounds()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		assertWithin(layout.getOpponentPanel(), b, "opponent");
		assertWithin(layout.getEventPanel(),    b, "event");
		assertWithin(layout.getSelfPanel(),     b, "self");
		assertWithin(layout.getBoostRow(),      b, "boostRow");
		assertWithin(layout.getActionStrip(),   b, "actionStrip");
	}

	@Test
	public void horiz_withBoostRow_mainPanelsDoNotOverlapHorizontally()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		assertNoHorizOverlap(layout.getOpponentPanel(), layout.getEventPanel(), "opponent/event");
		assertNoHorizOverlap(layout.getEventPanel(),    layout.getSelfPanel(),  "event/self");
	}

	@Test
	public void horiz_withBoostRow_verticalStackingNoOverlap()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		// Boost row sits below main panels
		assertNoVertOverlap(layout.getOpponentPanel(), layout.getBoostRow(),    "opponent/boost");
		// Action strip sits below boost row
		assertNoVertOverlap(layout.getBoostRow(),      layout.getActionStrip(), "boost/strip");
	}

	@Test
	public void horiz_withBoostRow_stripBottomEqualsOuterBoundsBottom()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		Rectangle strip = layout.getActionStrip();
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, strip.y + strip.height);
	}

	@Test
	public void horiz_withBoostRow_mainPanelHeightsAreEqual()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		assertEquals("opp.height == event.height",
			layout.getOpponentPanel().height, layout.getEventPanel().height);
		assertEquals("opp.height == self.height",
			layout.getOpponentPanel().height, layout.getSelfPanel().height);
	}

	@Test
	public void horiz_withoutBoostRow_noBoostRectangle_stripAtCorrectPosition()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b, false, false);

		assertNull("boost row must be null when disabled", layout.getBoostRow());

		Rectangle strip = layout.getActionStrip();
		assertNotNull(strip);
		assertWithin(strip, b, "actionStrip");
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, strip.y + strip.height);
	}

	@Test
	public void horiz_withoutBoostRow_mainPanelsTallerThanWithBoostRow()
	{
		HudLayoutState with    = new HudLayoutState();
		HudLayoutState without = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(with,    b, true,  false);
		PvpHudOverlay.computeHorizLayout(without, b, false, false);

		// Without boost row the main panels are taller (boost row height is reclaimed)
		assertTrue("main panels taller without boost row",
			without.getOpponentPanel().height > with.getOpponentPanel().height);
	}

	@Test
	public void horiz_smallBounds_noNegativeSizes()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 90, 60);
		PvpHudOverlay.computeHorizLayout(layout, b, true, false);

		assertNonNegativeSize(layout.getOpponentPanel(), "opponent");
		assertNonNegativeSize(layout.getEventPanel(),    "event");
		assertNonNegativeSize(layout.getSelfPanel(),     "self");
		assertNonNegativeSize(layout.getBoostRow(),      "boostRow");
		assertNonNegativeSize(layout.getActionStrip(),   "actionStrip");
	}

	@Test
	public void horiz_nonZeroOrigin_allCoordinatesOffsetCorrectly()
	{
		HudLayoutState at0   = new HudLayoutState();
		HudLayoutState at100 = new HudLayoutState();
		Rectangle b0   = new Rectangle(0,   0,   519, 143);
		Rectangle b100 = new Rectangle(100, 200, 519, 143);
		PvpHudOverlay.computeHorizLayout(at0,   b0,   true, false);
		PvpHudOverlay.computeHorizLayout(at100, b100, true, false);

		assertEquals("opponent x offset",
			at0.getOpponentPanel().x + 100, at100.getOpponentPanel().x);
		assertEquals("strip y offset",
			at0.getActionStrip().y + 200, at100.getActionStrip().y);
	}

	// ── VERTICAL layout — 220 × 400 ───────────────────────────────────────────

	@Test
	public void vert_withBoostRow_allPanelsFitWithinBounds()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		assertWithin(layout.getOpponentPanel(), b, "opponent");
		assertWithin(layout.getEventPanel(),    b, "event");
		assertWithin(layout.getSelfPanel(),     b, "self");
		assertWithin(layout.getBoostRow(),      b, "boostRow");
		assertWithin(layout.getActionStrip(),   b, "actionStrip");
	}

	@Test
	public void vert_withBoostRow_verticalStackingNoOverlap()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		assertNoVertOverlap(layout.getOpponentPanel(), layout.getEventPanel(),  "opponent/event");
		assertNoVertOverlap(layout.getEventPanel(),    layout.getSelfPanel(),   "event/self");
		assertNoVertOverlap(layout.getSelfPanel(),     layout.getBoostRow(),    "self/boost");
		assertNoVertOverlap(layout.getBoostRow(),      layout.getActionStrip(), "boost/strip");
	}

	@Test
	public void vert_withBoostRow_stripBottomEqualsOuterBoundsBottom()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		Rectangle strip = layout.getActionStrip();
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, strip.y + strip.height);
	}

	@Test
	public void vert_selfPanelLargerThanOppOrEvent_reducedSections()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		assertTrue("self panel taller than opponent (33% reduction applied)",
			layout.getSelfPanel().height > layout.getOpponentPanel().height);
		assertTrue("self panel taller than event (33% reduction applied)",
			layout.getSelfPanel().height > layout.getEventPanel().height);
	}

	@Test
	public void vert_withoutBoostRow_noBoostRectangle()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, false, false);

		assertNull("boost row must be null when disabled", layout.getBoostRow());
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, layout.getActionStrip().y + layout.getActionStrip().height);
	}

	@Test
	public void vert_smallBounds_noNegativeSizes()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 90, 60);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		assertNonNegativeSize(layout.getOpponentPanel(), "opponent");
		assertNonNegativeSize(layout.getEventPanel(),    "event");
		assertNonNegativeSize(layout.getSelfPanel(),     "self");
		assertNonNegativeSize(layout.getBoostRow(),      "boostRow");
		assertNonNegativeSize(layout.getActionStrip(),   "actionStrip");
	}

	@Test
	public void vert_reserveDockWithHiddenBoostRow_boostRowRectStillPresent()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		// reserveDock=true means the boost slot is kept even when showBoostRow=false
		PvpHudOverlay.computeVertLayout(layout, b, false, true);

		assertNotNull("boost row rect present when reserveDock=true", layout.getBoostRow());
		assertWithin(layout.getBoostRow(),    b, "boostRow");
		assertWithin(layout.getActionStrip(), b, "actionStrip");
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, layout.getActionStrip().y + layout.getActionStrip().height);
	}

	// ── allPanelsFullWidth invariant ─────────────────────────────────────────

	@Test
	public void vert_allPanelsSpanFullWidth()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b, true, false);

		assertEquals("opponent spans full width", b.width, layout.getOpponentPanel().width);
		assertEquals("event spans full width",    b.width, layout.getEventPanel().width);
		assertEquals("self spans full width",     b.width, layout.getSelfPanel().width);
		assertEquals("boost spans full width",    b.width, layout.getBoostRow().width);
		assertEquals("strip spans full width",    b.width, layout.getActionStrip().width);
	}
}
