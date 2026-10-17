package com.pvphud;

import com.pvphud.state.EffectState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Debug overlay that renders all 4 HUD layouts simultaneously with injected state.
 * Only active when config.debugMode() is true. Lets you inspect rendering across
 * every layout and buff style without entering combat.
 *
 * Layout (two-column grid):
 *   Left  — CHAT_LOCKED (top) / HORIZONTAL_FLOAT (middle) / INVENTORY_HUG (bottom)
 *   Right — VERTICAL_FLOAT
 *
 * State lifecycle: state is applied once when debug config changes; between changes
 * the overlay simulates game ticks so countdowns run at real speed.
 */
public class PvpHudDebugOverlay extends Overlay
{
	// ── Panel dimensions ──────────────────────────────────────────────────────
	private static final int HORIZ_W    = 519;   // matches chatbox width
	private static final int HORIZ_H    = 143;   // matches chatbox height
	private static final int VERT_W     = 220;   // matches PvpHudOverlay.VERT_W
	private static final int VERT_H     = 400;   // matches PvpHudOverlay.VERT_H
	private static final int INVY_TOP_H = 72;    // mirrored from PvpHudOverlay
	private static final int INVY_PANE_H = 320;  // synthetic pane height — large enough for all effects active
	private static final int INVY_H     = INVY_TOP_H + INVY_PANE_H;

	// ── Layout constants ──────────────────────────────────────────────────────
	private static final int HEADER_H = 18;
	private static final int LABEL_H  = 14;
	private static final int GAP      = 4;
	private static final int PAD      = 4;

	// Column X origins
	private static final int LEFT_X  = 0;
	private static final int RIGHT_X = HORIZ_W + GAP;

	// First panel Y (same for both columns)
	private static final int PANEL_ROW1_Y = HEADER_H + GAP + LABEL_H;

	// Total overlay width
	private static final int TOTAL_W = RIGHT_X + VERT_W;

	// ── Colours ───────────────────────────────────────────────────────────────
	private static final Color HEADER_BG   = new Color(15, 15, 15, 245);
	private static final Color LABEL_BG    = new Color(35, 35, 60, 220);
	private static final Color LABEL_TEXT  = new Color(255, 200, 50);
	private static final Color BORDER      = new Color(80, 80, 140, 180);

	// ── Injected services ─────────────────────────────────────────────────────
	private final Client         client;
	private final PvpHudConfig   config;
	private final PvpHudOverlay  overlay;
	private final PvpHudPlugin   plugin;

	/** False until state has been applied at least once. Guarantees first-render apply even if hash is 0. */
	private boolean debugStateApplied = false;

	/** Hash of state-relevant debug config values from last apply. */
	private int lastConfigHash = 0;

	/** The game tick at which we last simulated a tick on the debug state. */
	private int lastTick = -1;

	@Inject
	PvpHudDebugOverlay(Client client, PvpHudConfig config, PvpHudOverlay overlay, PvpHudPlugin plugin)
	{
		this.client  = client;
		this.config  = config;
		this.overlay = overlay;
		this.plugin  = plugin;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!config.debugMode()) return null;

		PvpHudState debugState = plugin.getDebugHudState();
		int currentTick = client.getTickCount();
		int configHash  = computeDebugConfigHash();

		if (!debugStateApplied || configHash != lastConfigHash)
		{
			// Config changed (or first render) — reinitialise state from scratch.
			DebugStateBuilder.apply(config, debugState, currentTick);
			debugStateApplied = true;
			lastConfigHash    = configHash;
			lastTick          = currentTick;
		}
		else if (currentTick > lastTick && lastTick >= 0)
		{
			// Advance the debug state forward by however many game ticks elapsed.
			// Cap at 200 to avoid runaway loops after long pauses.
			int elapsed = Math.min(currentTick - lastTick, 200);
			for (int i = 0; i < elapsed; i++)
				tickDebugState(debugState);
			lastTick = currentTick;
		}
		else if (lastTick < 0)
		{
			lastTick = currentTick;
		}

		BuffStyle buffStyle = config.debugBuffStyle();

