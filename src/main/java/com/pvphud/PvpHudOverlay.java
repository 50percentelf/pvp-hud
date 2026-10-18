package com.pvphud;

import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.DamageExchange;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import com.pvphud.state.PoisonState;
import com.pvphud.state.ProtectionState;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.SelfState;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.IconID;
import net.runelite.api.IndexedSprite;
import java.util.ArrayList;
import java.util.EnumMap;
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
	private static final int VERT_H         = 400;

	/** INVENTORY_HUG: top rail height and left rail width (px). */
	private static final int INVY_TOP_H  = 72;
	private static final int INVY_LEFT_W = 72;


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
	private static final Color TOXIC_GREEN    = new Color(110, 230,  60);
	/** Stable tint for periodic beneficial next-proc timers (Menaphite, Prayer Regen). */
	private static final Color BENEFICIAL_PROC = new Color(140, 200, 255);
	/** Outgoing damage — distinct from health-regen GREEN to avoid confusion. */
	private static final Color OUT_CYAN        = new Color( 80, 200, 215);
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
	private BufferedImage antifireIcon;       // antifire + extended antifire
	private BufferedImage superAntifireIcon;  // super antifire + extended super antifire
	private BufferedImage antiPoisonItemIcon; // anti-poison potion (immunity display)
	private BufferedImage antiVenomItemIcon;  // anti-venom potion (immunity display)
	private BufferedImage dsaIcon;            // divine super attack
	private BufferedImage dssIcon;            // divine super strength
	private BufferedImage dsdIcon;            // divine super defence
	private BufferedImage imbuedHeartIcon;    // imbued heart
	private BufferedImage prayerRegenIcon;    // prayer regeneration potion
	private BufferedImage sotdIcon;           // staff of the dead
	private BufferedImage surgeIcon;          // surge potion
	private volatile BufferedImage corruptIcon;   // greater corruption sprite
	private volatile BufferedImage wardIcon;      // ward of arceuus sprite
	private volatile BufferedImage markIcon;      // mark of darkness sprite

	// ── Cached icons for ironman account-type badge (loaded from mod icons) ────
	private volatile BufferedImage ironmanIcon;
	private volatile BufferedImage ultimateIronmanIcon;
	private volatile BufferedImage hardcoreIronmanIcon;

	// ── OSRS red hitsplat for prediction/confirmation indicator ──────────────
	private volatile BufferedImage redHitsplatIcon; // archive 1359, index 0

	// ── Cached skill icons for inventory-hug bars (HP / Prayer / Run / Spec) ──
	private volatile BufferedImage hpSkillIcon;
	private volatile BufferedImage praySkillIcon;
	private volatile BufferedImage runEnergyIcon;
	private volatile BufferedImage specIcon;

	// ── Cached icons (spell sprites — loaded async, volatile for EDT visibility)
	// vengIcon removed — Vengeance display deferred post-v0.1.
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

	private HudLayout    lastLayout;
	private LayoutProfile activeProfile = null;
	private boolean      lastHfChatLocked = false;

	/** True only during renderForDebug — bypasses config visibility guards so injected effects always show. */
	private boolean debugRendering = false;

	// ── Inventory Hug anchor cache ────────────────────────────────────────────
	// Recomputed only on: startup, LOGGED_IN/HOPPING, layout switch, canvas
	// resize, gameframe reload — not every render frame.
	private volatile boolean inventoryHugAnchorDirty = true;
	private HugGeometry      inventoryHugCachedGeom;
	private Rectangle        inventoryHugPaneBounds;

	/** Last known drag position for each float layout — restored on layout switch. */
	private GameframeType lastGameframe = null;
	private final EnumMap<GameframeType, Point> horizFloatPositions = new EnumMap<>(GameframeType.class);
	private final EnumMap<GameframeType, Point> vertFloatPositions  = new EnumMap<>(GameframeType.class);

	// Scratch list for building active-effect views each frame (cleared on reuse)
	private final List<ActiveEffectView> effectScratch = new ArrayList<>(16);

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
		antifireIcon       = itemManager.getImage(ItemID._4DOSE1ANTIDRAGON);
		superAntifireIcon  = itemManager.getImage(ItemID._4DOSE3ANTIDRAGON);
		antiPoisonItemIcon = itemManager.getImage(ItemID._4DOSEANTIPOISON);
		antiVenomItemIcon  = itemManager.getImage(ItemID.ANTIVENOM_4);
		dsaIcon         = itemManager.getImage(ItemID._4DOSEDIVINEATTACK);
		dssIcon         = itemManager.getImage(ItemID._4DOSEDIVINESTRENGTH);
		dsdIcon         = itemManager.getImage(ItemID._4DOSEDIVINEDEFENCE);
		imbuedHeartIcon = itemManager.getImage(ItemID.IMBUED_HEART);
		prayerRegenIcon = itemManager.getImage(ItemID._4DOSE1PRAYER_REGENERATION);
		sotdIcon        = itemManager.getImage(ItemID.SOTD);
		surgeIcon       = itemManager.getImage(ItemID._4DOSESURGE);
		// Ironman badges: loaded from client.getModIcons() in loadModIcons() (must be client thread).
		// OSRS red hitsplat: archive 1359, used for prediction/confirmation indicator.
		spriteManager.getSpriteAsync(1359, 0, img -> redHitsplatIcon = img);
		// corruptIcon, wardIcon, markIcon: SpriteID.MagicNecroOn not in API jar — text-only display.

		// SpriteID.SPELL_VENGEANCE not loaded — Vengeance display deferred post-v0.1.
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

		spriteManager.getSpriteAsync(SpriteID.SKILL_ATTACK,     0, img -> atkSkillIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_STRENGTH,   0, img -> strSkillIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_DEFENCE,    0, img -> defSkillIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_RANGED,     0, img -> rngSkillIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_MAGIC,      0, img -> magSkillIcon  = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_HITPOINTS,  0, img -> hpSkillIcon   = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_PRAYER,     0, img -> praySkillIcon = img);
		spriteManager.getSpriteAsync(SpriteID.SKILL_AGILITY,        0, img -> runEnergyIcon = img);
		spriteManager.getSpriteAsync(SpriteID.MINIMAP_ORB_SPECIAL_ICON, 0, img -> specIcon     = img);
	}

	/**
	 * Loads ironman account-type badges from {@code client.getModIcons()}.
	 * Must be called on the client thread (called from plugin's rehydrateFromClient).
	 */
	void loadModIcons()
	{
		IndexedSprite[] modIcons = client.getModIcons();
		if (modIcons == null) return;
		int imIdx  = IconID.IRONMAN.getIndex();
		int uimIdx = IconID.ULTIMATE_IRONMAN.getIndex();
		int hcIdx  = IconID.HARDCORE_IRONMAN.getIndex();
		if (imIdx  < modIcons.length) ironmanIcon         = indexedSpriteToImage(modIcons[imIdx]);
		if (uimIdx < modIcons.length) ultimateIronmanIcon = indexedSpriteToImage(modIcons[uimIdx]);
		if (hcIdx  < modIcons.length) hardcoreIronmanIcon = indexedSpriteToImage(modIcons[hcIdx]);
	}

	/**
	 * Re-attempts loading mod icons if any are still null.
	 * Called on the client thread when an opponent with non-NORMAL account type is tracked
	 * and the icon cache may have been null at initial load time (race condition).
	 */
	void ensureModIconsLoaded()
	{
		if (ironmanIcon == null || ultimateIronmanIcon == null || hardcoreIronmanIcon == null)
			loadModIcons();
	}

	private static BufferedImage indexedSpriteToImage(IndexedSprite sprite)
	{
		if (sprite == null) return null;
		byte[] pixels  = sprite.getPixels();
		int[]  palette = sprite.getPalette();
		int    w       = sprite.getWidth();
		int    h       = sprite.getHeight();
		int    ow      = sprite.getOriginalWidth();
		int    oh      = sprite.getOriginalHeight();
		int    offX    = sprite.getOffsetX();
		int    offY    = sprite.getOffsetY();
		if (pixels == null || palette == null || w <= 0 || h <= 0) return null;
		int imgW = ow > 0 ? ow : w;
		int imgH = oh > 0 ? oh : h;
		// Guard: ensure canvas is large enough for offsets + pixel data
		imgW = Math.max(imgW, offX + w);
		imgH = Math.max(imgH, offY + h);
		BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
		for (int py = 0; py < h; py++)
		{
			for (int px = 0; px < w; px++)
			{
				int idx = pixels[py * w + px] & 0xFF;
				if (idx == 0) continue; // index 0 = transparent
				if (idx >= palette.length) continue; // bounds guard
				int rgb = palette[idx];
				img.setRGB(offX + px, offY + py, 0xFF000000 | rgb);
			}
		}
		return img;
	}

	// ── Render entry point ────────────────────────────────────────────────────

	@Override
	public Dimension render(Graphics2D g)
	{
		boolean debugMode = config.debugMode();
		PvpHudState state = debugMode ? plugin.getDebugHudState() : plugin.getHudState();
		if (!debugMode && !plugin.shouldShowHud())
		{
			return null;
		}

		HudLayout currentLayout = config.hudLayout();
		HudLayoutState layout   = state.getLayout();
		activeProfile = LayoutProfile.forLayout(currentLayout, config);

		GameframeType currentGameframe = detectGameframe(client.isResized(), client.getTopLevelInterfaceId());

		if (lastGameframe == null)
		{
			lastGameframe = currentGameframe;
		}
		else if (currentGameframe != lastGameframe)
		{
			// Save current float position under the outgoing gameframe key
			Point outPos = getPreferredLocation();
			if (outPos != null)
			{
				if (currentLayout == HudLayout.HORIZONTAL_FLOAT)
					horizFloatPositions.put(lastGameframe, outPos);
				else if (currentLayout == HudLayout.VERTICAL_FLOAT)
					vertFloatPositions.put(lastGameframe, outPos);
			}
			// Restore position for incoming gameframe (null = cleared, first-time for this frame)
			if (currentLayout == HudLayout.HORIZONTAL_FLOAT)
				setPreferredLocation(horizFloatPositions.get(currentGameframe));
			else if (currentLayout == HudLayout.VERTICAL_FLOAT)
				setPreferredLocation(vertFloatPositions.get(currentGameframe));

			lastGameframe = currentGameframe;
			layout.markDirty();
			inventoryHugAnchorDirty = true;
		}

		if (currentLayout != lastLayout)
		{
			lastLayout = currentLayout;
			layout.markDirty();

			// Restore gameframe-specific position, or null to clear locked-layout anchor
			// on first-time float entry (prevents inheriting chatbox/inventory position).
			if (currentLayout == HudLayout.HORIZONTAL_FLOAT)
				setPreferredLocation(horizFloatPositions.get(currentGameframe));
			else if (currentLayout == HudLayout.VERTICAL_FLOAT)
				setPreferredLocation(vertFloatPositions.get(currentGameframe));

			invalidateInventoryHugAnchor();
		}

		// Per-frame position snapshot: keeps the map current while the user drags.
		if (currentLayout == HudLayout.HORIZONTAL_FLOAT)
		{
			Point pos = getPreferredLocation();
			if (pos != null) horizFloatPositions.put(currentGameframe, pos);
		}
		else if (currentLayout == HudLayout.VERTICAL_FLOAT)
		{
			Point pos = getPreferredLocation();
			if (pos != null) vertFloatPositions.put(currentGameframe, pos);
		}

		Font normal = FontManager.getRunescapeFont();
		Font small  = FontManager.getRunescapeSmallFont();

		switch (currentLayout)
		{
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

	private Dimension renderHorizontalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		boolean chatLocked = activeProfile.chatLocked;
		if (chatLocked != lastHfChatLocked)
		{
			lastHfChatLocked = chatLocked;
			layout.markDirty();
		}

		if (chatLocked)
		{
			Rectangle clBounds = getChatboxBounds();
			if (clBounds == null) return null;
			setPreferredLocation(new Point(clBounds.x, clBounds.y));
			Rectangle local = new Rectangle(0, 0, clBounds.width, clBounds.height);
			if (!local.equals(layout.getChatboxBounds()))
			{
				layout.setChatboxBounds(new Rectangle(local));
				layout.markDirty();
			}
			if (layout.isDirty()) computeHorizLayout(layout, local, activeProfile.showBoostRow, false);
			drawAll(g, state, layout, local, normal, small, false);
			return new Dimension(clBounds.width, clBounds.height);
		}

		Rectangle currentChat = getChatboxBounds();
		int w = currentChat != null ? currentChat.width  : 519;
		int h = currentChat != null ? currentChat.height : 200;
		Rectangle bounds = new Rectangle(0, 0, w, h);

		if (!bounds.equals(layout.getChatboxBounds()))
		{
			layout.setChatboxBounds(new Rectangle(bounds));
			layout.markDirty();
		}
		if (layout.isDirty()) computeHorizLayout(layout, bounds, activeProfile.showBoostRow, activeProfile.reserveBoostDockWhenHidden);

		drawAll(g, state, layout, bounds, normal, small, false);
		return new Dimension(w, h);
	}

	private Dimension renderVerticalFloat(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		Rectangle bounds = new Rectangle(0, 0, VERT_W, VERT_H);
		if (layout.isDirty()) computeVertLayout(layout, bounds, activeProfile.showBoostRow, activeProfile.reserveBoostDockWhenHidden);

		drawAll(g, state, layout, bounds, normal, small, true);
		return new Dimension(VERT_W, VERT_H);
	}

	private Dimension renderInventoryHug(Graphics2D g, PvpHudState state,
		HudLayoutState layout, Font normal, Font small)
	{
		// Recompute anchor only when dirty (event-driven invalidation).
		// Normal steady-state path: zero parent-chain traversal, no widget walks.
		if (inventoryHugAnchorDirty)
			recalculateInventoryHugAnchor();

		HugGeometry hug = inventoryHugCachedGeom;
		if (hug == null) return null;

		// Single per-render widget lookup — no parent chain traversal.
		// Used only to determine active-tab state for tabH/itemsH layout.
		Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
		boolean inventoryActive = invWidget != null
			&& !invWidget.isHidden()
			&& invWidget.getWidth() > 0
			&& invWidget.getHeight() > 0;

		// tabH/itemsH affect left-rail content layout and change when the user
		// switches tabs. The anchor itself is cached and unaffected.
		int tabH, itemsH;
		if (inventoryActive)
		{
			Rectangle items      = invWidget.getBounds();
			Rectangle paneBounds = inventoryHugPaneBounds;
			tabH   = paneBounds != null ? Math.max(0, items.y - paneBounds.y) : 0;
			itemsH = items.height;
		}
		else
		{
			tabH   = 0;
			itemsH = hug.paneH;
		}

		Color bg = bgColor(activeProfile.backgroundOpacity);
		g.setColor(bg);

		// ── Top arm: horizontal strip spanning the full overlay width ─────────
		g.fillRect(0, 0, hug.totalW, INVY_TOP_H);

		// ── Left arm: vertical strip covering the full pane height ────────────
		g.fillRect(0, INVY_TOP_H, INVY_LEFT_W, hug.paneH);

		// ── Inner border edges that face the inventory ────────────────────────
		g.setColor(DIVIDER);
		g.drawLine(0, INVY_TOP_H - 1, hug.totalW - 1, INVY_TOP_H - 1);
		g.drawLine(INVY_LEFT_W - 1, INVY_TOP_H, INVY_LEFT_W - 1, hug.totalH - 1);

		drawInventoryTopRail(g, state, small, hug.totalW);
		drawInventoryLeftRail(g, state, small, tabH, itemsH);

		return new Dimension(hug.totalW, hug.totalH);
	}

	/**
	 * Walks the widget parent chain from the ITEMS widget to find the side-panel
	 * container that also holds the tab row.
	 *
	 * In Fixed mode the immediate parent is the inventory pane (tab row + items).
	 * In Resizable modes one or more hidden intermediate wrappers may sit between
	 * ITEMS and the actual pane, so hidden widgets are skipped (not a break) and
	 * only visible candidates are evaluated. The first visible ancestor that starts
	 * above items.y (wraps the tab row) and is not unreasonably tall is returned.
	 */
	private Widget findInventoryPane(Widget invWidget)
	{
		Rectangle items = invWidget.getBounds();
		int maxH = items.height + 200;
		for (Widget w = invWidget.getParent(); w != null; w = w.getParent())
		{
			if (w.getWidth() <= 0) break;
			if (w.isHidden()) continue; // skip hidden intermediate wrappers (Resizable mode)
			Rectangle wb = w.getBounds();
			if (wb.height > maxH) break;
			if (wb.y < items.y && wb.width >= items.width)
				return w;
		}
		return null;
	}

	/** Mark the cached anchor stale; next render frame will recompute it. */
	void invalidateInventoryHugAnchor()
	{
		inventoryHugAnchorDirty = true;
	}

	/**
	 * True for one render frame after a gameframe switch.
	 * recalculateInventoryHugAnchor() skips committing on the settling frame so
	 * that stale widget positions (from the old gameframe's layout scripts) are
	 * never locked in as the anchor.  The subsequent frame re-reads fresh positions
	 * and commits normally.
	 */
	private boolean inventoryHugSettling = false;

	static GameframeType detectGameframe(boolean resized, int topLevelId)
	{
		if (!resized) return GameframeType.FIXED;
		if (topLevelId == InterfaceID.TOPLEVEL_OSRS_STRETCH) return GameframeType.RESIZABLE_CLASSIC;
		if (topLevelId == InterfaceID.TOPLEVEL_PRE_EOC)      return GameframeType.RESIZABLE_MODERN;
		return GameframeType.RESIZABLE_CLASSIC;
	}

	void onGameframeChanged()
	{
		plugin.getHudState().getLayout().markDirty();
		inventoryHugAnchorDirty = true;
		inventoryHugSettling = true;
	}

	/**
	 * Standard OSRS stone-tablet tab-row height in all three gamemodes.
	 * Used as a fallback when widget traversal cannot locate the pane container.
	 */
	private static final int STONE_TABLET_TAB_H = 38;

	/**
	 * Recomputes the inventory-hug anchor by locating the side-panel container widget.
	 * Called at most once per invalidation cycle; the result is cached until the next
	 * invalidation (canvas resize, login, layout switch, gameframe reload).
	 *
	 * In Fixed mode the widget traversal reliably returns the pane.
	 * In Resizable Classic / Modern the traversal may fail (e.g. the first visible
	 * ancestor above ITEMS is the full-height sidebar, exceeding the height guard).
	 * In that case a synthetic pane is derived from the ITEMS widget bounds plus the
	 * fixed 38-px tab-row height, which is consistent across all gamemodes.
	 *
	 * If the required widgets are not yet available, the dirty flag stays true so
	 * the next render frame retries. Previously cached geometry is preserved on
	 * failure so the HUD can still render between invalidation cycles.
	 */
	private void recalculateInventoryHugAnchor()
	{
		Widget invWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (invWidget == null)
			return; // widget not yet loaded; remain dirty and retry

		Widget paneWidget = findInventoryPane(invWidget);

		Rectangle pane;
		if (paneWidget != null && !paneWidget.isHidden()
			&& paneWidget.getWidth() > 0 && paneWidget.getHeight() > 0)
		{
			pane = paneWidget.getBounds();
		}
		else
		{
			// Traversal failed (Resizable mode: hidden intermediate wrappers or
			// oversized sidebar as the first visible ancestor above ITEMS).
			// Synthesize pane bounds from the ITEMS widget using the fixed tab-row height.
			Rectangle inv = invWidget.getBounds();
			if (inv.width <= 0 || inv.height <= 0) return; // remain dirty and retry
			pane = new Rectangle(inv.x, inv.y - STONE_TABLET_TAB_H,
				inv.width, STONE_TABLET_TAB_H + inv.height);
		}

		boolean invActive = !invWidget.isHidden() && invWidget.getWidth() > 0;
		Rectangle items   = invActive ? invWidget.getBounds() : pane;

		HugGeometry hug = computeHug(items, pane, INVY_LEFT_W, INVY_TOP_H);
		if (inventoryHugSettling)
		{
			inventoryHugSettling = false;
			return; // skip committing; recompute next frame with settled widget positions
		}
		inventoryHugCachedGeom = hug;
		inventoryHugPaneBounds = new Rectangle(pane);
		setPreferredLocation(hug.anchor);
		inventoryHugAnchorDirty = false;
	}

	/** Immutable geometry bundle for the inventory-hug L-shape. Package-visible for tests. */
	static final class HugGeometry
	{
		final Point anchor;  // overlay top-left in screen coordinates
		final int   totalW;  // left rail + pane width
		final int   totalH;  // top arm + pane height
		final int   paneH;   // pane.height = tabH + itemsH
		final int   tabH;    // pixels from pane top to item-grid top (tab-row height; 0 if absent)
		final int   itemsH;  // item-grid height

		HugGeometry(Point anchor, int totalW, int totalH, int paneH, int tabH, int itemsH)
		{
			this.anchor = anchor;
			this.totalW = totalW;
			this.totalH = totalH;
			this.paneH  = paneH;
			this.tabH   = tabH;
			this.itemsH = itemsH;
		}
	}

	/**
	 * Pure geometry: given item-grid and pane rectangles in screen coordinates and
	 * rail dimensions, returns all values needed to render the L-shape.
	 *
	 * {@code pane} equals {@code items} when no tab-containing ancestor was found.
	 * In that case {@code tabH} will be 0 and the top arm is anchored directly
	 * above the item grid — which may still clip into the tab row if the tab row
	 * lives outside the widget hierarchy we could reach.
	 *
	 * Package-visible so tests can verify the geometry without a running client.
	 */
	static HugGeometry computeHug(Rectangle items, Rectangle pane, int leftW, int topH)
	{
		int tabH   = items.y - pane.y;
		int totalW = leftW + pane.width;
		int totalH = topH  + pane.height;
		return new HugGeometry(
			new Point(pane.x - leftW, pane.y - topH),
			totalW, totalH, pane.height, tabH, items.height);
	}

	private void drawInventoryTopRail(Graphics2D g, PvpHudState state,
		Font small, int totalW)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();

		// ── Corner (above left rail): attack clock + eat/pot dots ─────────────
		ActionClockState clock = state.getActionClock();
		int tick = client.getTickCount();
		int atk  = clock.getAttackDelayTicks(tick);
		{
			final int DOT_SZ  = 4;
			final int DOT_GAP = 2;
			final int PER_ROW = 5;
			int rowW = PER_ROW * DOT_SZ + (PER_ROW - 1) * DOT_GAP; // 28 px
			int dotX = (INVY_LEFT_W - rowW) / 2;
			// Anchor the dot+EP block at the top of the corner, leaving room below for DEALT/TAKEN.
			int dotY = PAD + 2;
			for (int i = 0; i < 10; i++)
			{
				int row = i / PER_ROW;
				int col = i % PER_ROW;
				g.setColor(i < atk ? RED : DIVIDER);
				g.fillRect(dotX + col * (DOT_SZ + DOT_GAP), dotY + row * (DOT_SZ + DOT_GAP),
					DOT_SZ, DOT_SZ);
			}
			int epY = dotY + 2 * DOT_SZ + DOT_GAP + 4;
			int eat = clock.getEatCooldownTicks(tick);
			int pot = clock.getPotCooldownTicks(tick);
			int px  = PAD;
			g.setColor(eat > 0 ? ORANGE : DIVIDER);
			g.drawString("E", px, epY + fm.getAscent());
			px += fm.stringWidth("E") + 2;
			for (int i = 0; i < 3; i++)
			{
				g.setColor(i < eat ? ORANGE : DIVIDER);
				g.fillRect(px + i * (DOT_SZ + DOT_GAP), epY + (fm.getHeight() - DOT_SZ) / 2, DOT_SZ, DOT_SZ);
			}
			px += 3 * (DOT_SZ + DOT_GAP) + 3;
			g.setColor(pot > 0 ? POTION_BLUE : DIVIDER);
			g.drawString("P", px, epY + fm.getAscent());
			px += fm.stringWidth("P") + 2;
			for (int i = 0; i < 3; i++)
			{
				g.setColor(i < pot ? POTION_BLUE : DIVIDER);
				g.fillRect(px + i * (DOT_SZ + DOT_GAP), epY + (fm.getHeight() - DOT_SZ) / 2, DOT_SZ, DOT_SZ);
			}
		}

		// ── Corner: DEALT/TAKEN totals block (lower portion of corner) ───────
		{
			PvpFightSession dmgSession = state.getCurrentSession();
			int totalOut = dmgSession != null ? dmgSession.getTotalOutgoing() : state.getLastTotalOutgoing();
			int totalIn  = dmgSession != null ? dmgSession.getTotalIncoming() : state.getLastTotalIncoming();

			int dmgBlockH = 2 * fm.getHeight() + 2;
			int dmgY = INVY_TOP_H - PAD - dmgBlockH;
			int labelX  = PAD;
			int valueRx = INVY_LEFT_W - PAD;

			g.setColor(OUT_CYAN);
			g.drawString("DEALT", labelX, dmgY + fm.getAscent());
			String outStr = String.valueOf(totalOut);
			g.setColor(totalOut > 0 ? WHITE : GRAY);
			g.drawString(outStr, valueRx - fm.stringWidth(outStr), dmgY + fm.getAscent());
			dmgY += fm.getHeight() + 1;

			g.setColor(RED);
			g.drawString("TAKEN", labelX, dmgY + fm.getAscent());
			String inStr = String.valueOf(totalIn);
			g.setColor(totalIn > 0 ? WHITE : GRAY);
			g.drawString(inStr, valueRx - fm.stringWidth(inStr), dmgY + fm.getAscent());
		}

		// ── Pane: 3-row opponent panel ───────────────────────────────────────
		//   Row 0 (stats): [ATK:99] [STR:99] [DEF:99] [RNG:99] [MAG:99]
		//   Row 1 (name):  [opponent name]
		//   Row 2 (hp):    [====HP bar 43/99====]
		//   Right col:     [PRAYER — same height as all 3 rows]
		final int HUD_BAR_H = 10;
		final int GAP       = 3;
		final int sIconSz   = 11;          // stat icon size; fixed to break circular dependency
		int nameH     = fm.getHeight();
		int totalRowH = sIconSz + GAP + nameH + GAP + HUD_BAR_H;  // full content block height

		int x0 = INVY_LEFT_W + PAD;
		int rx = totalW - PAD;

		OpponentState opp     = state.getOpponent();
		HeadIcon      prayer  = opp.getOverheadPrayer();
		int praySz  = Math.min(totalRowH, config.opponentPrayerIconSize());
		int prayX   = rx - praySz;
		int barEndX = prayer != null ? prayX - 3 : rx;
		int barSz   = barEndX - x0;

		int y0 = (INVY_TOP_H - totalRowH) / 2;  // stats row top
		int y1 = y0 + sIconSz + GAP;             // name row top
		int y2 = y1 + nameH + GAP;               // HP bar row top

		if (!opp.isTracked())
		{
			g.setColor(GRAY);
			g.drawString("---", x0, y0 + fm.getAscent());
			return;
		}

		// ── Row 0: stat cells evenly distributed across bar width ─────────────
		OpponentStats stats = opp.getStats();
		int[] statLvls  = { stats.getAttack(), stats.getStrength(), stats.getDefence(),
		                    stats.getRanged(),  stats.getMagic()  };
		BufferedImage[] statIcons = { atkSkillIcon, strSkillIcon, defSkillIcon,
		                              rngSkillIcon, magSkillIcon   };
		int[] numW = new int[5];
		for (int i = 0; i < 5; i++)
		{
			String s = statLvls[i] >= 0 ? String.valueOf(statLvls[i]) : "?";
			numW[i] = fm.stringWidth(s);
		}
		int cellW = barSz / 5;
		int sNumY = y0 + fm.getAscent();
		for (int i = 0; i < 5; i++)
		{
			int contentW = sIconSz + 1 + numW[i];
			int cellX    = x0 + i * cellW + (cellW - contentW) / 2;
			if (statIcons[i] != null)
				g.drawImage(statIcons[i], cellX, y0, sIconSz, sIconSz, null);
			g.setColor(statLvls[i] >= 0 ? WHITE : GRAY);
			g.drawString(statLvls[i] >= 0 ? String.valueOf(statLvls[i]) : "?",
				cellX + sIconSz + 1, sNumY);
		}

		// ── Row 1: opponent name + ironman badge ──────────────────────────────────────
		int nameMaxW = barEndX - x0;
		String name = opp.getName();
		while (name.length() > 1 && fm.stringWidth(name) > nameMaxW)
			name = name.substring(0, name.length() - 1);
		g.setColor(WHITE);
		g.drawString(name, x0, y1 + fm.getAscent());
		OpponentStats.AccountType acctType = opp.getStats().getAccountType();
		if (acctType != OpponentStats.AccountType.NORMAL)
		{
			int badgeSz = fm.getHeight();
			int badgeX  = x0 + fm.stringWidth(name) + 3;
			BufferedImage badgeIcon = iconForAccountType(acctType);
			if (badgeIcon != null && badgeIcon.getWidth() > 0)
			{
				int bw = badgeIcon.getWidth();
				int bh = badgeIcon.getHeight();
				int drawSz = (bw <= badgeSz && bh <= badgeSz) ? Math.max(bw, bh) : badgeSz;
				g.drawImage(badgeIcon, badgeX, y1 + (badgeSz - drawSz) / 2, drawSz, drawSz, null);
			}
			else
			{
				g.setColor(acctType == OpponentStats.AccountType.HARDCORE_IRONMAN ? RED : GRAY);
				g.drawString(accountTypeBadge(acctType), badgeX, y1 + fm.getAscent());
			}
		}

		// ── Row 2: HP bar, text overlaid ─────────────────────────────────────
		int estHp = opp.getEstimatedHp();
		int maxHp = opp.getMaxHp();
		if (maxHp > 0)
		{
			float pct = Math.min(1f, (float) estHp / maxHp);
			Color fg  = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			g.setColor(HP_BG);
			g.fillRect(x0, y2, barSz, HUD_BAR_H);
			g.setColor(fg);
			g.fillRect(x0, y2, Math.max(1, (int)(barSz * pct)), HUD_BAR_H);
			String hpStr = estHp + "/" + maxHp;
			int    hpY   = y2 + (HUD_BAR_H + fm.getAscent() - fm.getDescent()) / 2;
			drawBarValueRight(g, fm, hpStr, barEndX - 2, hpY, WHITE);
		}
		else
		{
			g.setColor(HP_BG);
			g.fillRect(x0, y2, barSz, HUD_BAR_H);
			String hpStr = "HP?";
			int    hpY   = y2 + (HUD_BAR_H + fm.getAscent() - fm.getDescent()) / 2;
			drawBarValueRight(g, fm, hpStr, barEndX - 2, hpY, GRAY);
		}

		// ── Prayer icon: right column, vertically centred in the content block ──
		if (prayer != null)
		{
			int prayIconY = y0 + (totalRowH - praySz) / 2;
			BufferedImage pIcon = prayerIconFor(prayer);
			if (pIcon != null)
				g.drawImage(pIcon, prayX, prayIconY, praySz, praySz, null);
			else
			{
				g.setColor(prayerColorFor(prayer));
				g.drawString(prayerShortFor(prayer), prayX, prayIconY + fm.getAscent());
			}
		}

		// Transient outgoing damage number — shown below opponent HP bar for ~750 ms after we hit
		{
			long outMs  = opp.getLastOutgoingHitMs();
			int  outDmg = opp.getLastOutgoingHit();
			if (outMs > 0 && outDmg > 0 && System.currentTimeMillis() - outMs < 750)
			{
				String s = String.valueOf(outDmg);
				g.setColor(OUT_CYAN);
				g.drawString(s, x0, y2 + HUD_BAR_H + fm.getAscent() + 1);
			}
		}
	}

	private void drawInventoryLeftRail(Graphics2D g, PvpHudState state,
		Font small, int tabH, int itemsH)
	{
		int cx    = INVY_LEFT_W / 2;
		int lx    = PAD;
		int railW = INVY_LEFT_W - PAD * 2;
		// Fixed starting position — never shifts when switching inventory/equipment/prayer tabs.
		int cy = INVY_TOP_H + PAD;
		// Hard bottom of the painted background rect — nothing should render below this.
		final int railMaxY = INVY_TOP_H + tabH + itemsH;

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();

		SelfState     self   = state.getSelf();
		EffectState   fx     = state.getEffects();
		BoostState    boosts = state.getBoosts();
		PoisonState   poison = state.getPoison();
		OpponentState opp    = state.getOpponent();

		// HP shake — same damped-sine formula as drawHpPrayerBars.
		// Only the HP bar shifts; prayer/run/spec remain stationary.
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

		// ── Self resource bars (icon left, bar right, number below bar) ────────
		int hpBarCy = cy;
		cy = drawInventoryBar(g, fm, cx + shakeX, lx + shakeX, cy, railW, hpSkillIcon,
			self.getCurrentHp(), self.getMaxHp(), HP_FG, HP_BG, activeProfile.hpBarStyle);

		// Transient incoming splat — shown near HP bar for ~750 ms after taking damage
		{
			long inMs  = self.getLastIncomingDamageMs();
			int  inDmg = self.getLastIncomingDamage();
			if (inMs > 0 && inDmg > 0 && System.currentTimeMillis() - inMs < 750)
			{
				String s      = String.valueOf(inDmg);
				int gr        = Math.max(2, fm.getHeight() / 2 - 1);
				int totalW    = gr * 2 + 3 + fm.stringWidth(s);
				int startX    = lx + railW - totalW;
				drawSplatGlyph(g, startX + gr, hpBarCy + fm.getHeight() / 2, gr, RED);
				g.setColor(RED);
				g.drawString(s, startX + gr * 2 + 3, hpBarCy + fm.getAscent());
			}
		}

		cy = drawInventoryBar(g, fm, cx, lx, cy, railW, praySkillIcon,
			self.getCurrentPrayer(), self.getMaxPrayer(), PRAYER_FG, PRAYER_BG, activeProfile.prayerBarStyle);

		cy = drawInventoryBar(g, fm, cx, lx, cy, railW, runEnergyIcon,
			self.getRunEnergy(), 100, RUN_FG, RUN_BG, activeProfile.runBarStyle);
		cy = drawInventoryBar(g, fm, cx, lx, cy, railW, specIcon,
			fx.getSpecEnergy(), 100, YELLOW, HP_BG, activeProfile.specBarStyle);

		// ── Self boost stats (compact: icon + current level, 5 columns) ──
		if (activeProfile.showBoostRow)
		{
			g.setColor(DIVIDER);
			g.drawLine(lx, cy, INVY_LEFT_W - lx, cy);
			cy += 3;

			final int ICN  = 9;
			final int colW = INVY_LEFT_W / 5; // 14 px per column
			int[] boosted = { boosts.getAttackBoosted(), boosts.getStrengthBoosted(),
			                  boosts.getDefenceBoosted(), boosts.getRangedBoosted(),
			                  boosts.getMagicBoosted() };
			int[] real    = { boosts.getAttackReal(), boosts.getStrengthReal(),
			                  boosts.getDefenceReal(), boosts.getRangedReal(),
			                  boosts.getMagicReal() };
			BufferedImage[] bIcons = { atkSkillIcon, strSkillIcon, defSkillIcon,
			                           rngSkillIcon, magSkillIcon };
			for (int i = 0; i < 5; i++)
			{
				int delta = boosted[i] - real[i];
				g.setColor(delta > 0 ? GREEN : delta < 0 ? RED : GRAY);
				int iconX = i * colW + (colW - ICN) / 2;
				BufferedImage icon = debugIcon(bIcons[i]);
				if (icon != null)
					g.drawImage(icon, iconX, cy, ICN, ICN, null);
				else
					g.fillRect(iconX + 2, cy + 2, ICN - 4, ICN - 4); // coloured dot while icon loads
			}
			for (int i = 0; i < 5; i++)
			{
				int delta = boosted[i] - real[i];
				g.setColor(delta > 0 ? GREEN : delta < 0 ? RED : GRAY);
				drawCentered(g, fm, String.valueOf(boosted[i]), i * colW + colW / 2, cy + ICN + fm.getAscent());
			}
			cy += ICN + fm.getHeight() + 2;
		}

		List<ActiveEffectView> selfBuffs = buildActiveEffects(self, fx, poison, boosts);
		if (!selfBuffs.isEmpty())
		{
			g.setColor(DIVIDER);
			g.drawLine(lx, cy, INVY_LEFT_W - lx, cy);
			cy += 3;

			cy = drawEffects2Col(g, selfBuffs, lx, cy, railMaxY);
		}

		// ── Action labels (text-only — no standard icon exists for these) ──────
		if (cy >= railMaxY) return;
		ProtectionState prot = state.getProtection();
		boolean showLocks    = debugRendering || config.showCombatLocks();
		boolean showZoneInfo = debugRendering || config.showZoneInfo();
		boolean hasProt = prot.isLmsImmune()
			|| (showLocks && (prot.isPjSafe() || prot.isInCombatLogoutLock() || prot.isUnderAttackLocked()));
		ManualTimerState t1 = state.getTimer1();
		ManualTimerState t2 = state.getTimer2();
		boolean hasAction = hasProt || t1.isRunning() || t2.isRunning()
			|| (showZoneInfo && (state.getContext().getWildernessLevel() > 0 || state.getContext().isMultiCombat()));

		if (hasAction && cy < railMaxY)
		{
			g.setColor(DIVIDER);
			g.drawLine(lx, cy, INVY_LEFT_W - lx, cy);
			cy += 3;

			if (showLocks && prot.isPjSafe() && cy < railMaxY)
			{
				int s = (int) Math.round(prot.getPjSafeTicksRemaining() * 0.6);
				g.setColor(s > 20 ? GREEN : s > 10 ? YELLOW : RED);
				drawCentered(g, fm, "PJ " + s + "s", cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (prot.isLmsImmune() && cy < railMaxY)
			{
				int s = (int) Math.round(prot.getLmsImmuneTicksRemaining() * 0.6);
				g.setColor(PRAYER_FG);
				drawCentered(g, fm, "IMM " + s + "s", cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (showLocks && prot.isInCombatLogoutLock() && cy < railMaxY)
			{
				g.setColor(ORANGE);
				drawCentered(g, fm, "LOG", cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (showLocks && prot.isUnderAttackLocked() && cy < railMaxY)
			{
				g.setColor(LIGHT_BLUE);
				drawCentered(g, fm, "LCK", cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (t1.isRunning() && cy < railMaxY)
			{
				int s = (int)(t1.getRemainingMs() / 1000);
				g.setColor(s > 30 ? WHITE : RED);
				drawCentered(g, fm, "T1 " + (s / 60) + ":" + String.format("%02d", s % 60),
					cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (t2.isRunning() && cy < railMaxY)
			{
				int s = (int)(t2.getRemainingMs() / 1000);
				g.setColor(s > 30 ? WHITE : RED);
				drawCentered(g, fm, "T2 " + (s / 60) + ":" + String.format("%02d", s % 60),
					cx, cy + fm.getAscent());
				cy += fm.getHeight() + 1;
			}
			if (showZoneInfo && cy < railMaxY)
			{
				int wildLevel = state.getContext().getWildernessLevel();
				if (wildLevel > 0 && cy < railMaxY)
				{
					g.setColor(YELLOW);
					drawCentered(g, fm, "W" + wildLevel, cx, cy + fm.getAscent());
					cy += fm.getHeight() + 1;
				}
				if (state.getContext().isMultiCombat() && cy < railMaxY)
				{
					g.setColor(ORANGE);
					drawCentered(g, fm, "MLT", cx, cy + fm.getAscent());
					cy += fm.getHeight() + 1;
				}
			}
		}

	}

	/**
	 * Draws a skill-icon, bar, and optional number for the inventory-hug left rail.
	 * Icon (9×9) appears left of the bar when loaded; the bar fills the remaining width.
	 * Number is centred below the bar when shown. Returns updated cy.
	 */
	private int drawInventoryBar(Graphics2D g, FontMetrics fm, int cx, int lx, int cy, int railW,
		BufferedImage icon, int current, int max, Color fg, Color bgColor, BarDisplayStyle style)
	{
		if (max <= 0 || !barVisible(style)) return cy;
		float pct   = Math.min(1f, (float) current / max);
		Color barFg = pct > 0.5f ? fg : pct > 0.25f ? YELLOW : RED;
		final int ICN = 9;
		int barX  = icon != null ? lx + ICN + 2 : lx;
		int barSz = railW - (icon != null ? ICN + 2 : 0);
		if (showBar(style) && showNum(style))
		{
			// Combined: taller bar with number overlaid — saves one full text row.
			int barH = Math.max(fm.getHeight(), ICN);
			if (icon != null)
				g.drawImage(icon, lx, cy + (barH - ICN) / 2, ICN, ICN, null);
			g.setColor(bgColor);
			g.fillRect(barX, cy, barSz, barH);
			g.setColor(barFg);
			g.fillRect(barX, cy, Math.max(1, (int)(barSz * pct)), barH);
			drawBarValue(g, fm, String.valueOf(current), barX + 2, cy + fm.getAscent(), WHITE);
			cy += barH + 1;
		}
		else if (showBar(style))
		{
			// When the icon is taller than the thin bar, expand row height to fit it.
			int rowH   = icon != null ? Math.max(BAR_H, ICN) : BAR_H;
			int barOffY = (rowH - BAR_H) / 2;
			if (icon != null)
				g.drawImage(icon, lx, cy + (rowH - ICN) / 2, ICN, ICN, null);
			g.setColor(bgColor);
			g.fillRect(barX, cy + barOffY, barSz, BAR_H);
			g.setColor(barFg);
			g.fillRect(barX, cy + barOffY, Math.max(1, (int)(barSz * pct)), BAR_H);
			cy += rowH + 1;
		}
		else if (showNum(style))
		{
			g.setColor(barFg);
			drawCentered(g, fm, String.valueOf(current), cx, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}
		return cy;
	}


	// ── Layout computation ────────────────────────────────────────────────────

	// Package-private and static so HudLayoutBoundsTest can call them without a RuneLite client.
	static void computeHorizLayout(HudLayoutState layout, Rectangle b, boolean showBoostRow, boolean reserveDock)
	{
		int boostH = (showBoostRow || reserveDock) ? BOOST_ROW_H : 0;
		int mainH  = b.height - ACTION_STRIP_H - boostH;
		int third  = b.width / 3;
		int right  = b.width - third;

		layout.setOpponentPanel(new Rectangle(b.x,         b.y,         third,         mainH));
		layout.setEventPanel   (new Rectangle(b.x + third, b.y,         right - third, mainH));
		layout.setSelfPanel    (new Rectangle(b.x + right, b.y,         third,         mainH));
		layout.setBoostRow(boostH > 0
			? new Rectangle(b.x, b.y + mainH, b.width, BOOST_ROW_H) : null);
		layout.setActionStrip(new Rectangle(b.x, b.y + mainH + boostH, b.width, ACTION_STRIP_H));
		layout.setDirty(false);
	}

	static void computeVertLayout(HudLayoutState layout, Rectangle b, boolean showBoostRow, boolean reserveDock)
	{
		int boostH = (showBoostRow || reserveDock) ? BOOST_ROW_H : 0;
		int mainH  = b.height - ACTION_STRIP_H - boostH;
		// Opponent and event each take ~22% of mainH (⅓ × ⅔ ≈ 33% reduction vs the old ⅓ each).
		// Self panel gets the rest — more room for effect timers.
		int secH   = mainH * 2 / 9;

		layout.setOpponentPanel(new Rectangle(b.x, b.y,            b.width, secH));
		layout.setEventPanel   (new Rectangle(b.x, b.y + secH,     b.width, secH));
		layout.setSelfPanel    (new Rectangle(b.x, b.y + 2 * secH, b.width, mainH - 2 * secH));
		layout.setBoostRow(boostH > 0
			? new Rectangle(b.x, b.y + mainH, b.width, BOOST_ROW_H) : null);
		layout.setActionStrip(new Rectangle(b.x, b.y + mainH + boostH, b.width, ACTION_STRIP_H));
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

		g.setColor(bgColor(activeProfile.backgroundOpacity));
		boolean showBoost = activeProfile.showBoostRow;
		if (!showBoost && boostRow != null)
		{
			// reserveDock mode: leave the boost row band transparent for other overlays.
			g.fillRect(bounds.x, bounds.y, bounds.width, boostRow.y - bounds.y);
			if (strip != null)
				g.fillRect(strip.x, strip.y, strip.width, strip.height);
		}
		else
		{
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}

		if (strip != null)
		{
			g.setColor(STRIP_BG);
			g.fillRect(strip.x, strip.y, strip.width, strip.height);
		}
		if (boostRow != null && showBoost)
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
		drawEventPanel(g, state, layout, normal, small, vertical);
		drawSelfPanel(g, state, layout, normal, small, !vertical);
		drawBoostRow(g, state, layout, small, !vertical);
		drawActionStrip(g, state, layout, small, vertical);
	}

	// ── Opponent panel ────────────────────────────────────────────────────────

	private void drawOpponentPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small)
	{
		Rectangle p = layout.getOpponentPanel();
		if (p == null) return;

		int x  = p.x + PAD;
		int rx = p.x + p.width - PAD;
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

		// Pre-compute prayer icon dimensions to reserve right column.
		g.setFont(normal);
		FontMetrics fm = g.getFontMetrics();
		HeadIcon prayer = opp.getOverheadPrayer();
		int contentBlockH = fm.getHeight() + 4 + BAR_H + 3 + smFm.getHeight() + 2;
		int praySz  = prayer != null ? Math.min(config.opponentPrayerIconSize(), contentBlockH) : 0;
		int prayX   = rx - praySz;
		int contentW = (prayer != null ? prayX - 3 : rx) - x;

		// Name row + ironman badge
		g.setColor(WHITE);
		String name = opp.getName();
		while (name.length() > 1 && fm.stringWidth(name) > contentW)
			name = name.substring(0, name.length() - 1);
		g.drawString(name, x, cy + fm.getAscent());

		OpponentStats.AccountType acctType = opp.getStats().getAccountType();
		if (acctType != OpponentStats.AccountType.NORMAL)
		{
			int badgeSz = fm.getHeight();
			int badgeX  = x + fm.stringWidth(name) + 3;
			BufferedImage badgeIcon = iconForAccountType(acctType);
			if (badgeIcon != null && badgeIcon.getWidth() > 0)
			{
				int bw = badgeIcon.getWidth();
				int bh = badgeIcon.getHeight();
				int drawSz = (bw <= badgeSz && bh <= badgeSz) ? Math.max(bw, bh) : badgeSz;
				g.drawImage(badgeIcon, badgeX, cy + (badgeSz - drawSz) / 2, drawSz, drawSz, null);
			}
			else
			{
				g.setFont(small);
				smFm = g.getFontMetrics();
				g.setColor(acctType == OpponentStats.AccountType.HARDCORE_IRONMAN ? RED : GRAY);
				g.drawString(accountTypeBadge(acctType), badgeX, cy + fm.getAscent());
				g.setFont(normal);
			}
		}
		int nameCy = cy;
		cy += fm.getHeight() + 4;

		// HP bar + HP text (within contentW)
		int estHp = opp.getEstimatedHp();
		int maxHp = opp.getMaxHp();
		g.setFont(small);
		smFm = g.getFontMetrics();
		if (estHp >= 0 && maxHp > 0)
		{
			float pct = Math.min(1f, (float) estHp / maxHp);
			Color fg  = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			int hpBarH = smFm.getHeight(); // tall bar for overlaid text
			g.setColor(HP_BG);
			g.fillRect(x, cy, contentW, hpBarH);
			g.setColor(fg);
			g.fillRect(x, cy, Math.max(1, (int)(contentW * pct)), hpBarH);
			drawBarValueRight(g, smFm, "~" + estHp + " HP", x + contentW - 2, cy + smFm.getAscent(), WHITE);
			cy += hpBarH + 3;
		}
		else
		{
			int hpBarH = smFm.getHeight();
			g.setColor(HP_BG);
			g.fillRect(x, cy, contentW, hpBarH);
			drawBarValueRight(g, smFm, "HP ?", x + contentW - 2, cy + smFm.getAscent(), GRAY);
			cy += hpBarH + 3;
		}

		// Prayer icon: right column, vertically centred on the name+HP block
		if (prayer != null && praySz > 0)
		{
			int blockH    = cy - nameCy;
			int prayIconY = nameCy + Math.max(0, (blockH - praySz) / 2);
			BufferedImage pIcon = prayerIconFor(prayer);
			if (pIcon != null)
				g.drawImage(pIcon, prayX, prayIconY, praySz, praySz, null);
			else
			{
				g.setColor(prayerColorFor(prayer));
				g.drawString(prayerShortFor(prayer), prayX, prayIconY + smFm.getAscent());
			}
		}

		// Confirmed hitsplat indicator — factual only, ~750 ms window.
		{
			long outMs  = opp.getLastOutgoingHitMs();
			int  outDmg = opp.getLastOutgoingHit();
			if (outMs > 0 && outDmg > 0 && System.currentTimeMillis() - outMs < 750)
			{
				int   splatW = 16;
				int   splatH = 16;
				int   splatX = x + contentW - splatW - 2;
				int   splatY = cy - splatH - 6;
				if (redHitsplatIcon != null)
					g.drawImage(redHitsplatIcon, splatX, splatY, splatW, splatH, null);
				else
				{
					int gr = splatW / 2 - 1;
					drawSplatGlyph(g, splatX + splatW / 2, splatY + splatH / 2, gr, OUT_CYAN);
				}
				String numStr = String.valueOf(outDmg);
				int    numX   = splatX - smFm.stringWidth(numStr) - 2;
				int    numY   = splatY + splatH / 2 + smFm.getAscent() / 2;
				drawOutlinedString(g, numStr, numX, numY, WHITE);
			}
		}

		// Opponent base stats — prayer now occupies only the right column so stats fit.
		cy += 2;
		int statsRowH = ICON_SIZE + 2 + smFm.getAscent();
		if (cy + statsRowH <= p.y + p.height - PAD)
		{
			OpponentStats stats = opp.getStats();
			int col   = p.width / 5;
			int c1    = p.x + col / 2;
			int c2    = c1 + col;
			int c3    = c2 + col;
			int c4    = c3 + col;
			int c5    = c4 + col;
			int textY = cy + ICON_SIZE + 2 + smFm.getAscent();
			drawStatColumn(g, smFm, atkSkillIcon, "ATK", stats.getAttack(),   c1, cy, textY);
			drawStatColumn(g, smFm, strSkillIcon, "STR", stats.getStrength(), c2, cy, textY);
			drawStatColumn(g, smFm, defSkillIcon, "DEF", stats.getDefence(),  c3, cy, textY);
			drawStatColumn(g, smFm, rngSkillIcon, "RNG", stats.getRanged(),   c4, cy, textY);
			drawStatColumn(g, smFm, magSkillIcon, "MAG", stats.getMagic(),    c5, cy, textY);
		}
	}

	private BufferedImage iconForAccountType(OpponentStats.AccountType type)
	{
		switch (type)
		{
			case IRONMAN:          return ironmanIcon;
			case ULTIMATE_IRONMAN: return ultimateIronmanIcon;
			case HARDCORE_IRONMAN: return hardcoreIronmanIcon;
			default:               return null;
		}
	}

	private static String accountTypeBadge(OpponentStats.AccountType type)
	{
		switch (type)
		{
			case IRONMAN:          return "IM";
			case ULTIMATE_IRONMAN: return "UIM";
			case HARDCORE_IRONMAN: return "HC";
			default:               return "";
		}
	}

	// ── Event panel ───────────────────────────────────────────────────────────

	private static final Color GRAY_DIM = new Color(100, 100, 100);

	/**
	 * Formats the damage label for a single exchange (one direction).
	 * Single hit: "34". Two hits: "20+8". Three or more: "42 (3)".
	 * Returns "" when there are no hits in that direction.
	 * Package-visible for unit testing.
	 */
	static String formatExchangeDamage(DamageExchange ex, boolean outgoing)
	{
		int count = outgoing ? ex.getOutgoingHitCount() : ex.getIncomingHitCount();
		int total = outgoing ? ex.getOutgoingTotal()    : ex.getIncomingTotal();
		if (count == 0) return "";
		if (count == 1)
			return String.valueOf(outgoing ? ex.getOutgoingHit(0) : ex.getIncomingHit(0));
		if (count == 2)
			return (outgoing ? ex.getOutgoingHit(0) : ex.getIncomingHit(0))
				+ "+" + (outgoing ? ex.getOutgoingHit(1) : ex.getIncomingHit(1));
		return total + " (" + count + ")";
	}

	/**
	 * Number of exchange rows shown in the fight panel depending on orientation.
	 * Vertical Float uses 2 (less height); horizontal layouts use 3.
	 * Package-visible for unit testing.
	 */
	static int exchangeRowCount(boolean vertical)
	{
		return vertical ? 2 : 3;
	}

	private void drawEventPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small, boolean vertical)
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

		// Column centres
		int outX = p.x + p.width / 4;      // centre of left column
		int inX  = p.x + 3 * p.width / 4;  // centre of right column

		// Text column headers
		g.setColor(OUT_CYAN);
		drawCentered(g, smFm, "DEALT", outX, cy + smFm.getAscent());
		g.setColor(RED);
		drawCentered(g, smFm, "TAKEN", inX, cy + smFm.getAscent());
		// vertical divider
		g.setColor(VERT_DIV);
		g.drawLine(mid, cy - 1, mid, p.y + p.height - PAD * 2 - smFm.getHeight());
		cy += smFm.getHeight() + 2;

		PvpFightSession session = state.getCurrentSession();
		List<DamageExchange> exchanges = session != null
			? session.getRecentExchanges()
			: java.util.Collections.emptyList();

		g.setFont(normal);
		FontMetrics fmNorm = g.getFontMetrics();
		g.setFont(small);
		FontMetrics fmSm = g.getFontMetrics();

		int maxRows = exchangeRowCount(vertical);
		int rowH    = fmNorm.getHeight() + 1;
		for (int row = 0; row < maxRows && row < exchanges.size(); row++)
		{
			if (cy + rowH > p.y + p.height - PAD * 2 - smFm.getHeight()) break;
			boolean isFirst = row == 0;
			FontMetrics fm = isFirst ? fmNorm : fmSm;
			g.setFont(isFirst ? normal : small);

			DamageExchange ex = exchanges.get(row);

			if (ex.hasOutgoing())
			{
				String outLabel = formatExchangeDamage(ex, true);
				if (ex.getOutgoingPrayerDrain() > 0)
					outLabel += " -" + ex.getOutgoingPrayerDrain() + "p";
				g.setColor(isFirst ? OUT_CYAN : GRAY_DIM);
				drawCentered(g, fm, outLabel, outX, cy + fm.getAscent());
			}

			if (ex.hasIncoming())
			{
				g.setColor(isFirst ? RED : GRAY_DIM);
				drawCentered(g, fm, formatExchangeDamage(ex, false), inX, cy + fm.getAscent());
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

		// Totals row at the bottom (live session or last session) — splat glyph + number
		int totalDealt    = session != null ? session.getTotalOutgoing()  : state.getLastTotalOutgoing();
		int totalReceived = session != null ? session.getTotalIncoming() : state.getLastTotalIncoming();
		if (totalDealt > 0 || totalReceived > 0)
		{
			g.setFont(small);
			smFm = g.getFontMetrics();
			int divY = p.y + p.height - PAD - smFm.getHeight() - 3;
			g.setColor(VERT_DIV);
			g.drawLine(p.x + PAD, divY, p.x + p.width - PAD, divY);

			int totY = p.y + p.height - PAD;

			if (totalDealt > 0)
			{
				String s = String.valueOf(totalDealt);
				g.setColor(OUT_CYAN);
				g.drawString(s, outX - smFm.stringWidth(s) / 2, totY);
			}
			if (totalReceived > 0)
			{
				String s = String.valueOf(totalReceived);
				g.setColor(RED);
				g.drawString(s, inX - smFm.stringWidth(s) / 2, totY);
			}
		}
	}

	// ── Self panel ────────────────────────────────────────────────────────────

	private void drawSelfPanel(Graphics2D g, PvpHudState state, HudLayoutState layout,
		Font normal, Font small, boolean horizontal)
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
		PoisonState poison = state.getPoison();

		// Both modes: when boost row is enabled, draw a left column of 5 boost stats
		// alongside the bars. Effects move to the dedicated boost row strip.
		final int BOOST_COL_W = 40;
		if (activeProfile.showBoostRow)
		{
			drawBoostStack(g, state, lx, cy, BOOST_COL_W, p.y + p.height - PAD, small);
			g.setFont(small);
			smFm = g.getFontMetrics();
		}

		// Bar width: leave room for the boost column when present; cap horizontal bars
		// at 90px so they don't sprawl across a wide chatbox panel.
		int hpBarPanelW;
		if (activeProfile.showBoostRow)
		{
			int boostEndX = lx + BOOST_COL_W + PAD;   // first px right of the boost column
			int available = rx - boostEndX;
			hpBarPanelW = horizontal
				? Math.min(available, 90) + PAD * 2
				: available + PAD * 2;                 // full available width in vertical
		}
		else if (horizontal)
		{
			hpBarPanelW = Math.min(p.width, 90 + PAD * 2);
		}
		else
		{
			hpBarPanelW = p.width;                     // full width, no boost column
		}
		cy = drawHpPrayerBars(g, smFm, self, fx, rx, cy, hpBarPanelW);

		// Vertical mode: effects always stay in the self panel (secondary slots like
		// menaphite_next only render correctly in text/vert-bar styles).
		// Horizontal+boost: effects move to the boost row strip instead.
		if (!horizontal || !activeProfile.showBoostRow)
		{
			List<ActiveEffectView> effects = buildActiveEffects(self, fx, poison, boosts);
			if (!effects.isEmpty())
			{
				int maxY = p.y + p.height - PAD;
				BuffStyle style = activeProfile.buffStyle;
				if (style == BuffStyle.VERTICAL_BAR)
					drawEffectsVertBar(g, effects, lx, cy, maxY, debugRendering);
				else if (style == BuffStyle.ICON_TRAY)
					drawEffectsIconTray(g, effects, lx, cy, p.x + p.width - PAD, debugRendering);
				else
					drawEffectsText(g, normal, effects, lx, cy, maxY);
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
		int barW = panelWidth - PAD * 2; // callers set panelWidth to control max width

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
		int tallH = fm.getHeight(); // bar height when number is overlaid

		// ── HP ──────────────────────────────────────────────────────────────────
		BarDisplayStyle hpStyle = activeProfile.hpBarStyle;
		if (self.getMaxHp() > 0 && barVisible(hpStyle))
		{
			float pct = Math.min(1f, (float) self.getCurrentHp() / self.getMaxHp());
			Color fg   = pct > 0.5f ? HP_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar(hpStyle) && showNum(hpStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, tallH);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), tallH);
				drawBarValueRight(g, fm, "HP " + self.getCurrentHp() + "/" + self.getMaxHp(),
					effectiveRx - 2, cy + fm.getAscent(), WHITE);
				cy += tallH + 1;
			}
			else if (showBar(hpStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				cy += BAR_H + 1;
			}
			else // NUMBERS_ONLY
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "HP " + self.getCurrentHp() + "/" + self.getMaxHp(),
					effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Prayer ───────────────────────────────────────────────────────────────
		BarDisplayStyle prStyle = activeProfile.prayerBarStyle;
		if (self.getMaxPrayer() > 0 && barVisible(prStyle))
		{
			float pct = Math.min(1f, (float) self.getCurrentPrayer() / self.getMaxPrayer());
			Color fg   = pct > 0.5f ? PRAYER_FG : pct > 0.25f ? YELLOW : RED;
			if (showBar(prStyle) && showNum(prStyle))
			{
				g.setColor(PRAYER_BG);
				g.fillRect(barX, cy, barW, tallH);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), tallH);
				drawBarValueRight(g, fm, "PR " + self.getCurrentPrayer() + "/" + self.getMaxPrayer(),
					effectiveRx - 2, cy + fm.getAscent(), WHITE);
				cy += tallH + 1;
			}
			else if (showBar(prStyle))
			{
				g.setColor(PRAYER_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(fg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * pct)), BAR_H);
				cy += BAR_H + 1;
			}
			else
			{
				g.setColor(fg);
				drawRightAligned(g, fm, "PR " + self.getCurrentPrayer() + "/" + self.getMaxPrayer(),
					effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Run energy ───────────────────────────────────────────────────────────
		BarDisplayStyle runStyle = activeProfile.runBarStyle;
		if (barVisible(runStyle))
		{
			int run = self.getRunEnergy();
			float runPct = Math.min(1f, run / 100f);
			Color runFg  = RUN_FG;
			if (showBar(runStyle) && showNum(runStyle))
			{
				g.setColor(RUN_BG);
				g.fillRect(barX, cy, barW, tallH);
				g.setColor(runFg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * runPct)), tallH);
				drawBarValueRight(g, fm, "RUN " + run + "%", effectiveRx - 2, cy + fm.getAscent(), WHITE);
				cy += tallH + 1;
			}
			else if (showBar(runStyle))
			{
				g.setColor(RUN_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(runFg);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * runPct)), BAR_H);
				cy += BAR_H + 1;
			}
			else
			{
				g.setColor(runFg);
				drawRightAligned(g, fm, "RUN " + run + "%", effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		// ── Spec ─────────────────────────────────────────────────────────────────
		BarDisplayStyle specStyle = activeProfile.specBarStyle;
		if (barVisible(specStyle))
		{
			int spec = fx.getSpecEnergy();
			float specPct = spec / 100f;
			if (showBar(specStyle) && showNum(specStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, tallH);
				g.setColor(YELLOW);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * specPct)), tallH);
				drawBarValueRight(g, fm, "SPEC " + spec + "%", effectiveRx - 2, cy + fm.getAscent(), WHITE);
				cy += tallH + 1;
			}
			else if (showBar(specStyle))
			{
				g.setColor(HP_BG);
				g.fillRect(barX, cy, barW, BAR_H);
				g.setColor(YELLOW);
				g.fillRect(barX, cy, Math.max(1, (int) (barW * specPct)), BAR_H);
				cy += BAR_H + 1;
			}
			else
			{
				g.setColor(YELLOW);
				drawRightAligned(g, fm, "SPEC " + spec + "%", effectiveRx, cy + fm.getAscent());
				cy += fm.getHeight() + 2;
			}
		}

		return cy;
	}

	// ── Active effect builder (single source of truth for all buff styles) ──────

	/**
	 * Projects current state into render-ready effect views via {@link StatusProjector}.
	 * Replaces the old buildActiveEffects() inline logic.
	 */
	private List<ActiveEffectView> buildActiveEffects(SelfState self, EffectState fx,
		PoisonState poison, BoostState boosts)
	{
		return flattenToViews(StatusProjector.project(
			self, fx, poison, boosts, client.getTickCount(), config, debugRendering));
	}

	/**
	 * Converts a {@link StatusSlot} list to render-ready {@link ActiveEffectView}s by
	 * resolving icons via {@link #getIconFor}.  Reuses {@code effectScratch} as the
	 * backing list so callers must consume the result before the next render frame.
	 */
	private List<ActiveEffectView> flattenToViews(List<StatusSlot> slots)
	{
		effectScratch.clear();
		for (StatusSlot s : slots)
			effectScratch.add(new ActiveEffectView(
				s.slotId, getIconFor(s.slotId), s.label, s.textLabel, s.color, s.highlightColor,
				s.priority, s.isSecondary));
		return effectScratch;
	}

	/**
	 * Resolves a {@link StatusSlot#slotId} to its cached icon.
	 * Returns {@code null} if the slot has no icon or the icon has not loaded yet.
	 */
	private BufferedImage getIconFor(String slotId)
	{
		switch (slotId)
		{
			case "freeze_rush":        return debugIcon(iceRushIcon);
			case "freeze_burst":       return debugIcon(iceBurstIcon);
			case "freeze_blitz":       return debugIcon(iceBlitzIcon);
			case "freeze_barrage":     return debugIcon(iceBarrageIcon);
			case "freeze_imm":         return debugIcon(iceBarrageIcon);
			case "tb":                 return debugIcon(tbIcon);
			case "tb_imm":             return debugIcon(tbIcon);
			case "venom":              return debugIcon(venomIcon);
			case "poison":             return debugIcon(poisonIcon);
			case "corrupt":            return debugIcon(corruptIcon);
			case "corrupt_next":       return debugIcon(corruptIcon);
			case "sotd":               return debugIcon(sotdIcon);
			case "ward":               return debugIcon(wardIcon);
			case "ward_cd":            return debugIcon(wardIcon);
			case "mark":               return debugIcon(markIcon);
			case "mark_cd":            return debugIcon(markIcon);
			case "anti_venom":         return debugIcon(antiVenomItemIcon);
			case "anti_poison":        return debugIcon(antiPoisonItemIcon);
			case "dsc":                return debugIcon(dscIcon);
			case "drg":                return debugIcon(drgIcon);
			case "dmg":                return debugIcon(dmgIcon);
			case "bas":                return debugIcon(basIcon);
			case "btm":                return debugIcon(btmIcon);
			case "dsa":                return debugIcon(dsaIcon);
			case "dss":                return debugIcon(dssIcon);
			case "dsd":                return debugIcon(dsdIcon);
			case "menaphite":          return debugIcon(menIcon);
			case "menaphite_next":     return debugIcon(menIcon);
			case "prayer_regen":       return debugIcon(prayerRegenIcon);
			case "prayer_regen_next":  return debugIcon(prayerRegenIcon);
			case "stamina":            return debugIcon(staminaIcon);
			case "af":                 return debugIcon(antifireIcon);
			case "saf":                return debugIcon(superAntifireIcon);
			case "ih_cd":              return debugIcon(imbuedHeartIcon);
			case "surge":              return debugIcon(surgeIcon);
			default:                   return null; // plock, burn, burn_next, drain, restore, venom, poison
		}
	}

	/**
	 * Headless effect builder for offline snapshots and tests.
	 * Shows every tracked effect regardless of config; uses null icons (text-only rendering).
	 */
	static List<ActiveEffectView> buildEffectsHeadless(PvpHudState state, int tick)
	{
		List<StatusSlot> slots = StatusProjector.project(
			state.getSelf(), state.getEffects(), state.getPoison(), state.getBoosts(),
			tick, null, true);
		return flattenHeadless(slots);
	}

	private static List<ActiveEffectView> flattenHeadless(List<StatusSlot> slots)
	{
		List<ActiveEffectView> out = new ArrayList<>(slots.size());
		for (StatusSlot s : slots)
			out.add(new ActiveEffectView(s.slotId, null, s.label, s.textLabel, s.color,
				s.highlightColor, s.priority, s.isSecondary));
		return out;
	}

	/**
	 * In debug mode, always returns null so all effects fall back to text labels.
	 * AsyncBufferedImage from ItemManager.getImage() is non-null but transparent until the
	 * game cache loads — pixel sampling is unreliable outside a live session.
	 */
	private BufferedImage debugIcon(BufferedImage img)
	{
		return debugRendering ? null : img;
	}

	private static void drawIconHighlight(Graphics2D g, Color c, int x, int y, int size)
	{
		if (c == null) return;
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
		g.fillRect(x - 1, y - 1, size + 2, size + 2);
		g.setColor(c);
		g.drawRect(x - 1, y - 1, size + 2, size + 2);
	}

	private Color bgColor(int alpha)
	{
		Color base = config.hudBackgroundColor();
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	// ── TEXT: one label per line, colour-coded ────────────────────────────────

	static int drawEffectsText(Graphics2D g, Font font, List<ActiveEffectView> effects, int lx, int cy, int maxY)
	{
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		StatusPriority lastPriority = null;
		for (ActiveEffectView e : effects)
		{
			if (lastPriority != null && e.priority != lastPriority)
			{
				if (cy + 3 > maxY) break;
				cy += 3;
			}
			lastPriority = e.priority;
			if (cy + fm.getHeight() + 1 > maxY) break;
			g.setColor(e.color);
			int indent = e.isSecondary ? 6 : 0;
			g.drawString(e.getTextLabel(), lx + indent, cy + fm.getAscent());
			cy += fm.getHeight() + 1;
		}
		return cy;
	}

	// ── VERTICAL_BAR: icon + optional timer, one per row ────────────────────

	static int drawEffectsVertBar(Graphics2D g, List<ActiveEffectView> effects, int lx, int cy, int maxY,
		boolean textFallback)
	{
		FontMetrics fm = g.getFontMetrics();
		int lineH = Math.max(ICON_SIZE, fm.getHeight()) + 2;
		StatusPriority lastPriority = null;
		for (ActiveEffectView e : effects)
		{
			if (lastPriority != null && e.priority != lastPriority)
			{
				if (cy + 3 > maxY) break;
				cy += 3;
			}
			lastPriority = e.priority;
			if (cy + lineH > maxY) break;
			if (e.icon == null)
			{
				if (textFallback)
				{
					g.setColor(e.color);
					int indent = e.isSecondary ? ICON_SIZE + ICON_GAP : 0;
					g.drawString(e.getTextLabel(), lx + indent, cy + fm.getAscent());
					cy += lineH;
				}
				continue;
			}
			int iconY = cy + (lineH - ICON_SIZE) / 2;
			drawIconHighlight(g, e.highlightColor, lx, iconY, ICON_SIZE);
			g.drawImage(e.icon, lx, iconY, ICON_SIZE, ICON_SIZE, null);
			if (!e.label.isEmpty())
			{
				g.setColor(e.color);
				g.drawString(e.label, lx + ICON_SIZE + ICON_GAP, cy + fm.getAscent());
			}
			cy += lineH;
		}
		return cy;
	}

	// ── 2-COLUMN GRID: icon above timer, two entries per row (Inventory Hug) ──
	//
	// Compound parent/child pairs (e.g. menaphite + menaphite_next) are folded into
	// a single full-width cell: icon centred, primary label on line 1, secondary on
	// line 2.  Non-compound primaries pair two-per-row as before.
	// ICON_TRAY still shows primary only (secondaries always skipped there).

	/**
	 * Maps a primary slot ID to the slot ID of its associated secondary child, or
	 * {@code null} if the slot has no child.  Used by {@link #drawEffects2Col} to
	 * detect and fold compound pairs into one cell.
	 */
	static String childSlotIdFor(String parentSlotId)
	{
		if (parentSlotId == null) return null;
		switch (parentSlotId)
		{
			case "menaphite":    return "menaphite_next";
			case "prayer_regen": return "prayer_regen_next";
			case "corrupt":      return "corrupt_next";
			case "burn":         return "burn_next";
			default:             return null;
		}
	}

	/**
	 * Returns true if a parent slot should always occupy compound-cell height from the
	 * moment it becomes active, even before its next-proc child timer is first known.
	 * This prevents visible geometry snapping when the next-proc phase synchronises.
	 */
	static boolean reservesSecondaryLine(String parentSlotId)
	{
		if (parentSlotId == null) return false;
		switch (parentSlotId)
		{
			case "menaphite":    return true;
			case "prayer_regen": return true;
			default:             return false;
		}
	}

	/**
	 * Returns true if the child represents a beneficial periodic proc timer.
	 * These secondary lines use a stable neutral color instead of urgency-scaled red.
	 */
	static boolean isBeneficialPeriodicChild(String childSlotId)
	{
		if (childSlotId == null) return false;
		switch (childSlotId)
		{
			case "menaphite_next":    return true;
			case "prayer_regen_next": return true;
			default:                  return false;
		}
	}

	/**
	 * Draws a tiny circular-refresh glyph (↻) using an arc and a two-line arrowhead.
	 * Safe to call every frame; performs no image allocation.
	 *
	 * @param cx   horizontal centre of the glyph
	 * @param cy   vertical centre of the glyph
	 * @param size diameter in pixels
	 */
	static void drawCycleGlyph(Graphics2D g, int cx, int cy, int size, Color color)
	{
		if (size < 4) return;
		g.setColor(color);
		int r = size / 2;
		// 300° arc from 60° (upper-right), sweeping clockwise on screen, ending at east (0°).
		// In AWT, positive arcAngle = CCW in standard math coords = CW on screen (y-down).
		g.drawArc(cx - r, cy - r, size, size, 60, 300);
		// Arrowhead at arc end (east = right side of circle), pointing south (clockwise direction).
		g.drawLine(cx + r, cy, cx + r - 2, cy - 3);
		g.drawLine(cx + r, cy, cx + r + 2, cy - 3);
	}

	/**
	 * Draws 8 radial half-lines from centre, plus a small filled centre square.
	 * Reads as "damage splat" rather than an asterisk.
	 * Used as the damage-type glyph for outgoing (OUT_CYAN) and incoming (RED) hits.
	 */
	static void drawSplatGlyph(Graphics2D g, int cx, int cy, int r, Color color)
	{
		if (r < 2) return;
		g.setColor(color);
		for (int i = 0; i < 8; i++)
		{
			double angle = Math.toRadians(i * 45);
			int dx = (int) Math.round(Math.cos(angle) * r);
			int dy = (int) Math.round(Math.sin(angle) * r);
			g.drawLine(cx, cy, cx + dx, cy + dy);
		}
		g.fillRect(cx - 1, cy - 1, 2, 2);
	}

	/**
	 * Draws a cycle glyph followed by {@code text}, horizontally centred at {@code cx}.
	 * Used for beneficial periodic secondary timers (Menaphite, Prayer Regen).
	 */
	private static void drawBeneficialSecondaryLine(Graphics2D g, FontMetrics fm,
		String text, int cx, int lineTopY, Color color)
	{
		int glyphSz = Math.max(4, fm.getHeight() - 4);
		int gap     = 2;
		int textW   = fm.stringWidth(text);
		int totalW  = glyphSz + gap + textW;
		int startX  = cx - totalW / 2;
		int glyphCx = startX + glyphSz / 2;
		int glyphCy = lineTopY + fm.getHeight() / 2;
		drawCycleGlyph(g, glyphCx, glyphCy, glyphSz, color);
		g.setColor(color);
		g.drawString(text, startX + glyphSz + gap, lineTopY + fm.getAscent());
	}

	/**
	 * Returns a compact label for a secondary child entry suitable for the 2-col
	 * Inventory Hug cell where the parent context is already visible via icon.
	 * Beneficial periodic children (menaphite, prayer_regen) return just the raw timer;
	 * the renderer draws a cycle glyph beside it.  Harmful children strip the redundant
	 * parent-name prefix.  Unknown children fall back to {@link ActiveEffectView#label}.
	 */
	static String compactSecondaryLabel(String childSlotId, ActiveEffectView child)
	{
		String lbl = child.label != null ? child.label : "";
		if (childSlotId == null) return lbl;
		switch (childSlotId)
		{
			case "menaphite_next":    return lbl;   // "0:09"; cycle glyph drawn by renderer
			case "prayer_regen_next": return lbl;   // "0:04"; cycle glyph drawn by renderer
			case "corrupt_next":      return lbl;                    // "-3 5t" (already compact)
			case "burn_next":
				// child.label is "BURN+N"; strip "BURN" → "+N"
				return lbl.startsWith("BURN+") ? lbl.substring(4) : lbl;
			default:                  return lbl;
		}
	}

	static int drawEffects2Col(Graphics2D g, List<ActiveEffectView> effects, int lx, int cy, int maxY)
	{
		FontMetrics fm    = g.getFontMetrics();
		final int colW    = (INVY_LEFT_W - 2 * PAD) / 2; // 30px per column
		final int iconGap = 2;
		final int lineH   = fm.getHeight();
		final int cellH   = ICON_SIZE + iconGap + lineH + 2;
		final int compH   = ICON_SIZE + iconGap + lineH * 2 + 2; // compound: two text lines
		int col = 0;
		StatusPriority lastPriority = null;

		for (int i = 0; i < effects.size(); i++)
		{
			ActiveEffectView e = effects.get(i);
			if (e.isSecondary) continue; // already consumed by its parent's lookahead

			// Look ahead: does the immediately-next entry belong to this one as a child?
			ActiveEffectView child = null;
			String expectedChildId = childSlotIdFor(e.slotId);
			if (expectedChildId != null
				&& i + 1 < effects.size()
				&& effects.get(i + 1).isSecondary
				&& expectedChildId.equals(effects.get(i + 1).slotId))
			{
				child = effects.get(i + 1);
				i++; // consume the child so the outer loop skips it
			}

			// Priority-group separator
			if (lastPriority != null && e.priority != lastPriority)
			{
				if (col == 1) { cy += cellH; col = 0; }
				if (cy + 3 > maxY) break;
				cy += 3;
			}
			lastPriority = e.priority;

			// Stable compound: menaphite/prayer_regen always reserve secondary space so
			// neighboring statuses don't shift when the next-proc phase first synchronises.
			boolean isCompound = (child != null) || reservesSecondaryLine(e.slotId);
			if (isCompound)
			{
				// Force to new row before checking height (compound always full-width)
				if (col == 1) { cy += cellH; col = 0; }
				if (cy + compH > maxY)
				{
					if (cy + cellH > maxY) break;
					// Tight space: discard compound reservation and render primary only
					isCompound = false;
					child = null;
				}
			}

			if (isCompound)
			{
				// Full compound cell — always advances by compH regardless of child presence
				if (e.icon != null)
				{
					int iconX = lx + (2 * colW - ICON_SIZE) / 2;
					drawIconHighlight(g, e.highlightColor, iconX, cy, ICON_SIZE);
					g.drawImage(e.icon, iconX, cy, ICON_SIZE, ICON_SIZE, null);
				}
				// Primary label — line 1
				if (!e.label.isEmpty())
				{
					g.setColor(e.color);
					drawCentered(g, fm, e.label, lx + colW,
						cy + ICON_SIZE + iconGap + fm.getAscent());
				}
				// Secondary line — line 2: glyph+timer for beneficial, plain for harmful, blank when unknown
				if (child != null)
				{
					String secLabel = compactSecondaryLabel(child.slotId, child);
					if (isBeneficialPeriodicChild(child.slotId))
					{
						if (!secLabel.isEmpty())
							drawBeneficialSecondaryLine(g, fm, secLabel, lx + colW,
								cy + ICON_SIZE + iconGap + lineH, BENEFICIAL_PROC);
					}
					else if (!secLabel.isEmpty())
					{
						g.setColor(child.color);
						drawCentered(g, fm, secLabel, lx + colW,
							cy + ICON_SIZE + iconGap + lineH + fm.getAscent());
					}
				}
				// child == null → secondary line reserved but blank (proc phase unknown)
				cy += compH;
				// col stays 0 — compound row always ends flush
			}
			else if (e.icon == null)
			{
				// Text-only entry: one per row, centred
				if (col == 1) { cy += cellH; col = 0; }
				if (cy + lineH + 2 > maxY) break;
				g.setColor(e.color);
				drawCentered(g, fm, e.label, lx + colW, cy + fm.getAscent());
				cy += lineH + 2;
			}
			else
			{
				// Normal icon entry: two per row
				if (col == 0 && cy + cellH > maxY) break;
				int x     = lx + col * colW;
				int iconX = x + (colW - ICON_SIZE) / 2;
				drawIconHighlight(g, e.highlightColor, iconX, cy, ICON_SIZE);
				g.drawImage(e.icon, iconX, cy, ICON_SIZE, ICON_SIZE, null);
				if (!e.label.isEmpty())
				{
					g.setColor(e.color);
					drawCentered(g, fm, e.label, x + colW / 2,
						cy + ICON_SIZE + iconGap + fm.getAscent());
				}
				col++;
				if (col == 2) { col = 0; cy += cellH; }
			}
		}
		if (col == 1) cy += cellH;
		return cy;
	}

	// ── ICON_TRAY: horizontal strip, icon + optional timer below ─────────────

	static void drawEffectsIconTray(Graphics2D g, List<ActiveEffectView> effects, int lx, int cy, int maxX,
		boolean textFallback)
	{
		FontMetrics fm = g.getFontMetrics();
		int maxLabelW = 0;
		for (ActiveEffectView e : effects)
			if (!e.isSecondary && !e.label.isEmpty())
				maxLabelW = Math.max(maxLabelW, fm.stringWidth(e.label));
		int slotW = Math.max(ICON_SIZE, maxLabelW) + ICON_GAP;
		int col   = 0;
		StatusPriority lastPriority = null;
		for (ActiveEffectView e : effects)
		{
			if (e.isSecondary) continue; // next-proc sub-entries not shown in tray
			if (lastPriority != null && e.priority != lastPriority)
				col++; // extra gap slot between priority groups
			lastPriority = e.priority;
			if (lx + col * slotW + ICON_SIZE > maxX) break;
			if (e.icon == null)
			{
				if (textFallback)
				{
					g.setColor(e.color);
					g.drawString(e.label.isEmpty() ? "?" : e.label,
						lx + col * slotW, cy + fm.getAscent());
					col++;
				}
				continue;
			}
			int x = lx + col * slotW;
			drawIconHighlight(g, e.highlightColor, x, cy, ICON_SIZE);
			g.drawImage(e.icon, x, cy, ICON_SIZE, ICON_SIZE, null);
			if (!e.label.isEmpty())
			{
				g.setColor(e.color);
				drawCentered(g, fm, e.label, x + ICON_SIZE / 2, cy + ICON_SIZE + fm.getAscent());
			}
			col++;
		}
	}

	// ── Boost row ─────────────────────────────────────────────────────────────

	private void drawBoostRow(Graphics2D g, PvpHudState state, HudLayoutState layout, Font small, boolean horizontal)
	{
		if (!activeProfile.showBoostRow) return;
		Rectangle p = layout.getBoostRow();
		if (p == null) return;

		// Horizontal mode: boost stats are in the self panel's left column so this
		// strip shows active effects as an icon tray instead.
		// Vertical mode: effects remain in the self panel; nothing to render here.
		if (!horizontal) return;

		SelfState   self   = state.getSelf();
		EffectState fx     = state.getEffects();
		BoostState  boosts = state.getBoosts();
		PoisonState poison = state.getPoison();
		List<ActiveEffectView> effects = buildActiveEffects(self, fx, poison, boosts);
		if (!effects.isEmpty())
		{
			g.setFont(small);
			FontMetrics fm2 = g.getFontMetrics();
			int cy   = p.y + Math.max(1, (p.height - ICON_SIZE - fm2.getHeight()) / 2);
			int lx   = p.x + PAD;
			int maxX = p.x + p.width - PAD;
			drawEffectsIconTray(g, effects, lx, cy, maxX, debugRendering);
		}
	}

	/**
	 * Draws 5 boost-stat rows (icon + current level) stacked vertically in a narrow left column.
	 * Used in both horizontal and vertical float layouts; the boost row strip shows effects instead.
	 */
	private void drawBoostStack(Graphics2D g, PvpHudState state, int lx, int cy, int colW,
		int maxY, Font small)
	{
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		final int ICN = 13;
		int rowH = Math.max(ICN, fm.getHeight()) + 2;
		BoostState boosts = state.getBoosts();
		int[]           boosted = { boosts.getAttackBoosted(), boosts.getStrengthBoosted(),
		                            boosts.getDefenceBoosted(), boosts.getRangedBoosted(),
		                            boosts.getMagicBoosted() };
		int[]           real    = { boosts.getAttackReal(),    boosts.getStrengthReal(),
		                            boosts.getDefenceReal(),   boosts.getRangedReal(),
		                            boosts.getMagicReal()    };
		BufferedImage[] icons   = { atkSkillIcon, strSkillIcon, defSkillIcon,
		                            rngSkillIcon, magSkillIcon };
		for (int i = 0; i < 5 && cy + rowH <= maxY; i++)
		{
			int delta = boosted[i] - real[i];
			g.setColor(delta > 0 ? GREEN : delta < 0 ? RED : GRAY);
			BufferedImage icon = debugIcon(icons[i]);
			int iconY = cy + (rowH - ICN) / 2;
			if (icon != null)
				g.drawImage(icon, lx, iconY, ICN, ICN, null);
			else
				g.fillRect(lx + 1, iconY + 1, ICN - 2, ICN - 2);
			String val = activeProfile.boostXOverX
				? boosted[i] + "/" + real[i]
				: String.valueOf(boosted[i]);
			g.drawString(val, lx + ICN + 2, cy + fm.getAscent());
			cy += rowH;
		}
	}

	private void drawBoostColumns(Graphics2D g, PvpHudState state, Rectangle p, Font small)
	{
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
		String label = activeProfile.boostXOverX
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

		ActionClockState clock  = state.getActionClock();
		int              ctick  = client.getTickCount();
		int atk    = clock.getAttackDelayTicks(ctick);
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
				String lbl = "T2 " + (s / 60) + ":" + String.format("%02d", s % 60);
				g.drawString(lbl, textX, baseline);
				textX += fm.stringWidth(lbl) + 6;
			}
			ProtectionState prot     = state.getProtection();
			boolean         showLcks = debugRendering || config.showCombatLocks();
			if (showLcks && prot.isPjSafe() && textX < textRx)
			{
				int s = (int) Math.round(prot.getPjSafeTicksRemaining() * 0.6);
				String lbl = "PJ " + s;
				g.setColor(s > 20 ? GREEN : s > 10 ? YELLOW : RED);
				g.drawString(lbl, textX, baseline);
				textX += fm.stringWidth(lbl) + 6;
			}
			if (prot.isLmsImmune() && textX < textRx)
			{
				int s = (int) Math.round(prot.getLmsImmuneTicksRemaining() * 0.6);
				String lbl = "IMM " + s;
				g.setColor(PRAYER_FG);
				g.drawString(lbl, textX, baseline);
				textX += fm.stringWidth(lbl) + 6;
			}
			if (showLcks && prot.isInCombatLogoutLock() && textX < textRx)
			{
				g.setColor(ORANGE);
				g.drawString("LOG", textX, baseline);
				textX += fm.stringWidth("LOG") + 6;
			}
			if (showLcks && prot.isUnderAttackLocked() && textX < textRx)
			{
				g.setColor(LIGHT_BLUE);
				g.drawString("LCK", textX, baseline);
			}
			return;
		}

		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();
		int baseline = p.y + (p.height + fm.getAscent() - fm.getDescent()) / 2;

		int eat = state.getActionClock().getEatCooldownTicks(ctick);
		int pot = state.getActionClock().getPotCooldownTicks(ctick);

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
		List<String> labelList = new ArrayList<>(10);
		List<Color>  colorList  = new ArrayList<>(10);

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

		ProtectionState prot     = state.getProtection();
		boolean         showLcks = debugRendering || config.showCombatLocks();
		if (showLcks && prot.isPjSafe())
		{
			int s = (int) Math.round(prot.getPjSafeTicksRemaining() * 0.6);
			labelList.add("PJ " + s + "s");
			colorList.add(s > 20 ? GREEN : s > 10 ? YELLOW : RED);
		}
		if (prot.isLmsImmune())
		{
			int s = (int) Math.round(prot.getLmsImmuneTicksRemaining() * 0.6);
			labelList.add("IMM " + s + "s");
			colorList.add(PRAYER_FG);
		}
		if (showLcks && prot.isInCombatLogoutLock())
		{
			labelList.add("LOG");
			colorList.add(ORANGE);
		}
		if (showLcks && prot.isUnderAttackLocked())
		{
			labelList.add("LCK");
			colorList.add(LIGHT_BLUE);
		}

		if (debugRendering || config.showZoneInfo())
		{
			int wildLevel = state.getContext().getWildernessLevel();
			if (wildLevel > 0)
			{
				labelList.add("W" + wildLevel);
				colorList.add(YELLOW);
			}
			if (state.getContext().isMultiCombat())
			{
				labelList.add("MLT");
				colorList.add(ORANGE);
			}
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

	static void drawCentered(Graphics2D g, FontMetrics fm, String text, int cx, int baseline)
	{
		g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
	}

	private static void drawRightAligned(Graphics2D g, FontMetrics fm, String text, int rx, int baseline)
	{
		g.drawString(text, rx - fm.stringWidth(text), baseline);
	}

	/**
	 * Draws a number over a bar using a compact dark backing plate for contrast.
	 * The backing rectangle tightly wraps the text with 1px horizontal padding.
	 * No fuzzy outline — clean white glyph edges.
	 */
	private static void drawBarValue(Graphics2D g, FontMetrics fm, String text, int x, int baseline, Color fg)
	{
		int textW = fm.stringWidth(text);
		int padX  = 1;
		int padY  = 1;
		int boxX  = x - padX;
		int boxY  = baseline - fm.getAscent() - padY;
		int boxW  = textW + padX * 2;
		int boxH  = fm.getHeight() + padY * 2;
		Composite prev = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
		g.setColor(Color.BLACK);
		g.fillRect(boxX, boxY, boxW, boxH);
		g.setComposite(prev);
		g.setColor(fg);
		g.drawString(text, x, baseline);
	}

	private static void drawBarValueRight(Graphics2D g, FontMetrics fm, String text, int rx, int baseline, Color fg)
	{
		drawBarValue(g, fm, text, rx - fm.stringWidth(text), baseline, fg);
	}

	/**
	 * Draws text with a 1-pixel black outline on all 4 sides, then the foreground colour.
	 * Makes numbers legible when rendered directly over coloured bars.
	 */
	static void drawOutlinedString(Graphics2D g, String text, int x, int baseline, Color fg)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x - 1, baseline);
		g.drawString(text, x + 1, baseline);
		g.drawString(text, x, baseline - 1);
		g.drawString(text, x, baseline + 1);
		g.setColor(fg);
		g.drawString(text, x, baseline);
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

	// ── Debug rendering entry points ──────────────────────────────────────────
	// Package-private so PvpHudDebugOverlay (same package) can call in without
	// duplicating the full rendering pipeline.

	/**
	 * Renders one layout at (0, 0) in g's coordinate space using synthetic state.
	 * Caller must translate g to the desired screen position before calling and
	 * restore the transform afterwards.
	 */
	void renderForDebug(Graphics2D g, PvpHudState state, HudLayout layout,
		LayoutProfile profile, int panelW, int panelH)
	{
		LayoutProfile savedProfile = activeProfile;
		activeProfile = profile;
		debugRendering = true;
		try
		{
			Font normal = FontManager.getRunescapeFont();
			Font small  = FontManager.getRunescapeSmallFont();
			HudLayoutState tempLayout = new HudLayoutState();
			Rectangle bounds = new Rectangle(0, 0, panelW, panelH);

			switch (layout)
			{
				case HORIZONTAL_FLOAT:
					computeHorizLayout(tempLayout, bounds, profile.showBoostRow, false);
					drawAll(g, state, tempLayout, bounds, normal, small, false);
					break;
				case VERTICAL_FLOAT:
					computeVertLayout(tempLayout, bounds, profile.showBoostRow, false);
					drawAll(g, state, tempLayout, bounds, normal, small, true);
					break;
				case INVENTORY_HUG:
					renderInventoryHugForDebug(g, state, panelW, panelH);
					break;
			}
		}
		finally
		{
			activeProfile  = savedProfile;
			debugRendering = false;
		}
	}

	/**
	 * Renders the inventory-hug L-shape with synthetic geometry (no widget anchor).
	 * activeProfile must be set before calling; renderForDebug always does this.
	 */
	private void renderInventoryHugForDebug(Graphics2D g, PvpHudState state,
		int totalW, int totalH)
	{
		int paneH = totalH - INVY_TOP_H;
		if (paneH < 0) paneH = 0;

		int opacity = activeProfile != null ? activeProfile.backgroundOpacity : 220;
		g.setColor(bgColor(opacity));
		g.fillRect(0, 0, totalW, INVY_TOP_H);
		g.fillRect(0, INVY_TOP_H, INVY_LEFT_W, paneH);

		g.setColor(DIVIDER);
		g.drawLine(0, INVY_TOP_H - 1, totalW - 1, INVY_TOP_H - 1);
		g.drawLine(INVY_LEFT_W - 1, INVY_TOP_H, INVY_LEFT_W - 1, totalH - 1);

		Font small = FontManager.getRunescapeSmallFont();
		drawInventoryTopRail(g, state, small, totalW);
		drawInventoryLeftRail(g, state, small, STONE_TABLET_TAB_H, Math.max(0, paneH - STONE_TABLET_TAB_H));
	}
}
