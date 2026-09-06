package com.pvphud;

import com.pvphud.state.BoostState;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.SelfState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PvpHudOverlay extends Overlay
{
	// ── Layout constants ──────────────────────────────────────────────────────
	private static final int ACTION_STRIP_H = 22;
	private static final int BOOST_ROW_H    = 18;
	private static final int PAD            = 6;
	private static final int BAR_H          = 5;

	/** Default vertical-float dimensions (freely draggable by user). */
	private static final int VERT_W = 220;
	private static final int VERT_H = 400;

	// ── Colour palette ────────────────────────────────────────────────────────
	private static final Color BG          = new Color(20,  20,  20,  230);
	private static final Color STRIP_BG    = new Color(10,  10,  10,  245);
	private static final Color DIVIDER     = new Color(75,  75,  75,  200);
	private static final Color VERT_DIV    = new Color(55,  55,  55,  160);
	private static final Color WHITE       = Color.WHITE;
	private static final Color GRAY        = new Color(155, 155, 155);
	private static final Color YELLOW      = new Color(255, 203,   5);
	private static final Color GREEN       = new Color( 10, 210,  80);
	private static final Color LIGHT_BLUE  = new Color(100, 185, 255);
	private static final Color ORANGE      = new Color(255, 145,   0);
	private static final Color RED         = new Color(220,  60,  60);
	private static final Color TOXIC_GREEN = new Color(110, 230,  60);
	private static final Color HP_FG       = new Color( 20, 185,  45);
	private static final Color HP_BG       = new Color( 75,  15,  15);

	private final Client       client;
	private final PvpHudPlugin plugin;
	private final PvpHudConfig config;

	/** Tracks last layout mode so we can mark layout dirty on a switch. */
	private HudLayout lastLayout;

	@Inject
	PvpHudOverlay(Client client, PvpHudPlugin plugin, PvpHudConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
	}

	// ── Render entry point ────────────────────────────────────────────────────

	@Override
	public Dimension render(Graphics2D g)
	{
		PvpHudState state = plugin.getHudState();
		if (!state.getContext().isPvpActive())
		{
			return null;
		}

		HudLayout currentLayout = config.hudLayout();
		HudLayoutState layout   = state.getLayout();

		if (currentLayout != lastLayout)
		{
			lastLayout = currentLayout;
			layout.markDirty();
		}

		Font normal = FontManager.getRunescapeFont();
		Font small  = FontManager.getRunescapeSmallFont();

		switch (currentLayout)
		{
			case CHAT_LOCKED:
				return renderChatLocked(g, state, layout, normal, small);
			case HORIZONTAL_FLOAT:
				return renderHorizontalFloat(g, state, layout, normal, small);
			case VERTICAL_FLOAT:
				return renderVerticalFloat(g, state, layout, normal, small);
			default:
				return null;
		}
	}

	// ── Chat-locked rendering (anchored to chatbox widget) ────────────────────

	private Dimension renderChatLocked(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle bounds = getChatboxBounds();
		if (bounds == null)
		{
			return null;
		}

		if (!bounds.equals(layout.getChatboxBounds()))
		{
			layout.setChatboxBounds(new Rectangle(bounds));
			layout.markDirty();
		}
		if (layout.isDirty())
		{
			computeHorizLayout(layout, bounds);
		}

		drawAll(g, state, layout, normal, small, false);
		return null;
	}

	// ── Horizontal-float rendering (same size as chat, freely draggable) ──────

	private Dimension renderHorizontalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle stored = layout.getChatboxBounds();
		int w = stored != null ? stored.width  : 519;
		int h = stored != null ? stored.height : 142;
		Rectangle bounds = new Rectangle(0, 0, w, h);

		if (layout.isDirty())
		{
			computeHorizLayout(layout, bounds);
		}

		drawAll(g, state, layout, normal, small, false);
		return new Dimension(w, h);
	}

	// ── Vertical-float rendering (narrow sidebar, freely draggable) ───────────

	private Dimension renderVerticalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle bounds = new Rectangle(0, 0, VERT_W, VERT_H);

		if (layout.isDirty())
		{
			computeVertLayout(layout, bounds);
		}

		drawAll(g, state, layout, normal, small, true);
		return new Dimension(VERT_W, VERT_H);
	}

	// ── Layout computation ────────────────────────────────────────────────────

	private void computeHorizLayout(HudLayoutState layout, Rectangle b)
	{
		int mainH = b.height - ACTION_STRIP_H - BOOST_ROW_H;
		int third = b.width / 3;
		int right = b.width - third;

		layout.setOpponentPanel(new Rectangle(b.x,         b.y,         third,         mainH));
		layout.setEventPanel   (new Rectangle(b.x + third, b.y,         right - third, mainH));
		layout.setSelfPanel    (new Rectangle(b.x + right, b.y,         third,         mainH));
		layout.setBoostRow     (new Rectangle(b.x,         b.y + mainH, b.width,       BOOST_ROW_H));
		layout.setActionStrip  (new Rectangle(b.x,         b.y + mainH + BOOST_ROW_H, b.width, ACTION_STRIP_H));
		layout.setDirty(false);
	}

	private void computeVertLayout(HudLayoutState layout, Rectangle b)
	{
		int mainH = b.height - ACTION_STRIP_H - BOOST_ROW_H;
		int secH  = mainH / 3;

		layout.setOpponentPanel(new Rectangle(b.x, b.y,              b.width, secH));
		layout.setEventPanel   (new Rectangle(b.x, b.y + secH,       b.width, secH));
		layout.setSelfPanel    (new Rectangle(b.x, b.y + 2 * secH,   b.width, mainH - 2 * secH));
		layout.setBoostRow     (new Rectangle(b.x, b.y + mainH,      b.width, BOOST_ROW_H));
		layout.setActionStrip  (new Rectangle(b.x, b.y + mainH + BOOST_ROW_H, b.width, ACTION_STRIP_H));
		layout.setDirty(false);
	}

	// ── Draw everything ───────────────────────────────────────────────────────

	private void drawAll(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small, boolean vertical)
	{
		Rectangle strip    = layout.getActionStrip();
		Rectangle boostRow = layout.getBoostRow();
		Rectangle opp      = layout.getOpponentPanel();
		Rectangle ev       = layout.getEventPanel();

		// background + chrome
		if (opp != null)
		{
			g.setColor(BG);
			Rectangle full = new Rectangle(
				opp.x, opp.y,
				opp.width + (ev != null ? ev.width : 0) + (layout.getSelfPanel() != null ? layout.getSelfPanel().width : 0),
				opp.height + BOOST_ROW_H + ACTION_STRIP_H);
			g.fillRect(full.x, full.y, full.width, full.height);
		}

		if (strip != null)
		{
			g.setColor(STRIP_BG);
			g.fillRect(strip.x, strip.y, strip.width, strip.height);
		}

		if (boostRow != null)
		{
			g.setColor(DIVIDER);
			g.drawLine(boostRow.x, boostRow.y, boostRow.x + boostRow.width, boostRow.y);
		}
		if (strip != null)
		{
			g.setColor(DIVIDER);
			g.drawLine(strip.x, strip.y, strip.x + strip.width, strip.y);
		}

		if (vertical)
		{
			// Horizontal dividers between stacked panels
			if (ev != null)
			{
				g.setColor(DIVIDER);
				g.drawLine(ev.x + PAD, ev.y, ev.x + ev.width - PAD, ev.y);
			}
			Rectangle self = layout.getSelfPanel();
			if (self != null)
			{
				g.setColor(DIVIDER);
				g.drawLine(self.x + PAD, self.y, self.x + self.width - PAD, self.y);
			}
		}
		else
		{
			// Vertical dividers between columns
			if (opp != null)
			{
				int vx = opp.x + opp.width;
				g.setColor(VERT_DIV);
				g.drawLine(vx, opp.y + PAD, vx, opp.y + opp.height - PAD);
			}
			if (ev != null)
			{
				int vx = ev.x + ev.width;
				g.setColor(VERT_DIV);
				g.drawLine(vx, ev.y + PAD, vx, ev.y + ev.height - PAD);
			}
		}

		drawOpponentPanel(g, layout, normal, small);
		drawEventPanel(g, layout, normal, small);
		drawSelfPanel(g, state, layout, normal, small);
		drawBoostRow(g, state, layout, small);
		drawActionStrip(g, state, layout, small);
	}

	// ── Opponent panel ────────────────────────────────────────────────────────

	private void drawOpponentPanel(Graphics2D g, HudLayoutState layout, Font normal, Font small)
	{
		Rectangle p = layout.getOpponentPanel();
		if (p == null) return;

		int x  = p.x + PAD;
		int w  = p.width - PAD * 2;
		int cy = p.y + PAD;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		g.drawString("OPPONENT", x, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(WHITE);
		g.drawString("Marcbob", x, cy + fm.getAscent());
		cy += fm.getHeight() + 4;

		g.setColor(HP_BG);
		g.fillRect(x, cy, w, BAR_H);
		g.setColor(HP_FG);
		g.fillRect(x, cy, (int) (w * (43f / 99f)), BAR_H);
		cy += BAR_H + 3;

		g.setFont(small);
		smFm = g.getFontMetrics();
		g.setColor(GRAY);
		g.drawString("~43 HP", x, cy + smFm.getAscent());

		g.setColor(YELLOW);
		g.drawString("OPP VENG", x, p.y + p.height - PAD - smFm.getDescent());
	}

	// ── Event panel ───────────────────────────────────────────────────────────

	private void drawEventPanel(Graphics2D g, HudLayoutState layout, Font normal, Font small)
	{
		Rectangle p = layout.getEventPanel();
		if (p == null) return;

		int cx = p.x + p.width / 2;
		int cy = p.y + PAD;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawCentered(g, smFm, "FIGHT", cx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 6;

		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(YELLOW);
		drawCentered(g, fm, "* CHANCE *", cx, cy + fm.getAscent());
		cy += fm.getHeight() + 3;

		g.setColor(GREEN);
		drawCentered(g, fm, "<- 46", cx, cy + fm.getAscent());
		cy += fm.getHeight() + 3;

		g.setFont(small);
		smFm = g.getFontMetrics();
		g.setColor(ORANGE);
		drawCentered(g, smFm, "PR -11", cx, cy + smFm.getAscent());
	}

	// ── Self panel ────────────────────────────────────────────────────────────

	private void drawSelfPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
	{
		Rectangle p = layout.getSelfPanel();
		if (p == null) return;

		int rx = p.x + p.width - PAD;
		int cy = p.y + PAD;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawRightAligned(g, smFm, "YOU", rx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();

		SelfState   self = state.getSelf();
		EffectState fx   = state.getEffects();

		if (self.isVengActive())
		{
			g.setColor(GREEN);
			drawRightAligned(g, fm, "VENG RDY", rx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int freezeTicks = self.getFreezeTicksRemaining();
		if (freezeTicks > 0)
		{
			g.setColor(LIGHT_BLUE);
			drawRightAligned(g, fm, "ICE " + freezeTicks + "t", rx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int tbTicks = self.getTeleBlockTicksRemaining();
		if (tbTicks > 0)
		{
			int s = tbTicks * 600 / 1000;
			g.setColor(ORANGE);
			drawRightAligned(g, fm, "TB " + (s / 60) + ":" + String.format("%02d", s % 60),
				rx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		// Divine potion timers
		cy = drawDivineTimer(g, fm, "DSC", fx.getDivineSupercombatTicks(), rx, cy);
		cy = drawDivineTimer(g, fm, "DRG", fx.getDivineRangingTicks(),     rx, cy);
		cy = drawDivineTimer(g, fm, "DMG", fx.getDivineMagicTicks(),       rx, cy);
		cy = drawDivineTimer(g, fm, "BAS", fx.getDivineBastionTicks(),     rx, cy);
		cy = drawDivineTimer(g, fm, "BTM", fx.getDivineBattlemageTicks(),  rx, cy);
		cy = drawDivineTimer(g, fm, "MEN", fx.getMenaphiteRemedyTicks(),   rx, cy);

		if (self.isVenomed())
		{
			g.setColor(TOXIC_GREEN);
			drawRightAligned(g, fm, "VENOM", rx, cy + fm.getAscent());
		}
		else if (self.isPoisoned())
		{
			g.setColor(TOXIC_GREEN);
			drawRightAligned(g, fm, "POISON", rx, cy + fm.getAscent());
		}
	}

	/** Draws one divine-pot timer right-aligned. Returns the next cy or unchanged cy if ticks == 0. */
	private int drawDivineTimer(Graphics2D g, FontMetrics fm, String label, int ticks, int rx, int cy)
	{
		if (ticks <= 0) return cy;
		int s = ticks * 600 / 1000;
		String text = label + " " + (s / 60) + ":" + String.format("%02d", s % 60);
		g.setColor(s > 60 ? GREEN : s > 30 ? YELLOW : RED);
		drawRightAligned(g, fm, text, rx, cy + fm.getAscent());
		return cy + fm.getHeight() + 1;
	}

	// ── Boost row ─────────────────────────────────────────────────────────────

	private void drawBoostRow(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getBoostRow();
		if (p == null) return;

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		int col = p.width / 5;
		int cx1 = p.x + col / 2;
		int cx2 = cx1 + col;
		int cx3 = cx2 + col;
		int cx4 = cx3 + col;
		int cx5 = cx4 + col;

		BoostState boosts = state.getBoosts();
		drawBoostLabel(g, fm, "ATK", boosts.getAttackDelta(),   cx1, baseline);
		drawBoostLabel(g, fm, "STR", boosts.getStrengthDelta(), cx2, baseline);
		drawBoostLabel(g, fm, "DEF", boosts.getDefenceDelta(),  cx3, baseline);
		drawBoostLabel(g, fm, "RNG", boosts.getRangedDelta(),   cx4, baseline);
		drawBoostLabel(g, fm, "MAG", boosts.getMagicDelta(),    cx5, baseline);
	}

	private void drawBoostLabel(Graphics2D g, FontMetrics fm, String prefix, int delta, int cx, int baseline)
	{
		String label = prefix + (delta >= 0 ? "+" : "") + delta;
		g.setColor(delta > 0 ? GREEN : delta < 0 ? RED : GRAY);
		drawCentered(g, fm, label, cx, baseline);
	}

	// ── Action strip ──────────────────────────────────────────────────────────

	private void drawActionStrip(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getActionStrip();
		if (p == null) return;

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		int spec = state.getEffects().getSpecEnergy();
		String[] labels = {"ATK 2t", "EAT", "POT", "SPEC " + spec, "5tx7", "T1 4:31", "SGL"};
		Color[]  colors = {YELLOW,   GREEN, GREEN, WHITE,            GRAY,   WHITE,     GRAY};

		int usable = p.width - PAD * 2;
		int count  = labels.length;
		while (count > 1)
		{
			int total = 0;
			for (int i = 0; i < count; i++) total += fm.stringWidth(labels[i]);
			if (total + (count - 1) * 6 <= usable) break;
			count--;
		}

		int textW = 0;
		for (int i = 0; i < count; i++) textW += fm.stringWidth(labels[i]);

		int gaps = count - 1;
		int gapW = gaps > 0 ? (usable - textW) / gaps : 0;
		int sepX = gapW / 2;

		int x = p.x + PAD;
		for (int i = 0; i < count; i++)
		{
			int itemW = fm.stringWidth(labels[i]);
			if (i > 0)
			{
				int sx = x - gapW + sepX;
				g.setColor(DIVIDER);
				g.drawLine(sx, p.y + 4, sx, p.y + p.height - 4);
			}
			g.setColor(colors[i]);
			g.drawString(labels[i], x, baseline);
			x += itemW + gapW;
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void drawCentered(Graphics2D g, FontMetrics fm, String text, int cx, int baseline)
	{
		g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
	}

	private void drawRightAligned(Graphics2D g, FontMetrics fm, String text, int rx, int baseline)
	{
		g.drawString(text, rx - fm.stringWidth(text), baseline);
	}

	private Rectangle getChatboxBounds()
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (chatbox == null || chatbox.isHidden())
		{
			return null;
		}
		return chatbox.getBounds();
	}
}