		// Use BARS_AND_NUMBERS for all bar styles so everything is visible in debug.
		LayoutProfile profile = new LayoutProfile(
			buffStyle,
			220,        // backgroundOpacity
			true,       // showBoostRow
			false,      // boostXOverX
			false,      // reserveBoostDockWhenHidden
			BarDisplayStyle.BARS_AND_NUMBERS,
			BarDisplayStyle.BARS_AND_NUMBERS,
			BarDisplayStyle.BARS_AND_NUMBERS,
			BarDisplayStyle.BARS_AND_NUMBERS
		);

		Font small = FontManager.getRunescapeSmallFont();
		g.setFont(small);
		FontMetrics fm = g.getFontMetrics();

		// ── Pre-compute panel positions ───────────────────────────────────────
		// Left column (stacked): CHAT_LOCKED → H_FLOAT → INVENTORY_HUG
		int leftRow1Y = PANEL_ROW1_Y;
		int leftRow2Y = leftRow1Y + HORIZ_H + GAP + LABEL_H;
		int leftRow3Y = leftRow2Y + HORIZ_H + GAP + LABEL_H;
		int usedH     = leftRow3Y + INVY_H;

		// ── Header + outer border ─────────────────────────────────────────────
		g.setColor(HEADER_BG);
		g.fillRect(0, 0, TOTAL_W, HEADER_H);
		g.setColor(BORDER);
		g.drawRect(0, 0, TOTAL_W - 1, usedH - 1);

		g.setColor(LABEL_TEXT);
		String header = "PvP HUD DEBUG  [" + buffStyle + "]"
			+ (config.debugPresetAllEffects() ? "  ● ALL EFFECTS PRESET" : "");
		g.drawString(header, PAD, HEADER_H - (HEADER_H - fm.getAscent()) / 2 - 1);

		// ── Left column ───────────────────────────────────────────────────────
		drawPanelLabel(g, fm, small, "CHAT_LOCKED", LEFT_X, leftRow1Y - LABEL_H, HORIZ_W);
		renderPanel(g, debugState, HudLayout.CHAT_LOCKED, profile,
			LEFT_X, leftRow1Y, HORIZ_W, HORIZ_H);

		drawPanelLabel(g, fm, small, "HORIZONTAL_FLOAT", LEFT_X, leftRow2Y - LABEL_H, HORIZ_W);
		renderPanel(g, debugState, HudLayout.HORIZONTAL_FLOAT, profile,
			LEFT_X, leftRow2Y, HORIZ_W, HORIZ_H);

		drawPanelLabel(g, fm, small, "INVENTORY_HUG", LEFT_X, leftRow3Y - LABEL_H, HORIZ_W);
		renderPanel(g, debugState, HudLayout.INVENTORY_HUG, profile,
			LEFT_X, leftRow3Y, HORIZ_W, INVY_H);

		// ── Right column ──────────────────────────────────────────────────────
		drawPanelLabel(g, fm, small, "VERTICAL_FLOAT", RIGHT_X, PANEL_ROW1_Y - LABEL_H, VERT_W);
		renderPanel(g, debugState, HudLayout.VERTICAL_FLOAT, profile,
			RIGHT_X, PANEL_ROW1_Y, VERT_W, VERT_H);

