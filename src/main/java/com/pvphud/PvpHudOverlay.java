package com.pvphud;

import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.SelfState;
import java.awt.AlphaComposite;
import java.awt.Composite;
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
import net.runelite.api.HeadIcon;
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
	private static final int BOOST_ROW_H    = 34; // icon (18) + gap (2) + text (~9) + padding
	private static final int PAD            = 6;
	private static final int BAR_H          = 5;
	private static final int VERT_W         = 220;
	private static final int VERT_H         = 520;

	/** INVENTORY_HUG: top rail height and left rail width (px). */
	private static final int INVY_TOP_H  = 50;
	private static final int INVY_LEFT_W = 54;

	/** Icon size for VERTICAL_BAR and ICON_TRAY buff styles (pixels). */
	private static final int ICON_SIZE = 18;
	private static final int ICON_GAP  = 3;

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
	private static final Color RUN_FG       = new Color(160,  95,  20);
	private static final Color RUN_STAM_FG  = new Color(210, 145,  30); // stamina-tinted run energy
	private static final Color RUN_BG       = new Color( 30,  18,   5);
	private static final Color POTION_BLUE  = new Color( 80, 160, 240); // pot-cooldown dots

	// ── Injectable services ───────────────────────────────────────────────────
	private final Client       client;
	private final PvpHudPlugin plugin;
	private final PvpHudConfig config;
	private final ItemManager  itemManager;
	private final SpriteManager spriteManager;

	// ── Cached icons (item images — AsyncBufferedImage, thread-safe) ──────────
	private BufferedImage dscIcon;            // divine super combat
	private BufferedImage drgIcon;            // divine ranging
	private BufferedImage dmgIcon;            // divine magic
	private BufferedImage basIcon;            // divine bastion
	private BufferedImage btmIcon;            // divine battlemage
	private BufferedImage menIcon;            // menaphite remedy
	private BufferedImage staminaIcon;        // stamina potion
	private BufferedImage antiPoisonItemIcon; // anti-poison potion (immunity display)
	private BufferedImage antiVenomItemIcon;  // anti-venom potion (immunity display)

	// ── Cached icons (spell sprites — loaded async, volatile for EDT visibility)
	private volatile BufferedImage vengIcon;
	private volatile BufferedImage tbIcon;
	private volatile BufferedImage iceRushIcon;
	private volatile BufferedImage iceBurstIcon;
	private volatile BufferedImage iceBlitzIcon;
	private volatile BufferedImage iceBarrageIcon;
	private volatile BufferedImage poisonIcon;
	private volatile BufferedImage venomIcon;

	// ── Prayer icons for opponent overhead display ────────────────────────────
	private volatile BufferedImage prayMeleeIcon;
	private volatile BufferedImage prayRangedIcon;
	private volatile BufferedImage prayMagicIcon;
	private volatile BufferedImage smiteIcon;

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
		dscIcon            = itemManager.getImage(ItemID._4DOSEDIVINECOMBAT);
		drgIcon            = itemManager.getImage(ItemID._4DOSEDIVINERANGE);
		dmgIcon            = itemManager.getImage(ItemID._4DOSEDIVINEMAGIC);
		basIcon            = itemManager.getImage(ItemID._4DOSEDIVINEBASTION);
		btmIcon            = itemManager.getImage(ItemID._4DOSEDIVINEBATTLEMAGE);
		menIcon            = itemManager.getImage(MENAPHITE_REMEDY_4);
		staminaIcon        = itemManager.getImage(ItemID._4DOSESTAMINA);
		antiPoisonItemIcon = itemManager.getImage(ItemID._4DOSEANTIPOISON);
		antiVenomItemIcon  = itemManager.getImage(ItemID.ANTIVENOM_4);

		spriteManager.getSpriteAsync(SpriteID.SPELL_VENGEANCE,               0, img -> vengIcon      = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_TELE_BLOCK,              0, img -> tbIcon         = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_RUSH,                0, img -> iceRushIcon    = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BURST,               0, img -> iceBurstIcon   = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BLITZ,               0, img -> iceBlitzIcon   = img);
		spriteManager.getSpriteAsync(SpriteID.SPELL_ICE_BARRAGE,             0, img -> iceBarrageIcon = img);
		spriteManager.getSpriteAsync(SpriteID.MINIMAP_ORB_HITPOINTS_POISON,  0, img -> poisonIcon     = img);
		spriteManager.getSpriteAsync(SpriteID.MINIMAP_ORB_HITPOINTS_VENOM,   0, img -> venomIcon      = img);

		spriteManager.getSpriteAsync(SpriteID.PRAYER_PROTECT_FROM_MELEE,    0, img -> prayMeleeIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0, img -> prayRangedIcon = img);
		spriteManager.getSpriteAsync(SpriteID.PRAYER_PROTECT_FROM_MAGIC,    0, img -> prayMagicIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.PRAYER_SMITE,                 0, img -> smiteIcon      = img);

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
			case INVENTORY_HUG:
				return renderInventoryHug(g, state, layout, normal, small);
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
		int h = stored != null ? stored.height : 200;
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

	private Dimension renderInventoryHug(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (invWidget == null || invWidget.getWidth() == 0 || invWidget.getHeight() == 0) return null;

		Rectangle inv = invWidget.getBounds();
		int totalW = INVY_LEFT_W + inv.width;

		setPreferredLocation(new Point(inv.x - INVY_LEFT_W, inv.y - INVY_TOP_H));

		g.setColor(new Color(20, 20, 20, config.backgroundOpacity()));
		g.fillRect(0, 0, totalW, INVY_TOP_H);               // top rail
		g.fillRect(0, INVY_TOP_H, INVY_LEFT_W, inv.height); // left rail

		g.setColor(DIVIDER);
		g.drawLine(0, INVY_TOP_H - 1, totalW, INVY_TOP_H - 1);
		g.drawLine(INVY_LEFT_W - 1, INVY_TOP_H, INVY_LEFT_W - 1, INVY_TOP_H + inv.height);

		drawInventoryTopRail(g, state, normal, small, totalW);
		drawInventoryLeftRail(g, state, small, inv.height);

		return new Dimension(totalW, INVY_TOP_H + inv.height);
	}

	private void drawInventoryTopRail(Graphics2D g, PvpHudState state,
		Font normal, Font small, int totalW)
	{
		OpponentState opp = state.getOpponent();
		int cy = PAD / 2 + 1;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();

		if (!opp.isTracked())
		{
			g.setColor(GRAY);
			g.drawString("no target", PAD, cy + smFm.getAscent());
			return;
		}

		// Opponent name (left); veng warning (right) on same line
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(WHITE);
		String name = opp.getName();
		int maxNameW = Math.max(40, totalW - PAD * 2
			- (opp.isVengActive() ? smFm.stringWidth("VENG!") + PAD : 0));
		while (name.length() > 1 && fm.stringWidth(name) > maxNameW)
			name = name.substring(0, name.length() - 1);
		g.drawString(name, PAD, cy + fm.getAscent());

		if (opp.isVengActive())
		{
			g.setFont(small);
			g.setColor(ORANGE);
			drawRightAligned(g, smFm, "VENG!", totalW - PAD, cy + fm.getAscent());
		}
		cy += fm.getHeight() + 2;

		// HP bar spanning full top rail width
		int estHp = opp.getEstimatedHp();
		int maxHp = opp.getMaxHp();
		int barW  = totalW - PAD * 2;
		g.setFont(small);
		smFm = g.getFontMetrics();
		if (maxHp > 0)
		{
			float pct = Math.min(1f, (float) estHp / maxHp);
			Color fg = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			g.setColor(HP_BG);
			g.fillRect(PAD, cy, barW, BAR_H);
			g.setColor(fg);
			g.fillRect(PAD, cy, Math.max(1, (int)(barW * pct)), BAR_H);
			cy += BAR_H + 2;
			g.setColor(fg);
			g.drawString("~" + estHp + " HP", PAD, cy + smFm.getAscent());
		}
		else
		{
			g.setColor(HP_BG);
			g.fillRect(PAD, cy, barW, BAR_H);
			cy += BAR_H + 2;
			g.setColor(GRAY);
			g.drawString("HP ?", PAD, cy + smFm.getAscent());
		}
	}

	private void drawInventoryLeftRail(Graphics2D g, PvpHudState state,
		Font small, int railH)
	{
		int cx = INVY_LEFT_W / 2;
		int cy = INVY_TOP_H + PAD;

		ActionClockState clock = state.getActionClock();
		int atk    = clock.getAttackDelayTicks();
		int atkMax = clock.getLastWeaponSpeedTicks();

		if (atkMax > 0)
		{
			final int DOT_SZ  = 7;
			final int DOT_GAP = 3;
			for (int i = 0; i < atkMax; i++)
			{
				g.setColor(i < atk ? RED : DIVIDER);
				g.fillRect(cx - DOT_SZ / 2, cy + i * (DOT_SZ + DOT_GAP), DOT_SZ, DOT_SZ);
			}
			cy += atkMax * (DOT_SZ + DOT_GAP) + PAD;
		}

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		SelfState self = state.getSelf();

		if (self.isVengActive())
		{
			g.setColor(GREEN);
			drawCentered(g, fm, "VNG", cx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int ice = self.getFreezeTicksRemaining();
		if (ice > 0)
		{
			g.setColor(LIGHT_BLUE);
			drawCentered(g, fm, "ICE", cx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int tb = self.getTeleBlockTicksRemaining();
		if (tb > 0)
		{
			g.setColor(ORANGE);
			drawCentered(g, fm, "TB", cx, cy + fm.getAscent());
		}
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
		drawActionStrip(g, state, layout, small, vertical);
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

		// Title row: "OPPONENT" label left; large dedicated prayer icon right.
		// The icon sits here unconditionally so it's never displaced by content below.
		OpponentState opp    = state.getOpponent();
		final int     PRAY_SZ = 24;
		int titleRowH = Math.max(smFm.getHeight() + 2, PRAY_SZ + 2);

		g.setColor(GRAY);
		g.drawString("OPPONENT", x, cy + smFm.getAscent());

		HeadIcon prayer = opp.isTracked() ? opp.getOverheadPrayer() : null;
		if (prayer != null)
		{
			BufferedImage pIcon  = prayerIconFor(prayer);
			Color         pColor = prayerColorFor(prayer);
			int iconX = p.x + p.width - PAD - PRAY_SZ;
			int iconY = cy + (titleRowH - PRAY_SZ) / 2;
			if (pIcon != null)
			{
				g.drawImage(pIcon, iconX, iconY, PRAY_SZ, PRAY_SZ, null);
			}
			else
			{
				String pLabel = prayerShortFor(prayer);
				if (pLabel != null)
				{
					g.setColor(pColor);
					drawRightAligned(g, smFm, pLabel, p.x + p.width - PAD, cy + smFm.getAscent());
				}
			}
		}
		cy += titleRowH;

		if (!opp.isTracked())
		{
			g.setColor(GRAY);
			g.drawString("none", x, cy + smFm.getAscent());
			return;
		}

		// Name row (left) + VENG! indicator (right, if active)
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(WHITE);
		String name = opp.getName();
		int maxNameW = w - (opp.isVengActive() ? smFm.stringWidth("VENG!") + PAD : 0);
		while (name.length() > 1 && fm.stringWidth(name) > maxNameW)
			name = name.substring(0, name.length() - 1);
		g.drawString(name, x, cy + fm.getAscent());
		if (opp.isVengActive())
		{
			g.setFont(small);
			g.setColor(ORANGE);
			drawRightAligned(g, smFm, "VENG!", p.x + p.width - PAD, cy + fm.getAscent());
		}
		cy += fm.getHeight() + 4;

		// HP bar + HP text
		int estHp  = opp.getEstimatedHp();
		int maxHp  = opp.getMaxHp();
		int hpBarY = cy;
		g.setFont(small);
		smFm = g.getFontMetrics();
		if (estHp >= 0 && maxHp > 0)
		{
			float pct = Math.min(1f, (float) estHp / maxHp);
			g.setColor(HP_BG);
			g.fillRect(x, cy, w, BAR_H);
			Color fg = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			g.setColor(fg);
			g.fillRect(x, cy, Math.max(1, (int)(w * pct)), BAR_H);
			cy += BAR_H + 3;
			g.setColor(GRAY);
			g.drawString("~" + estHp + " HP", x, cy + smFm.getAscent());
			cy += smFm.getHeight() + 2;
		}
		else
		{
			g.setColor(HP_BG);
			g.fillRect(x, cy, w, BAR_H);
			cy += BAR_H + 3;
			g.setColor(GRAY);
			g.drawString("HP ?", x, cy + smFm.getAscent());
			cy += smFm.getHeight() + 2;
		}

		// Pending-hit animation: floats above the HP bar then fades
		if (opp.hasPendingHit())
		{
			long elapsed = System.currentTimeMillis() - opp.getPendingHitTimestampMs();
			float t = Math.min(1f, elapsed / 600f);
			int floatY   = hpBarY - 14;
			int landY    = hpBarY + BAR_H / 2;
			int displayY = (int)(floatY + (landY - floatY) * t);
			float alpha  = t < 0.67f ? 1f : (float)(1 - (t - 0.67) / 0.33);

			Composite prev = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
			g.setFont(normal);
			FontMetrics hitFm = g.getFontMetrics();
			g.setColor(YELLOW);
			String hitLabel = "→ " + opp.getPendingHitDamage();
			drawCentered(g, hitFm, hitLabel, p.x + p.width / 2, displayY + hitFm.getAscent());
			g.setComposite(prev);
		}

		// Opponent base stats (hiscores, async) — pinned to the panel bottom.
		// Omitted when there's not enough vertical room so the HP content is never displaced.
		g.setFont(small);
		smFm = g.getFontMetrics();
		int statsRowH = ICON_SIZE + 2 + smFm.getHeight() + 3;
		int statsY    = p.y + p.height - PAD - statsRowH;
		if (statsY >= cy + 2)
		{
			OpponentStats stats = opp.getStats();
			int col = p.width / 5;
			int c1  = p.x + col / 2;
			int c2  = c1 + col;
			int c3  = c2 + col;
			int c4  = c3 + col;
			int c5  = c4 + col;
			int iconY  = statsY;
			int textY  = statsY + ICON_SIZE + 2 + smFm.getAscent();
			drawStatColumn(g, smFm, atkSkillIcon, "ATK", stats.getAttack(),   c1, iconY, textY);
			drawStatColumn(g, smFm, strSkillIcon, "STR", stats.getStrength(), c2, iconY, textY);
			drawStatColumn(g, smFm, defSkillIcon, "DEF", stats.getDefence(),  c3, iconY, textY);
			drawStatColumn(g, smFm, rngSkillIcon, "RNG", stats.getRanged(),   c4, iconY, textY);
			drawStatColumn(g, smFm, magSkillIcon, "MAG", stats.getMagic(),    c5, iconY, textY);
		}
	}

	// ── Event panel ───────────────────────────────────────────────────────────

	private static final Color GRAY_DIM = new Color(100, 100, 100);

	private void drawEventPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
	{
		Rectangle p = layout.getEventPanel();
		if (p == null) return;

		int cx  = p.x + p.width / 2;
		int cy  = p.y + PAD;
		int mid = p.x + p.width / 2;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawCentered(g, smFm, "FIGHT", cx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		// Column headers: OUT | IN
		int outX = p.x + p.width / 4;      // centre of left column
		int inX  = p.x + 3 * p.width / 4;  // centre of right column
		g.setColor(GREEN);
		drawCentered(g, smFm, "OUT", outX, cy + smFm.getAscent());
		g.setColor(RED);
		drawCentered(g, smFm, "IN", inX, cy + smFm.getAscent());
		// vertical divider
		g.setColor(VERT_DIV);
		g.drawLine(mid, cy - 1, mid, p.y + p.height - PAD * 2 - smFm.getHeight());
		cy += smFm.getHeight() + 2;

		// Split events into out/in lists (newest first)
		PvpFightSession session = state.getCurrentSession();
		List<CombatEvent> allEvents = session != null
			? session.getRecentEvents()
			: java.util.Collections.emptyList();
		List<CombatEvent> outEvents = new ArrayList<>(3);
		List<CombatEvent> inEvents  = new ArrayList<>(3);
		for (CombatEvent ev : allEvents)
		{
			if (ev.getType() == CombatEventType.OUTGOING_HIT && outEvents.size() < 3)
				outEvents.add(ev);
			else if (ev.getType() == CombatEventType.INCOMING_HIT && inEvents.size() < 3)
				inEvents.add(ev);
		}

		g.setFont(normal);
		FontMetrics fmNorm = g.getFontMetrics();
		g.setFont(small);
		FontMetrics fmSm = g.getFontMetrics();

		int maxRows = 3;
		int rowH    = fmNorm.getHeight() + 1;
		for (int row = 0; row < maxRows; row++)
		{
			if (cy + rowH > p.y + p.height - PAD * 2 - smFm.getHeight()) break;
			boolean isFirst = row == 0;
			FontMetrics fm = isFirst ? fmNorm : fmSm;
			g.setFont(isFirst ? normal : small);

			if (row < outEvents.size())
			{
				g.setColor(isFirst ? GREEN : GRAY_DIM);
				drawCentered(g, fm, String.valueOf(outEvents.get(row).getDamage()), outX, cy + fm.getAscent());
			}
			if (row < inEvents.size())
			{
				CombatEvent inEv = inEvents.get(row);
				g.setColor(isFirst ? RED : GRAY_DIM);
				String inLabel = String.valueOf(inEv.getDamage());
				if (inEv.getPrayerDrain() > 0) inLabel += " -" + inEv.getPrayerDrain() + "p";
				drawCentered(g, fm, inLabel, inX, cy + fm.getAscent());
			}
			cy += isFirst ? fm.getHeight() + 2 : fm.getHeight() + 1;
		}

		// Fight over banner — shown when session has ended and no new session is active
		if (session == null && state.getSessionEndedMs() > 0)
		{
			g.setFont(small);
			smFm = g.getFontMetrics();
			g.setColor(RED);
			drawCentered(g, smFm, "FIGHT OVER", cx, cy + smFm.getAscent());
			cy += smFm.getHeight() + 4;
		}

		// Totals row at the bottom (live session or last session)
		int totalDealt    = session != null ? session.getTotalOutgoing()  : state.getLastTotalOutgoing();
		int totalReceived = session != null ? session.getTotalIncoming() : state.getLastTotalIncoming();
		if (totalDealt > 0 || totalReceived > 0)
		{
			// Thin divider above totals
			int divY = p.y + p.height - PAD - smFm.getHeight() - 3;
			g.setColor(VERT_DIV);
			g.drawLine(p.x + PAD, divY, p.x + p.width - PAD, divY);

			g.setFont(small);
			smFm = g.getFontMetrics();
			int totY = p.y + p.height - PAD;
			if (totalDealt > 0)
			{
				g.setColor(GREEN);
				drawCentered(g, smFm, totalDealt + "D", outX, totY);
			}
			if (totalReceived > 0)
			{
				g.setColor(RED);
				drawCentered(g, smFm, totalReceived + "R", inX, totY);
			}
		}
	}

	// ── Self panel ────────────────────────────────────────────────────────────

	private void drawSelfPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
	{
		Rectangle p = layout.getSelfPanel();
		if (p == null) return;

		int rx  = p.x + p.width - PAD;
		int lx  = p.x + PAD;
		int cy  = p.y + PAD;

		g.setFont(small);
		FontMetrics smFm = g.getFontMetrics();
		g.setColor(GRAY);
		drawRightAligned(g, smFm, "YOU", rx, cy + smFm.getAscent());
		cy += smFm.getHeight() + 2;

		SelfState   self   = state.getSelf();
		EffectState fx     = state.getEffects();
		BoostState  boosts = state.getBoosts();

		// Right side: resource bars (HP, Prayer, Run, Spec). Left side: buff indicators.
		// Both start from the same cy so they coexist side-by-side within the panel.
		drawHpPrayerBars(g, smFm, self, fx, rx, cy, p.width);

		BuffStyle style = config.buffStyle();
		if (style == BuffStyle.TEXT)
		{
			drawBuffsText(g, normal, small, self, fx, boosts, lx, cy);
		}
		else
		{
			List<Buff> buffs = buildBuffList(self, fx, boosts);
			if (!buffs.isEmpty())
			{
				if (style == BuffStyle.VERTICAL_BAR)
					drawBuffsVertBar(g, small, buffs, lx, cy);
				else
					drawBuffsIconTray(g, small, buffs, lx, cy);
			}
		}
	}

	// ── HP / Prayer bars ─────────────────────────────────────────────────────

	private static boolean barVisible(BarDisplayStyle s) { return s != BarDisplayStyle.HIDDEN; }
	private static boolean showBar(BarDisplayStyle s)    { return s == BarDisplayStyle.BARS_AND_NUMBERS || s == BarDisplayStyle.BARS_ONLY; }
	private static boolean showNum(BarDisplayStyle s)    { return s == BarDisplayStyle.BARS_AND_NUMBERS || s == BarDisplayStyle.NUMBERS_ONLY; }

	private int drawHpPrayerBars(Graphics2D g, FontMetrics fm, SelfState self,
		EffectState fx, int rx, int cy, int panelWidth)
	{
		int barW = Math.min(panelWidth - PAD * 2, 90);

		// Damped shake for 600 ms after incoming damage (can be disabled in config).
		// ω = 0.025 rad/ms → ~4 Hz (3 cycles over 600 ms); amplitude 8px.
		// Bar and text both shift by shakeX so they move as a unit.
		int shakeX = 0;
		if (config.showHpShake())
		{
			long incomingMs = self.getLastIncomingDamageMs();
			if (incomingMs > 0)
			{
				long elapsed = System.currentTimeMillis() - incomingMs;
				if (elapsed < 600)
				{
					double damping = 1.0 - elapsed / 600.0;
					shakeX = (int) (Math.sin(elapsed * 0.025) * 8 * damping);
				}
			}
		}

		// Uniform shift: effectiveRx is used for both bar origin and text alignment.
		int effectiveRx = rx + shakeX;
		int barX = effectiveRx - barW;

		// ── HP ──────────────────────────────────────────────────────────────────
		BarDisplayStyle hpStyle = config.hpBarStyle();
		if (self.getMaxHp() > 0 && barVisible(hpStyle))
		{
			float pct = Math.min(1f, (float) self.getCurrentHp() / self.getMaxHp());
			Color fg   = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar(hpStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				/* HP regen strip — commented out until timing is correct */
				cy += BAR_H + 1;
			}
			if (showNum(hpStyle))
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "HP " + self.getCurrentHp() + "/" + self.getMaxHp(),
					effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Prayer ───────────────────────────────────────────────────────────────
		BarDisplayStyle prStyle = config.prayerBarStyle();
		if (self.getMaxPrayer() > 0 && barVisible(prStyle))
		{
			float pct = Math.min(1f, (float) self.getCurrentPrayer() / self.getMaxPrayer());
			Color fg   = pct > 0.5f ? PRAYER_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar(prStyle))
			{
				g.setColor(PRAYER_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				cy += BAR_H + 1;
			}
			if (showNum(prStyle))
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "PR " + self.getCurrentPrayer() + "/" + self.getMaxPrayer(),
					effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Run energy ───────────────────────────────────────────────────────────
		BarDisplayStyle runStyle = config.runBarStyle();
		if (barVisible(runStyle))
		{
			int run = self.getRunEnergy();
			if (showBar(runStyle))
			{
				float runPct = Math.min(1f, run / 100f);
				g.setColor(RUN_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(RUN_FG);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * runPct)), BAR_H);
				cy += BAR_H + 1;
			}
			if (showNum(runStyle))
			{
				g.setColor(RUN_FG);
				drawRightAligned(g, fm, "RUN " + self.getRunEnergy() + "%", effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Spec ─────────────────────────────────────────────────────────────────
		BarDisplayStyle specStyle = config.specBarStyle();
		if (barVisible(specStyle))
		{
			int spec = fx.getSpecEnergy();
			if (showBar(specStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(YELLOW);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * spec / 100.0)), BAR_H);
				cy += BAR_H + 1;
			}
			if (showNum(specStyle))
			{
				g.setColor(YELLOW);
				drawRightAligned(g, fm, "SPEC " + spec + "%", effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		return cy;
	}

	// ── Text buff display (original behaviour) ────────────────────────────────

	private void drawBuffsText(Graphics2D g, Font normal, Font small,
		SelfState self, EffectState fx, BoostState boosts, int lx, int cy)
	{
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();

		if (self.isVengActive())
		{
			g.setColor(GREEN);
			g.drawString("VENG RDY", lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int freezeTicks = self.getFreezeTicksRemaining();
		if (freezeTicks > 0 && config.showFreezeTimer())
		{
			g.setColor(LIGHT_BLUE);
			g.drawString("ICE " + ticksToSecs(freezeTicks), lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		int tbTicks = self.getTeleBlockTicksRemaining();
		if (tbTicks > 0)
		{
			int s = tbTicks * 600 / 1000;
			g.setColor(ORANGE);
			g.drawString("TB " + (s / 60) + ":" + String.format("%02d", s % 60),
				lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		if (config.showDivineTimers())
		{
			cy = drawDivineTimerText(g, fm, "DSC", fx.getDivineSupercombatTicks(), lx, cy);
			cy = drawDivineTimerText(g, fm, "DRG", fx.getDivineRangingTicks(),     lx, cy);
			cy = drawDivineTimerText(g, fm, "DMG", fx.getDivineMagicTicks(),       lx, cy);
			cy = drawDivineTimerText(g, fm, "BAS", fx.getDivineBastionTicks(),     lx, cy);
			cy = drawDivineTimerText(g, fm, "BTM", fx.getDivineBattlemageTicks(),  lx, cy);
			cy = drawDivineTimerText(g, fm, "MEN", fx.getMenaphiteRemedyTicks(),   lx, cy);
		}

		int staminaTicks = fx.getStaminaEffectTicks();
		if (staminaTicks > 0)
		{
			int secs = staminaTicks * 600 / 1000;
			g.setColor(secs > 60 ? GREEN : secs > 30 ? YELLOW : RED);
			g.drawString("STAM " + (secs / 60) + ":" + String.format("%02d", secs % 60),
				lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}

		if (self.isVenomed())
		{
			g.setColor(TOXIC_GREEN);
			g.drawString("VENOM", lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}
		else if (self.isPoisoned())
		{
			g.setColor(TOXIC_GREEN);
			g.drawString("POISON", lx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}
		if (self.isAntiVenomActive())
		{
			cy = drawDivineTimerText(g, fm, "ANTI-V", fx.getAntiVenomTicks(), lx, cy);
		}
		else if (self.isAntiPoisonActive())
		{
			cy = drawDivineTimerText(g, fm, "ANTI-P", fx.getAntiPoisonTicks(), lx, cy);
		}

		int drainTicks = self.getStatDrainTicksRemaining();
		if (drainTicks > 0 && self.getStatDrainPeriod() > 0 && boosts.hasAnyBoost())
		{
			g.setColor(YELLOW);
			g.drawString("drain " + drainTicks + "t", lx, cy + fm.getAscent());
		}
	}

	/**
	 * Draws a timer label. When ticks > 0 shows "LABEL M:SS" with colour-coded time.
	 * When ticks == 0 the effect is active but the countdown is unavailable — shows
	 * just the label in green so the buff is still visible.
	 */
	private int drawDivineTimerText(Graphics2D g, FontMetrics fm, String label, int ticks, int lx, int cy)
	{
		if (ticks > 0)
		{
			int s = ticks * 600 / 1000;
			g.setColor(s > 60 ? GREEN : s > 30 ? YELLOW : RED);
			g.drawString(label + " " + (s / 60) + ":" + String.format("%02d", s % 60),
				lx, cy + fm.getAscent());
		}
		else
		{
			g.setColor(GREEN);
			g.drawString(label, lx, cy + fm.getAscent());
		}
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
		if (freeze > 0 && config.showFreezeTimer())
			slot = addBuff(buffScratch, buffPool, slot, iceIconFor(self.getFreezeSpriteId()),
				ticksToSecs(freeze), LIGHT_BLUE);

		int tb = self.getTeleBlockTicksRemaining();
		if (tb > 0)
		{
			int s = tb * 600 / 1000;
			slot = addBuff(buffScratch, buffPool, slot, tbIcon,
				(s / 60) + ":" + String.format("%02d", s % 60), ORANGE);
		}

		if (config.showDivineTimers())
		{
			slot = addDivineBuff(buffScratch, buffPool, slot, dscIcon, fx.getDivineSupercombatTicks());
			slot = addDivineBuff(buffScratch, buffPool, slot, drgIcon, fx.getDivineRangingTicks());
			slot = addDivineBuff(buffScratch, buffPool, slot, dmgIcon, fx.getDivineMagicTicks());
			slot = addDivineBuff(buffScratch, buffPool, slot, basIcon, fx.getDivineBastionTicks());
			slot = addDivineBuff(buffScratch, buffPool, slot, btmIcon, fx.getDivineBattlemageTicks());
			slot = addDivineBuff(buffScratch, buffPool, slot, menIcon, fx.getMenaphiteRemedyTicks());
		}

		slot = addDivineBuff(buffScratch, buffPool, slot, staminaIcon, fx.getStaminaEffectTicks());

		if (self.isVenomed())
			slot = addBuff(buffScratch, buffPool, slot, venomIcon, "VEN", TOXIC_GREEN);
		else if (self.isPoisoned())
			slot = addBuff(buffScratch, buffPool, slot, poisonIcon, "PSN", TOXIC_GREEN);

		if (self.isAntiVenomActive())
			slot = addDivineBuffFallback(buffScratch, buffPool, slot, antiVenomItemIcon,
				fx.getAntiVenomTicks(), "ANTI-V");
		else if (self.isAntiPoisonActive())
			slot = addDivineBuffFallback(buffScratch, buffPool, slot, antiPoisonItemIcon,
				fx.getAntiPoisonTicks(), "ANTI-P");

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

	/** Like addDivineBuff but shows the fallback label in green when ticks == 0 (active, no countdown). */
	private static int addDivineBuffFallback(List<Buff> list, Buff[] pool, int slot,
		BufferedImage icon, int ticks, String fallbackLabel)
	{
		if (ticks > 0) return addDivineBuff(list, pool, slot, icon, ticks);
		return addBuff(list, pool, slot, icon, fallbackLabel, GREEN);
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

	// ── VERTICAL_BAR: icon then label, left-aligned in the buff column ──────────

	private void drawBuffsVertBar(Graphics2D g, Font small, List<Buff> buffs, int lx, int cy)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int lineH = Math.max(fm.getHeight(), ICON_SIZE) + 1;

		for (Buff b : buffs)
		{
			int iconY = cy + (lineH - 1 - ICON_SIZE) / 2;
			if (b.icon != null)
			{
				g.drawImage(b.icon, lx, iconY, ICON_SIZE, ICON_SIZE, null);
			}
			int textX = b.icon != null ? lx + ICON_SIZE + ICON_GAP : lx;
			g.setColor(b.color);
			g.drawString(b.label, textX, cy + fm.getAscent());
			cy += lineH;
		}
	}

	// ── ICON_TRAY: horizontal strip of icons with labels below ────────────────

	private void drawBuffsIconTray(Graphics2D g, Font small, List<Buff> buffs, int lx, int cy)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();

		int slotW  = ICON_SIZE + ICON_GAP;
		int count  = buffs.size();
		int trayX  = lx; // left-aligned in the buff column

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
			int labelX = ix + (ICON_SIZE - fm.stringWidth(b.label)) / 2;
			g.drawString(b.label, labelX, cy + ICON_SIZE + 1 + fm.getAscent());
		}
	}

	// ── Boost row ─────────────────────────────────────────────────────────────

	private void drawBoostRow(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small)
	{
		if (!config.showBoostRow()) return;
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

	/** Renders an opponent stat column: skill icon above, plain level number below (white = known, gray = unknown). */
	private void drawStatColumn(Graphics2D g, FontMetrics fm, BufferedImage icon,
		String prefix, int level, int cx, int iconY, int textBaseline)
	{
		if (icon != null)
			g.drawImage(icon, cx - ICON_SIZE / 2, iconY, ICON_SIZE, ICON_SIZE, null);
		else
		{
			g.setColor(GRAY);
			drawCentered(g, fm, prefix, cx, iconY + fm.getAscent());
		}
		g.setColor(level >= 0 ? WHITE : GRAY);
		drawCentered(g, fm, level >= 0 ? String.valueOf(level) : "?", cx, textBaseline);
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

	private void drawActionStrip(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font small, boolean vertical)
	{
		Rectangle p = layout.getActionStrip();
		if (p == null) return;

		ActionClockState clock = state.getActionClock();
		int atk    = clock.getAttackDelayTicks();
		int atkMax = clock.getLastWeaponSpeedTicks();

		// Vertical layout: compact dots on the left, T1/T2 timer text on the right.
		if (vertical)
		{
			final int DOT_SZ_V  = 8;
			final int DOT_GAP_V = 2;
			int dotY = p.y + (p.height - DOT_SZ_V) / 2;

			for (int i = 0; i < 10; i++)
			{
				g.setColor(i < Math.min(atk, 10) ? RED : DIVIDER);
				g.fillRect(p.x + PAD + i * (DOT_SZ_V + DOT_GAP_V), dotY, DOT_SZ_V, DOT_SZ_V);
			}

			int textX    = p.x + PAD + 10 * (DOT_SZ_V + DOT_GAP_V) + 4;
			int textRx   = p.x + p.width - PAD;
			g.setFont(small);
			FontMetrics fm = g.getFontMetrics();
			int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

			ManualTimerState t1 = state.getTimer1();
			if (t1.isRunning() && textX < textRx)
			{
				int s = (int)(t1.getRemainingMs() / 1000);
				g.setColor(s > 30 ? WHITE : RED);
				String lbl = "T1 " + (s / 60) + ":" + String.format("%02d", s % 60);
				g.drawString(lbl, textX, baseline);
				textX += fm.stringWidth(lbl) + 6;
			}
			ManualTimerState t2 = state.getTimer2();
			if (t2.isRunning() && textX < textRx)
			{
				int s = (int)(t2.getRemainingMs() / 1000);
				g.setColor(s > 30 ? WHITE : RED);
				g.drawString("T2 " + (s / 60) + ":" + String.format("%02d", s % 60), textX, baseline);
			}
			return;
		}

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		int eat = state.getActionClock().getEatCooldownTicks();
		int pot = state.getActionClock().getPotCooldownTicks();

		// Dot section: [ATK dots] [EAT dots × 3] [POT dots × 3]
		final int DOT_SZ  = 5;
		final int DOT_GAP = 3;
		int dp = 0; // dot-pen x offset from (p.x + PAD)

		int dotY    = p.y + (p.height - DOT_SZ) / 2;
		int labelY  = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		if (atkMax > 0)
		{
			for (int i = 0; i < atkMax; i++)
			{
				g.setColor(i < atk ? RED : DIVIDER);
				g.fillRect(p.x + PAD + dp + i * (DOT_SZ + DOT_GAP), dotY, DOT_SZ, DOT_SZ);
			}
			dp += atkMax * (DOT_SZ + DOT_GAP) + DOT_GAP; // +DOT_GAP = group separator
		}

		// EAT dots: "E" label then 3 dots (orange = cooling down, dimmed = ready)
		{
			g.setColor(eat > 0 ? ORANGE : DIVIDER);
			g.drawString("E", p.x + PAD + dp, labelY);
			dp += fm.stringWidth("E") + 2;
			for (int i = 0; i < 3; i++)
			{
				g.setColor(i < eat ? ORANGE : DIVIDER);
				g.fillRect(p.x + PAD + dp + i * (DOT_SZ + DOT_GAP), dotY, DOT_SZ, DOT_SZ);
			}
			dp += 3 * (DOT_SZ + DOT_GAP) + DOT_GAP;
		}

		// POT dots: "P" label then 3 dots (blue = cooling down, dimmed = ready)
		{
			g.setColor(pot > 0 ? POTION_BLUE : DIVIDER);
			g.drawString("P", p.x + PAD + dp, labelY);
			dp += fm.stringWidth("P") + 2;
			for (int i = 0; i < 3; i++)
			{
				g.setColor(i < pot ? POTION_BLUE : DIVIDER);
				g.fillRect(p.x + PAD + dp + i * (DOT_SZ + DOT_GAP), dotY, DOT_SZ, DOT_SZ);
			}
			dp += 3 * (DOT_SZ + DOT_GAP);
		}

		int dotSectionW = dp;

		// Text items (spec moved to self panel; strip shows timers + context info only)
		List<String> labelList = new ArrayList<>(6);
		List<Color>  colorList  = new ArrayList<>(6);

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

		int usable = p.width - PAD * 2 - dotSectionW;
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

		int x = p.x + PAD + dotSectionW;
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

	/** Convert game ticks to a rounded-seconds string, e.g. 32 → "19s". */
	private static String ticksToSecs(int ticks)
	{
		return (int) Math.round(ticks * 0.6) + "s";
	}

	private BufferedImage prayerIconFor(HeadIcon prayer)
	{
		switch (prayer)
		{
			case MELEE:  return prayMeleeIcon;
			case RANGED: return prayRangedIcon;
			case MAGIC:  return prayMagicIcon;
			case SMITE:  return smiteIcon;
			default:     return null;
		}
	}

	private static Color prayerColorFor(HeadIcon prayer)
	{
		switch (prayer)
		{
			case MELEE:  return YELLOW;
			case RANGED: return GREEN;
			case MAGIC:  return LIGHT_BLUE;
			case SMITE:  return PRAYER_FG;
			default:     return GRAY;
		}
	}

	private static String prayerShortFor(HeadIcon prayer)
	{
		switch (prayer)
		{
			case MELEE:  return "MELE";
			case RANGED: return "RNG";
			case MAGIC:  return "MAGE";
			case SMITE:  return "SMITE";
			default:     return null;
		}
	}

	private Rectangle getChatboxBounds()
	{
		// UNIVERSE is the full chatbox panel — its bounds match the area we want to fill.
		// Fall back to CHATAREA if UNIVERSE is unavailable (e.g. transparent chatbox mode).
		Widget w = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (w == null || w.isHidden() || w.getWidth() == 0)
		{
			w = client.getWidget(InterfaceID.Chatbox.CHATAREA);
		}
		if (w == null || w.getWidth() == 0 || w.getHeight() == 0) return null;
		return w.getBounds();
	}
}
