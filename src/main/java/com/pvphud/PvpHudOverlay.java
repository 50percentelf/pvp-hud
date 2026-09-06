package com.pvphud;

import com.pvphud.state.HudLayoutState;
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

	private final Client client;
	private final PvpHudPlugin plugin;

	@Inject
	PvpHudOverlay(Client client, PvpHudPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
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

		Rectangle bounds = getChatboxBounds();
		if (bounds == null)
		{
			return null;
		}

		HudLayoutState layout = state.getLayout();
		if (!bounds.equals(layout.getChatboxBounds()))
		{
			layout.setChatboxBounds(new Rectangle(bounds));
			layout.markDirty();
		}
		if (layout.isDirty())
		{
			computeLayout(layout, bounds);
		}

		Font normal = FontManager.getRunescapeFont();
		Font small  = FontManager.getRunescapeSmallFont();

		drawBackground(g, bounds, layout);
		drawOpponentPanel(g, layout, normal, small);
		drawEventPanel(g, layout, normal, small);
		drawSelfPanel(g, layout, normal, small);
		drawBoostRow(g, layout, small);
		drawActionStrip(g, layout, small);

		return null;
	}

	// ── Layout ────────────────────────────────────────────────────────────────

	private void computeLayout(HudLayoutState layout, Rectangle b)
	{
		int mainH = b.height - ACTION_STRIP_H - BOOST_ROW_H;
		int third = b.width / 3;
		int right = b.width - third;

		layout.setOpponentPanel(new Rectangle(b.x,         b.y,         third,            mainH));
		layout.setEventPanel   (new Rectangle(b.x + third, b.y,         right - third,    mainH));
		layout.setSelfPanel    (new Rectangle(b.x + right, b.y,         third,            mainH));
		layout.setBoostRow     (new Rectangle(b.x,         b.y + mainH, b.width,          BOOST_ROW_H));
		layout.setActionStrip  (new Rectangle(b.x,         b.y + mainH + BOOST_ROW_H, b.width, ACTION_STRIP_H));
		layout.setDirty(false);
	}

	// ── Background & chrome ───────────────────────────────────────────────────

	private void drawBackground(Graphics2D g, Rectangle b, HudLayoutState layout)
	{
		// Base fill
		g.setColor(BG);
		g.fillRect(b.x, b.y, b.width, b.height);

		// Darker action strip
		Rectangle strip = layout.getActionStrip();
		if (strip != null)
		{
			g.setColor(STRIP_BG);
			g.fillRect(strip.x, strip.y, strip.width, strip.height);
		}

		// Horizontal dividers
		Rectangle boostRow = layout.getBoostRow();
		if (boostRow != null)
		{
			g.setColor(DIVIDER);
			g.drawLine(b.x, boostRow.y, b.x + b.width, boostRow.y);
		}
		if (strip != null)
		{
			g.setColor(DIVIDER);
			g.drawLine(b.x, strip.y, b.x + b.width, strip.y);
		}

		// Vertical panel dividers
		Rectangle opp = layout.getOpponentPanel();
		Rectangle ev  = layout.getEventPanel();
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

	// ── Opponent panel (left) ─────────────────────────────────────────────────

	private void drawOpponentPanel(Graphics2D g, HudLayoutState layout, Font normal, Font small)
	{
		Rectangle p = layout.getOpponentPanel();
		if (p == null)
		{
			return;
		}

		int x  = p.x + PAD;
		int w  = p.width - PAD * 2;
		int cy = p.y + PAD;

		// Section label
		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		g.drawString("OPPONENT", x, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		// Opponent name
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(WHITE);
		g.drawString("Marcbob", x, cy + fm.getAscent());
		cy += fm.getHeight() + 4;

		// HP bar
		g.setColor(HP_BG);
		g.fillRect(x, cy, w, BAR_H);
		g.setColor(HP_FG);
		g.fillRect(x, cy, (int) (w * (43f / 99f)), BAR_H);
		cy += BAR_H + 3;

		// HP label
		g.setFont(small);
		smFm = g.getFontMetrics();
		g.setColor(GRAY);
		g.drawString("~43 HP", x, cy + smFm.getAscent());

		// OPP VENG indicator pinned to panel bottom
		g.setColor(YELLOW);
		g.drawString("OPP VENG", x, p.y + p.height - PAD - smFm.getDescent());
	}

	// ── Event panel (center) ──────────────────────────────────────────────────

	private void drawEventPanel(Graphics2D g, HudLayoutState layout, Font normal, Font small)
	{
		Rectangle p = layout.getEventPanel();
		if (p == null)
		{
			return;
		}

		int cx = p.x + p.width / 2;
		int cy = p.y + PAD;

		// Section label
		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawCentered(g, smFm, "FIGHT", cx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 6;

		// CHANCE event
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(YELLOW);
		drawCentered(g, fm, "* CHANCE *", cx, cy + fm.getAscent());
		cy += fm.getHeight() + 3;

		// Outgoing hit  (<- = toward opponent)
		g.setColor(GREEN);
		drawCentered(g, fm, "<- 46", cx, cy + fm.getAscent());
		cy += fm.getHeight() + 3;

		// Prayer drain sub-note
		g.setFont(small);
		smFm = g.getFontMetrics();
		g.setColor(ORANGE);
		drawCentered(g, smFm, "PR -11", cx, cy + smFm.getAscent());
	}

	// ── Self panel (right) ────────────────────────────────────────────────────

	private void drawSelfPanel(Graphics2D g, HudLayoutState layout, Font normal, Font small)
	{
		Rectangle p = layout.getSelfPanel();
		if (p == null)
		{
			return;
		}

		int rx = p.x + p.width - PAD;
		int cy = p.y + PAD;

		// Section label
		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawRightAligned(g, smFm, "YOU", rx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();

		g.setColor(GREEN);
		drawRightAligned(g, fm, "VENG RDY", rx, cy + fm.getAscent());
		cy += fm.getHeight() + 1;

		g.setColor(LIGHT_BLUE);
		drawRightAligned(g, fm, "ICE 4t", rx, cy + fm.getAscent());
		cy += fm.getHeight() + 1;

		g.setColor(ORANGE);
		drawRightAligned(g, fm, "TB 1:21", rx, cy + fm.getAscent());
		cy += fm.getHeight() + 1;

		g.setColor(TOXIC_GREEN);
		drawRightAligned(g, fm, "VENOM", rx, cy + fm.getAscent());
	}

	// ── Boost row ─────────────────────────────────────────────────────────────

	private void drawBoostRow(Graphics2D g, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getBoostRow();
		if (p == null)
		{
			return;
		}

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		int col = p.width / 3;
		int cx1 = p.x + col / 2;
		int cx2 = cx1 + col;
		int cx3 = cx2 + col;

		g.setColor(GREEN);
		drawCentered(g, fm, "STR +18", cx1, baseline);

		g.setColor(GREEN);
		drawCentered(g, fm, "RNG +13", cx2, baseline);

		g.setColor(RED);
		drawCentered(g, fm, "MAG -7", cx3, baseline);
	}

	// ── Action strip (bottom) ─────────────────────────────────────────────────

	private void drawActionStrip(Graphics2D g, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getActionStrip();
		if (p == null)
		{
			return;
		}

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		String[] labels = {"ATK 2t", "EAT RDY", "POT RDY", "SPEC 50%", "5tx7", "T1 4:31", "SINGLE"};
		Color[]  colors = {YELLOW,   GREEN,      GREEN,      WHITE,      GRAY,   WHITE,      GRAY};

		int x = p.x + PAD;
		for (int i = 0; i < labels.length; i++)
		{
			if (i > 0)
			{
				g.setColor(DIVIDER);
				int sx = x - 4;
				g.drawLine(sx, p.y + 4, sx, p.y + p.height - 4);
				x += 3;
			}
			g.setColor(colors[i]);
			g.drawString(labels[i], x, baseline);
			x += fm.stringWidth(labels[i]) + 8;
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