		return new Dimension(TOTAL_W, usedH);
	}

	/**
	 * Mirrors the per-tick decrements from PvpHudPlugin.onGameTick() on the debug state.
	 * Freeze and antifire use absolute tick anchors so they count down automatically
	 * as client.getTickCount() advances — no explicit decrement needed here.
	 * Manual timers (T1/T2) are wall-clock based and also self-advancing.
	 */
	private static void tickDebugState(PvpHudState state)
	{
		// SelfState: teleblock raw counter
		var self = state.getSelf();
		if (self.getTeleBlockTicksRemaining() > 0)
			self.setTeleBlockTicksRemaining(self.getTeleBlockTicksRemaining() - 1);

		// EffectState: divine potion counters, stamina, menaphite periodic
		EffectState fx = state.getEffects();
		if (fx.getDivineSupercombatTicks()  > 0) fx.setDivineSupercombatTicks(fx.getDivineSupercombatTicks()   - 1);
		if (fx.getDivineRangingTicks()      > 0) fx.setDivineRangingTicks(fx.getDivineRangingTicks()           - 1);
		if (fx.getDivineMagicTicks()        > 0) fx.setDivineMagicTicks(fx.getDivineMagicTicks()               - 1);
		if (fx.getDivineBastionTicks()      > 0) fx.setDivineBastionTicks(fx.getDivineBastionTicks()           - 1);
		if (fx.getDivineBattlemageTicks()   > 0) fx.setDivineBattlemageTicks(fx.getDivineBattlemageTicks()     - 1);
		if (fx.getStaminaEffectTicks()      > 0) fx.setStaminaEffectTicks(fx.getStaminaEffectTicks()           - 1);
		fx.getMenaphite().tick();

		// Poison/anti-poison/anti-venom periodic logic
		state.getPoison().tick();

		// Protection timers (PJ, combat logout, under-attack lock)
		state.getProtection().tick();

		// Action clock deadlines are absolute tick values — they count down automatically
		// as client.getTickCount() advances. No explicit decrement needed here.
	}

	/**
	 * Hash of the config values that drive DebugStateBuilder.apply().
	 * A change here triggers state reinitialisation.
	 */
	private int computeDebugConfigHash()
	{
		return Objects.hash(
			config.debugPresetAllEffects(),
			config.debugHp(), config.debugMaxHp(),
			config.debugPrayer(), config.debugMaxPrayer(),
			config.debugRunEnergy(), config.debugSpecEnergy(),
			config.debugFreezeType(), config.debugFreezeTicks(),
			config.debugTeleblock(), config.debugVengeance(),
			config.debugPoisonState(),
			config.debugDivineSupercombat(), config.debugDivineRanging(),
			config.debugDivineMagic(), config.debugDivineBastion(), config.debugDivineBattlemage(),
			config.debugMenaphiteRemedy(), config.debugStamina(),
			config.debugAntifire(),
			config.debugBoostAtk(), config.debugBoostStr(), config.debugBoostDef(),
			config.debugBoostRng(), config.debugBoostMag(),
			config.debugAttackDelay(), config.debugEatCooldown(), config.debugPotCooldown(),
			config.debugOpponentName(), config.debugOpponentHp(), config.debugOpponentMaxHp(),
			config.debugOpponentPrayer(), config.debugOpponentVengeance(),
			config.debugPjTimer(), config.debugCombatLogoutLock(), config.debugUnderAttackLock(),
			config.debugWildernessLevel(), config.debugMultiCombat(),
			config.debugInjectHits()
		);
	}

	// ── Drawing helpers ───────────────────────────────────────────────────────

	private void drawPanelLabel(Graphics2D g, FontMetrics fm, Font small,
		String text, int x, int y, int w)
	{
		g.setColor(LABEL_BG);
		g.fillRect(x, y, w, LABEL_H);
		g.setColor(BORDER);
		g.drawRect(x, y, w - 1, LABEL_H - 1);
		g.setFont(small);
		g.setColor(LABEL_TEXT);
		g.drawString(text, x + PAD, y + fm.getAscent() + (LABEL_H - fm.getHeight()) / 2);
	}

	/**
	 * Translates g to (panelX, panelY), calls renderForDebug, then restores the transform.
	 * A thin border is drawn around each panel to delimit it visually.
	 */
	private void renderPanel(Graphics2D g, PvpHudState state, HudLayout layout,
		LayoutProfile profile, int panelX, int panelY, int panelW, int panelH)
	{
		AffineTransform saved = g.getTransform();
		g.translate(panelX, panelY);
		try
		{
			overlay.renderForDebug(g, state, layout, profile, panelW, panelH);
		}
		finally
		{
			g.setTransform(saved);
		}
		// Panel border
		g.setColor(BORDER);
		g.drawRect(panelX, panelY, panelW - 1, panelH - 1);
	}
}
