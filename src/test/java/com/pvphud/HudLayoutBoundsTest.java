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
	public void horiz_allPanelsFitWithinBounds()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b);

		assertWithin(layout.getOpponentPanel(), b, "opponent");
		assertWithin(layout.getEventPanel(),    b, "event");
		assertWithin(layout.getSelfPanel(),     b, "self");
		assertWithin(layout.getActionStrip(),   b, "actionStrip");
		assertNull("boost row removed in this release", layout.getBoostRow());
	}

	@Test
	public void horiz_mainPanelsDoNotOverlapHorizontally()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b);

		assertNoHorizOverlap(layout.getOpponentPanel(), layout.getEventPanel(), "opponent/event");
		assertNoHorizOverlap(layout.getEventPanel(),    layout.getSelfPanel(),  "event/self");
	}

	@Test
	public void horiz_verticalStackingNoOverlap()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b);

		// Action strip sits below main panels (no boost row anymore)
		assertNoVertOverlap(layout.getOpponentPanel(), layout.getActionStrip(), "opponent/strip");
	}

	@Test
	public void horiz_stripBottomEqualsOuterBoundsBottom()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b);

		Rectangle strip = layout.getActionStrip();
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, strip.y + strip.height);
	}

	@Test
	public void horiz_mainPanelHeightsAreEqual()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 519, 143);
		PvpHudOverlay.computeHorizLayout(layout, b);

		assertEquals("opp.height == event.height",
			layout.getOpponentPanel().height, layout.getEventPanel().height);
		assertEquals("opp.height == self.height",
			layout.getOpponentPanel().height, layout.getSelfPanel().height);
	}

	@Test
	public void horiz_smallBounds_noNegativeSizes()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 90, 60);
		PvpHudOverlay.computeHorizLayout(layout, b);

		assertNonNegativeSize(layout.getOpponentPanel(), "opponent");
		assertNonNegativeSize(layout.getEventPanel(),    "event");
		assertNonNegativeSize(layout.getSelfPanel(),     "self");
		assertNonNegativeSize(layout.getActionStrip(),   "actionStrip");
	}

	@Test
	public void horiz_nonZeroOrigin_allCoordinatesOffsetCorrectly()
	{
		HudLayoutState at0   = new HudLayoutState();
		HudLayoutState at100 = new HudLayoutState();
		Rectangle b0   = new Rectangle(0,   0,   519, 143);
		Rectangle b100 = new Rectangle(100, 200, 519, 143);
		PvpHudOverlay.computeHorizLayout(at0,   b0);
		PvpHudOverlay.computeHorizLayout(at100, b100);

		assertEquals("opponent x offset",
			at0.getOpponentPanel().x + 100, at100.getOpponentPanel().x);
		assertEquals("strip y offset",
			at0.getActionStrip().y + 200, at100.getActionStrip().y);
	}

	// ── VERTICAL layout — 220 × 400 ───────────────────────────────────────────

	@Test
	public void vert_allPanelsFitWithinBounds()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b);

		assertWithin(layout.getOpponentPanel(), b, "opponent");
		assertWithin(layout.getEventPanel(),    b, "event");
		assertWithin(layout.getSelfPanel(),     b, "self");
		assertWithin(layout.getActionStrip(),   b, "actionStrip");
		assertNull("boost row removed in this release", layout.getBoostRow());
	}

	@Test
	public void vert_verticalStackingNoOverlap()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b);

		assertNoVertOverlap(layout.getOpponentPanel(), layout.getEventPanel(),  "opponent/event");
		assertNoVertOverlap(layout.getEventPanel(),    layout.getSelfPanel(),   "event/self");
		assertNoVertOverlap(layout.getSelfPanel(),     layout.getActionStrip(), "self/strip");
	}

	@Test
	public void vert_stripBottomEqualsOuterBoundsBottom()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b);

		Rectangle strip = layout.getActionStrip();
		assertEquals("strip fills to bounds bottom",
			b.y + b.height, strip.y + strip.height);
	}

	@Test
	public void vert_selfPanelLargerThanOppOrEvent_reducedSections()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b);

		assertTrue("self panel taller than opponent (33% reduction applied)",
			layout.getSelfPanel().height > layout.getOpponentPanel().height);
		assertTrue("self panel taller than event (33% reduction applied)",
			layout.getSelfPanel().height > layout.getEventPanel().height);
	}

	@Test
	public void vert_smallBounds_noNegativeSizes()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 90, 60);
		PvpHudOverlay.computeVertLayout(layout, b);

		assertNonNegativeSize(layout.getOpponentPanel(), "opponent");
		assertNonNegativeSize(layout.getEventPanel(),    "event");
		assertNonNegativeSize(layout.getSelfPanel(),     "self");
		assertNonNegativeSize(layout.getActionStrip(),   "actionStrip");
	}

	// ── allPanelsFullWidth invariant ─────────────────────────────────────────

	@Test
	public void vert_allPanelsSpanFullWidth()
	{
		HudLayoutState layout = new HudLayoutState();
		Rectangle b = new Rectangle(0, 0, 220, 400);
		PvpHudOverlay.computeVertLayout(layout, b);

		assertEquals("opponent spans full width", b.width, layout.getOpponentPanel().width);
		assertEquals("event spans full width",    b.width, layout.getEventPanel().width);
		assertEquals("self spans full width",     b.width, layout.getSelfPanel().width);
		assertEquals("strip spans full width",    b.width, layout.getActionStrip().width);
	}
}
