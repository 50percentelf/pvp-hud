package com.pvphud;

import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.SelfState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PvpHudOverlay extends Overlay
{
	// ── Layout constants ──────────────────────────────────────────────────────
	private static final int ACTION_STRIP_H = 22;
	private static final int BOOST_ROW_H    = 28; // icon (14) + gap (2) + text (~7) + padding
	private static final int PAD            = 6;
	private static final int BAR_H          = 5;
	private static final int VERT_W         = 220;
	private static final int VERT_H         = 400;

	/** Icon size for VERTICAL_BAR and ICON_TRAY buff styles (pixels). */
	private static final int ICON_SIZE = 14;
	private static final int ICON_GAP  = 2;

	// Menaphite Remedy 4-dose — not yet in gameval/ItemID, use raw ID
	private static final int MENAPHITE_REMEDY_4 = 27202;

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
	private static final Color HP_FG        = new Color( 20, 185,  45);
	private static final Color HP_BG        = new Color( 75,  15,  15);
	private static final Color PRAYER_FG    = new Color(150, 140, 255);
	private static final Color PRAYER_BG    = new Color( 20,  15,  40);

	// ── Injectable services ───────────────────────────────────────────────────
	private final Client       client;
	private final PvpHudPlugin plugin;
	private final PvpHudConfig config;
	private final ItemManager  itemManager;
	private final SpriteManager spriteManager;

	// ── Cached icons (item images — AsyncBufferedImage, thread-safe) ──────────
	private BufferedImage dscIcon;   // divine super combat
	private BufferedImage drgIcon;   // divine ranging
	private BufferedImage dmgIcon;   // divine magic
	private BufferedImage basIcon;   // divine bastion
	private BufferedImage btmIcon;   // divine battlemage
	private BufferedImage menIcon;   // menaphite remedy

	// ── Cached icons (spell sprites — loaded async, volatile for EDT visibility)
	private volatile BufferedImage vengIcon;
	private volatile BufferedImage tbIcon;
	private volatile BufferedImage iceRushIcon;
	private volatile BufferedImage iceBurstIcon;
	private volatile BufferedImage iceBlitzIcon;
	private volatile BufferedImage iceBarrageIcon;
	private volatile BufferedImage poisonIcon;
	private volatile BufferedImage venomIcon;

	// ── Skill icons for boost row (volatile — written on client thread) ───────
	private volatile BufferedImage atkSkillIcon;
	private volatile BufferedImage strSkillIcon;
	private volatile BufferedImage defSkillIcon;
	private volatile BufferedImage rngSkillIcon;
	private volatile BufferedImage magSkillIcon;

	private HudLayout lastLayout;

	// ── Buff descriptor (built each frame, kept small to minimise GC) ─────────
	private static final class Buff
	{
		BufferedImage icon;
		String        label;
		Color         color;
	}

	// Pre-allocated buff list — reused each frame
	private final List<Buff> buffScratch = new ArrayList<>(12);
	private final Buff[]     buffPool    = new Buff[12];

	@Inject
	PvpHudOverlay(Client client, PvpHudPlugin plugin, PvpHudConfig config,
		ItemManager itemManager, SpriteManager spriteManager)
	{
		this.client       = client;
		this.plugin       = plugin;
		this.config       = config;
		this.itemManager  = itemManager;
		this.spriteManager = spriteManager;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
		for (int i = 0; i < buffPool.length; i++) buffPool[i] = new Buff();
	}

	/** Called from plugin startUp so images are queued as early as possible. */
	void loadIcons()
	{
		dscIcon = itemManager.getImage(ItemID._4DOSEDIVINECOMBAT);
		drgIcon = itemManager.getImage(ItemID._4DOSEDIVINERANGE);
		dmgIcon = itemManager.getImage(ItemID._4DOSEDIVINEMAGIC);
		basIcon = itemManager.getImage(ItemID._4DOSEDIVINEBASTION);
		btmIcon = itemManager.getImage(ItemID._4DOSEDIVINEBATTLEMAGE);
		menIcon = itemManager.getImage(MENAPHITE_REMEDY_4);

		spriteManager.getSpriteAsync(SpriteID.SPELL_VENGEANCE,               0, img -> vengIcon      = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_TELE_BLOCK,              0, img -> tbIcon         = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_RUSH,                0, img -> iceRushIcon    = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BURST,               0, img -> iceBurstIcon   = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BLITZ,               0, img -> iceBlitzIcon   = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BARRAGE,             0, img -> iceBarrageIcon = img);
		spriteManager.getSpriteAsync(SpriteID.MINIMAP_ORB_HITPOINTS_POISON,  0, img -> poisonIcon     = img);
		spriteManager.getSpriteAsync(SpriteID.MINIMAP_ORB_HITPOINTS_VENOM,   0, img -> venomIcon      = img);

		spriteManager.getSpriteAsync(SpriteID.SKILL_ATTACK,   0, img -> atkSkillIcon = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_STRENGTH, 0, img -> strSkillIcon = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_DEFENCE,  0, img -> defSkillIcon = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_RANGED,   0, img -> rngSkillIcon = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_MAGIC,    0, img -> magSkillIcon = img);
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

	// ── Per-layout render paths ───────────────────────────────────────────────

	private Dimension renderChatLocked(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle bounds = getChatboxBounds();
		if (bounds == null) return null;

		// Pin overlay to the chatbox position each frame so it can't be dragged away.
		setPreferredLocation(new Point(bounds.x, bounds.y));

		Rectangle local = new Rectangle(0, 0, bounds.width, bounds.height);
		if (!local.equals(layout.getChatboxBounds()))
		{
			layout.setChatboxBounds(new Rectangle(local));
			layout.markDirty();
		}
		if (layout.isDirty()) computeHorizLayout(layout, local);

		drawAll(g, state, layout, local, normal, small, false);
		return new Dimension(bounds.width, bounds.height);
	}

	private Dimension renderHorizontalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle stored = layout.getChatboxBounds();
		int w = stored != null ? stored.width  : 519;
		int h = stored != null ? stored.height : 142;
		Rectangle bounds = new Rectangle(0, 0, w, h);

		if (layout.isDirty()) computeHorizLayout(layout, bounds);

		drawAll(g, state, layout, bounds, normal, small, false);
		return new Dimension(w, h);
	}

	private Dimension renderVerticalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle bounds = new Rectangle(0, 0, VERT_W, VERT_H);
		if (layout.isDirty()) computeVertLayout(layout, bounds);

		drawAll(g, state, layout, bounds, normal, small, true);
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

		layout.setOpponentPanel(new Rectangle(b.x, b.y,            b.width, secH));
		layout.setEventPanel   (new Rectangle(b.x, b.y + secH,     b.width, secH));
		layout.setSelfPanel    (new Rectangle(b.x, b.y + 2 * secH, b.width, mainH - 2 * secH));
		layout.setBoostRow     (new Rectangle(b.x, b.y + mainH,    b.width, BOOST_ROW_H));
		layout.setActionStrip  (new Rectangle(b.x, b.y + mainH + BOOST_ROW_H, b.width, ACTION_STRIP_H));
		layout.setDirty(false);
	}

	// ── Draw everything ───────────────────────────────────────────────────────

	private void drawAll(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Rectangle bounds, Font normal, Font small, boolean vertical)
	{
		Rectangle strip    = layout.getActionStrip();
		Rectangle boostRow = layout.getBoostRow();
		Rectangle opp      = layout.getOpponentPanel();
		Rectangle ev       = layout.getEventPanel();

		g.setColor(new Color(20, 20, 20, config.backgroundOpacity()));
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

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
			Rectangle ev2   = layout.getEventPanel();
			Rectangle self2 = layout.getSelfPanel();
			if (ev2 != null)
			{
				g.setColor(DIVIDER);
				g.drawLine(ev2.x + PAD, ev2.y, ev2.x + ev2.width - PAD, ev2.y);
			}
			if (self2 != null)
			{
				g.setColor(DIVIDER);
				g.drawLine(self2.x + PAD, self2.y, self2.x + self2.width - PAD, self2.y);
			}
		}
		else
		{
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

		drawOpponentPanel(g, state, layout, normal, small);
		drawEventPanel(g, state, layout, normal, small);
		drawSelfPanel(g, state, layout, normal, small);
		drawBoostRow(g, state, layout, small);
		drawActionStrip(g, state, layout, small);
	}

	// ── Opponent panel ────────────────────────────────────────────────────────

	private void drawOpponentPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
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

		OpponentState opp = state.getOpponent();
		if (!opp.isTracked())
		{
			g.setColor(GRAY);
			g.drawString("none", x, cy + smFm.getAscent());
			return;
		}

		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(WHITE);
		String name = opp.getName();
		// Truncate long names to fit panel
		while (name.length() > 1 && fm.stringWidth(name) > w)
			name = name.substring(0, name.length() - 1);
		g.drawString(name, x, cy + fm.getAscent());
		cy += fm.getHeight() + 4;

		// HP bar (ratio-based from actor health bar)
		int estHp  = opp.getEstimatedHp();
		int maxHp  = opp.getMaxHp();
		if (estHp >= 0 && maxHp > 0)
		{
			float pct = Math.min(1f, (float) estHp / maxHp);
			g.setColor(HP_BG);
			g.fillRect(x, cy, w, BAR_H);
			Color fg = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			g.setColor(fg);
			g.fillRect(x, cy, Math.max(1, (int) (w * pct)), BAR_H);
			cy += BAR_H + 3;
			g.setFont(small);
			smFm = g.getFontMetrics();
			g.setColor(GRAY);
			g.drawString("~" + estHp + " HP", x, cy + smFm.getAscent());
		}
		else
		{
			g.setColor(HP_BG);
			g.fillRect(x, cy, w, BAR_H);
			cy += BAR_H + 3;
			g.setFont(small);
			smFm = g.getFontMetrics();
			g.setColor(GRAY);
			g.drawString("HP ?", x, cy + smFm.getAscent());
		}

		// Veng warning — shown when opponent has active Vengeance
		if (opp.isVengActive())
		{
			g.setFont(small);
			smFm = g.getFontMetrics();
			g.setColor(ORANGE);
			drawRightAligned(g, smFm, "VENG!", p.x + p.width - PAD,
				cy + smFm.getAscent());
			cy += smFm.getHeight() + 2;
		}

		// Last outgoing hit in corner
		int lastHit = opp.getLastOutgoingHit();
		if (lastHit > 0)
		{
			g.setFont(small);
			smFm = g.getFontMetrics();
			g.setColor(YELLOW);
			String hitStr = "→ " + lastHit;
			g.drawString(hitStr, p.x + p.width - PAD - smFm.stringWidth(hitStr),
				p.y + p.height - PAD - smFm.getDescent());
		}
	}

	// ── Event panel ───────────────────────────────────────────────────────────

	private static final Color GRAY_DIM = new Color(100, 100, 100);

	private void drawEventPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
	{
		Rectangle p = layout.getEventPanel();
		if (p == null) return;

		int cx = p.x + p.width / 2;
		int cy = p.y + PAD;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawCentered(g, smFm, "FIGHT", cx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 4;

		List<CombatEvent> events = state.getCombatEvent().getRecentEvents();
		if (events.isEmpty())
		{
			g.setColor(GRAY);
			drawCentered(g, smFm, "---", cx, cy + smFm.getAscent());
			return;
		}

		// Show most recent event large, older events small and dimmed
		g.setFont(normal);
		FontMetrics fmNorm = g.getFontMetrics();
		g.setFont(small);
		FontMetrics fmSm = g.getFontMetrics();

		for (int i = 0; i < events.size(); i++)
		{
			CombatEvent ev = events.get(i);
			boolean isNewest = i == 0;
			FontMetrics fm = isNewest ? fmNorm : fmSm;
			g.setFont(isNewest ? normal : small);

			Color base = ev.getType() == CombatEventType.OUTGOING_HIT ? GREEN : RED;
			g.setColor(isNewest ? base : GRAY_DIM);

			String arrow = ev.getType() == CombatEventType.OUTGOING_HIT ? "→ " : "← ";
			drawCentered(g, fm, arrow + ev.getDamage(), cx, cy + fm.getAscent());
			cy += fm.getHeight() + (isNewest ? 3 : 1);

			if (cy + fm.getHeight() > p.y + p.height - PAD) break;
		}
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

		SelfState   self = state.getSelf();
		EffectState fx   = state.getEffects();

		cy = drawHpPrayerBars(g, smFm, self, rx, cy, p.width);

		BoostState boosts = state.getBoosts();
		BuffStyle style = config.buffStyle();
		if (style == BuffStyle.TEXT)
		{
			drawBuffsText(g, normal, small, self, fx, boosts, rx, cy);
		}
		else
		{
			List<Buff> buffs = buildBuffList(self, fx, boosts);
			if (buffs.isEmpty()) return;
			if (style == BuffStyle.VERTICAL_BAR)
				drawBuffsVertBar(g, small, buffs, rx, cy);
			else
				drawBuffsIconTray(g, small, buffs, p, cy);
		}
	}

	// ── HP / Prayer bars ─────────────────────────────────────────────────────

	private int drawHpPrayerBars(Graphics2D g, FontMetrics fm, SelfState self,
		int rx, int cy, int panelWidth)
	{
		SelfBarStyle style = config.selfBarStyle();
		if (style == SelfBarStyle.HIDDEN) return cy;

		boolean showBar = style == SelfBarStyle.BARS_AND_NUMBERS || style == SelfBarStyle.BARS_ONLY;
		boolean showNum = style == SelfBarStyle.BARS_AND_NUMBERS || style == SelfBarStyle.NUMBERS_ONLY;

		int barW = Math.min(panelWidth - PAD * 2, 90);
		int barX = rx - barW;

		if (self.getMaxHp() > 0)
		{
			float pct = (float) self.getCurrentHp() / self.getMaxHp();
			Color fg   = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar)
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				cy += BAR_H + 1;
			}
			if (showNum)
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "HP " + self.getCurrentHp() + "/" + self.getMaxHp(),
					rx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;

				// Natural HP regen countdown: shown only while HP is below max
				int regenTicks = self.getHpRegenTicksRemaining();
				if (regenTicks > 0 && self.getCurrentHp() < self.getMaxHp())
				{
					g.setColor(GRAY);
					drawRightAligned(g, fm, "regen " + regenTicks + "t", rx, cy + fm.getAscent());
					cy += fm.getHeight() + 1;
				}
				else
				{
					cy += 1;
				}
			}
		}

		if (self.getMaxPrayer() > 0)
		{
			float pct = (float) self.getCurrentPrayer() / self.getMaxPrayer();
			Color fg   = pct > 0.5f ? PRAYER_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar)
			{
				g.setColor(PRAYER_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				cy += BAR_H + 1;
			}
			if (showNum)
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "PR " + self.getCurrentPrayer() + "/" + self.getMaxPrayer(),
					rx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		return cy;
	}

	// ── Text buff display (original behaviour) ────────────────────────────────

	private void drawBuffsText(Graphics2D g, Font normal, Font small,
		SelfState self, EffectState fx, BoostState boosts, int rx, int cy)
	{
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();

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

		cy = drawDivineTimerText(g, fm, "DSC", fx.getDivineSupercombatTicks(), rx, cy);
		cy = drawDivineTimerText(g, fm, "DRG", fx.getDivineRangingTicks(),     rx, cy);
		cy = drawDivineTimerText(g, fm, "DMG", fx.getDivineMagicTicks(),       rx, cy);
		cy = drawDivineTimerText(g, fm, "BAS", fx.getDivineBastionTicks(),     rx, cy);
		cy = drawDivineTimerText(g, fm, "BTM", fx.getDivineBattlemageTicks(),  rx, cy);
		cy = drawDivineTimerText(g, fm, "MEN", fx.getMenaphiteRemedyTicks(),   rx, cy);

		if (self.isVenomed())
		{
			g.setColor(TOXIC_GREEN);
			drawRightAligned(g, fm, "VENOM", rx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}
		else if (self.isPoisoned())
		{
			g.setColor(TOXIC_GREEN);
			drawRightAligned(g, fm, "POISON", rx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int drainTicks = self.getStatDrainTicksRemaining();
		if (drainTicks > 0 && self.getStatDrainPeriod() > 0 && boosts.hasAnyBoost())
		{
			g.setColor(YELLOW);
			drawRightAligned(g, fm, "drain " + drainTicks + "t", rx, cy + fm.getAscent());
		}
	}

	private int drawDivineTimerText(Graphics2D g, FontMetrics fm, String label, int ticks, int rx, int cy)
	{
		if (ticks <= 0) return cy;
		int s = ticks * 600 / 1000;
		g.setColor(s > 60 ? GREEN : s > 30 ? YELLOW : RED);
		drawRightAligned(g, fm, label + " " + (s / 60) + ":" + String.format("%02d", s % 60),
			rx, cy + fm.getAscent());
		return cy + fm.getHeight() + 1;
	}

	// ── Buff list builder (shared by VERTICAL_BAR and ICON_TRAY) ─────────────

	private List<Buff> buildBuffList(SelfState self, EffectState fx, BoostState boosts)
	{
		buffScratch.clear();
		int slot = 0;

		if (self.isVengActive())
			slot = addBuff(buffScratch, buffPool, slot, vengIcon, "VENG", GREEN);

		int freeze = self.getFreezeTicksRemaining();
		if (freeze > 0)
			slot = addBuff(buffScratch, buffPool, slot, iceIconFor(self.getFreezeSpriteId()),
				freeze + "t", LIGHT_BLUE);

		int tb = self.getTeleBlockTicksRemaining();
		if (tb > 0)
		{
			int s = tb * 600 / 1000;
			slot = addBuff(buffScratch, buffPool, slot, tbIcon,
				(s / 60) + ":" + String.format("%02d", s % 60), ORANGE);
		}

		slot = addDivineBuff(buffScratch, buffPool, slot, dscIcon, fx.getDivineSupercombatTicks());
		slot = addDivineBuff(buffScratch, buffPool, slot, drgIcon, fx.getDivineRangingTicks());
		slot = addDivineBuff(buffScratch, buffPool, slot, dmgIcon, fx.getDivineMagicTicks());
		slot = addDivineBuff(buffScratch, buffPool, slot, basIcon, fx.getDivineBastionTicks());
		slot = addDivineBuff(buffScratch, buffPool, slot, btmIcon, fx.getDivineBattlemageTicks());
		slot = addDivineBuff(buffScratch, buffPool, slot, menIcon, fx.getMenaphiteRemedyTicks());

		if (self.isVenomed())
			slot = addBuff(buffScratch, buffPool, slot, venomIcon, "VEN", TOXIC_GREEN);
		else if (self.isPoisoned())
			slot = addBuff(buffScratch, buffPool, slot, poisonIcon, "PSN", TOXIC_GREEN);

		int drainTicks = self.getStatDrainTicksRemaining();
		if (drainTicks > 0 && self.getStatDrainPeriod() > 0 && boosts.hasAnyBoost())
			slot = addBuff(buffScratch, buffPool, slot, null, "drain " + drainTicks + "t", YELLOW);

		return buffScratch;
	}

	private static int addBuff(List<Buff> list, Buff[] pool, int slot,
		BufferedImage icon, String label, Color color)
	{
		if (slot >= pool.length) return slot;
		Buff b = pool[slot];
		b.icon  = icon;
		b.label = label;
		b.color = color;
		list.add(b);
		return slot + 1;
	}

	private static int addDivineBuff(List<Buff> list, Buff[] pool, int slot,
		BufferedImage icon, int ticks)
	{
		if (ticks <= 0) return slot;
		int s = ticks * 600 / 1000;
		Color c = s > 60 ? GREEN : s > 30 ? YELLOW : RED;
		return addBuff(list, pool, slot, icon,
			(s / 60) + ":" + String.format("%02d", s % 60), c);
	}

	private BufferedImage iceIconFor(int spriteId)
	{
		switch (spriteId)
		{
			case SpriteID.SPELL_ICE_RUSH:    return iceRushIcon;
			case SpriteID.SPELL_ICE_BURST:   return iceBurstIcon;
			case SpriteID.SPELL_ICE_BLITZ:   return iceBlitzIcon;
			case SpriteID.SPELL_ICE_BARRAGE: return iceBarrageIcon;
			default:                         return iceBarrageIcon;
		}
	}

	// ── VERTICAL_BAR: icon to left of right-aligned label ────────────────────

	private void drawBuffsVertBar(Graphics2D g, Font small, List<Buff> buffs, int rx, int cy)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int lineH = Math.max(fm.getHeight(), ICON_SIZE) + 1;

		for (Buff b : buffs)
		{
			int textX = rx - fm.stringWidth(b.label);
			int iconY = cy + (lineH - 1 - ICON_SIZE) / 2;

			if (b.icon != null)
			{
				g.drawImage(b.icon, textX - ICON_SIZE - ICON_GAP, iconY, ICON_SIZE, ICON_SIZE, null);
			}

			g.setColor(b.color);
			g.drawString(b.label, textX, cy + fm.getAscent());
			cy += lineH;
		}
	}

	// ── ICON_TRAY: horizontal strip of icons with labels below ────────────────

	private void drawBuffsIconTray(Graphics2D g, Font small, List<Buff> buffs, Rectangle p, int cy)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();

		int slotW   = ICON_SIZE + ICON_GAP;
		int count   = buffs.size();
		int trayW   = count * slotW - ICON_GAP;
		int trayX   = p.x + p.width - PAD - trayW; // right-aligned

		for (int i = 0; i < count; i++)
		{
			Buff b  = buffs.get(i);
			int ix  = trayX + i * slotW;

			if (b.icon != null)
			{
				g.drawImage(b.icon, ix, cy, ICON_SIZE, ICON_SIZE, null);
			}
			else
			{
				// Placeholder rectangle when sprite not yet loaded
				g.setColor(b.color);
				g.fillRect(ix, cy, ICON_SIZE, ICON_SIZE);
			}

			// Label centred under the icon
			g.setColor(b.color);
			int lx = ix + (ICON_SIZE - fm.stringWidth(b.label)) / 2;
			g.drawString(b.label, lx, cy + ICON_SIZE + 1 + fm.getAscent());
		}
	}

	// ── Boost row ─────────────────────────────────────────────────────────────

	private void drawBoostRow(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getBoostRow();
		if (p == null) return;

		g.setFont(small);
		FontMetrics fm  = g.getFontMetrics();
		int iconY        = p.y + 1;
		int textBaseline = p.y + ICON_SIZE + 3 + fm.getAscent();

		int col = p.width / 5;
		int cx1 = p.x + col / 2;
		int cx2 = cx1 + col;
		int cx3 = cx2 + col;
		int cx4 = cx3 + col;
		int cx5 = cx4 + col;

		BoostState boosts = state.getBoosts();
		drawBoostColumn(g, fm, atkSkillIcon, "ATK", boosts.getAttackBoosted(),   boosts.getAttackReal(),   cx1, iconY, textBaseline);
		drawBoostColumn(g, fm, strSkillIcon, "STR", boosts.getStrengthBoosted(), boosts.getStrengthReal(), cx2, iconY, textBaseline);
		drawBoostColumn(g, fm, defSkillIcon, "DEF", boosts.getDefenceBoosted(),  boosts.getDefenceReal(),  cx3, iconY, textBaseline);
		drawBoostColumn(g, fm, rngSkillIcon, "RNG", boosts.getRangedBoosted(),   boosts.getRangedReal(),   cx4, iconY, textBaseline);
		drawBoostColumn(g, fm, magSkillIcon, "MAG", boosts.getMagicBoosted(),    boosts.getMagicReal(),    cx5, iconY, textBaseline);
	}

	private void drawBoostColumn(Graphics2D g, FontMetrics fm, BufferedImage icon,
		String prefix, int boosted, int real, int cx, int iconY, int textBaseline)
	{
		// Icon (or fallback prefix text) above the value
		if (icon != null)
		{
			g.drawImage(icon, cx - ICON_SIZE / 2, iconY, ICON_SIZE, ICON_SIZE, null);
		}
		else
		{
			g.setColor(GRAY);
			drawCentered(g, fm, prefix, cx, iconY + fm.getAscent());
		}

		// Value below
		int delta = boosted - real;
		g.setColor(delta > 0 ? GREEN : delta < 0 ? RED : GRAY);
		String label = config.boostXOverX()
			? boosted + "/" + real
			: (delta >= 0 ? "+" : "") + delta;
		drawCentered(g, fm, label, cx, textBaseline);
	}

	// ── Action strip ──────────────────────────────────────────────────────────

	private void drawActionStrip(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small)
	{
		Rectangle p = layout.getActionStrip();
		if (p == null) return;

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		ActionClockState clock = state.getActionClock();
		int atk  = clock.getAttackDelayTicks();
		int eat  = clock.getEatCooldownTicks();
		int pot  = clock.getPotCooldownTicks();
		int spec = state.getEffects().getSpecEnergy();

		// Build strip items dynamically; timers only shown while running
		List<String> labelList = new ArrayList<>(6);
		List<Color>  colorList  = new ArrayList<>(6);

		labelList.add(atk > 0 ? "ATK " + atk + "t" : "ATK rdy");
		colorList.add(atk > 0 ? YELLOW : GREEN);
		labelList.add(eat > 0 ? "EAT " + eat + "t" : "EAT");
		colorList.add(eat > 0 ? ORANGE : GREEN);
		labelList.add(pot > 0 ? "POT " + pot + "t" : "POT");
		colorList.add(pot > 0 ? ORANGE : GREEN);
		labelList.add("SPEC " + spec);
		colorList.add(spec < 100 ? YELLOW : WHITE);

		ManualTimerState t1 = state.getTimer1();
		if (t1.isRunning())
		{
			int s = (int) (t1.getRemainingMs() / 1000);
			labelList.add("T1 " + (s / 60) + ":" + String.format("%02d", s % 60));
			colorList.add(s > 30 ? WHITE : RED);
		}
		ManualTimerState t2 = state.getTimer2();
		if (t2.isRunning())
		{
			int s = (int) (t2.getRemainingMs() / 1000);
			labelList.add("T2 " + (s / 60) + ":" + String.format("%02d", s % 60));
			colorList.add(s > 30 ? WHITE : RED);
		}

		int wildLevel = state.getContext().getWildernessLevel();
		if (wildLevel > 0)
		{
			labelList.add("W" + wildLevel);
			colorList.add(YELLOW);
		}
		if (state.getContext().isMultiCombat())
		{
			labelList.add("MULTI");
			colorList.add(ORANGE);
		}

		String[] labels = labelList.toArray(new String[0]);
		Color[]  colors = colorList.toArray(new Color[0]);

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
				g.setColor(DIVIDER);
				g.drawLine(x - gapW + sepX, p.y + 4, x - gapW + sepX, p.y + p.height - 4);
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
		// CHATAREA (the message lines region) is reliable across transparent/opaque modes.
		// Fall back to UNIVERSE (root layer) if CHATAREA is unavailable.
		Widget w = client.getWidget(InterfaceID.Chatbox.CHATAREA);
		if (w == null || w.isHidden() || w.getWidth() == 0)
		{
			w = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		}
		if (w == null || w.getWidth() == 0 || w.getHeight() == 0) return null;
		return w.getBounds();
	}
}
