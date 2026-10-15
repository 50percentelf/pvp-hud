package com.pvphud;

import com.pvphud.debug.DebugAntifireType;
import com.pvphud.debug.DebugFreezeType;
import com.pvphud.debug.DebugOpponentPrayer;
import com.pvphud.debug.DebugPoisonType;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("pvp-hud")
public interface PvpHudConfig extends Config
{
	// ── General section ──────────────────────────────────────────────────────
	// Plugin-wide behaviour: shared across all layouts. Key names are unchanged
	// from the previous release so existing user settings are preserved.

	@ConfigSection(
		name = "General",
		description = "Plugin-wide behaviour settings shared across all layouts",
		position = 0,
		closedByDefault = true
	)
	String generalSection = "general";

	@ConfigItem(
		keyName = "hudMode",
		name = "HUD Mode",
		description = "When the PvP HUD is displayed: in PvP areas, automatically during combat, or manually via hotkey",
		section = generalSection,
		position = 0
	)
	default HudMode hudMode()
	{
		return HudMode.MANUAL;
	}

	@ConfigItem(
		keyName = "hudVisible",
		name = "HUD Enabled",
		description = "Master enable/disable for PvP HUD. HUD Mode determines when an enabled HUD is displayed. The Toggle HUD Hotkey changes this setting.",
		section = generalSection,
		position = 1
	)
	default boolean hudVisible()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hudLayout",
		name = "HUD Layout",
		description = "Chat Locked: fills the chatbox area. Horizontal Float: draggable same size. Vertical Float: narrow side panel. Inventory Hug: anchors to inventory.",
		section = generalSection,
		position = 2
	)
	default HudLayout hudLayout()
	{
		return HudLayout.CHAT_LOCKED;
	}

	@ConfigItem(
		keyName = "showHpShake",
		name = "HP Bar Shake on Hit",
		description = "Shake the HP bar and text briefly when taking damage.",
		section = generalSection,
		position = 3
	)
	default boolean showHpShake()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFreezeTimer",
		name = "Show Freeze Timer",
		description = "Show the self-freeze (ICE) countdown timer in the buff strip.",
		section = generalSection,
		position = 4
	)
	default boolean showFreezeTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineTimers",
		name = "Show Divine Potion Timers",
		description = "Show divine potion countdown timers (DSC, DRG, DMG, BAS, BTM, MEN) in the buff strip.",
		section = generalSection,
		position = 5
	)
	default boolean showDivineTimers()
	{
		return true;
	}

	@Range(min = 16, max = 40)
	@ConfigItem(
		keyName = "opponentPrayerIconSize",
		name = "Opponent Prayer Icon Size",
		description = "Size of the opponent overhead prayer icon in the opponent panel (pixels).",
		section = generalSection,
		position = 6
	)
	default int opponentPrayerIconSize()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "hudToggleKey",
		name = "Toggle HUD Hotkey",
		description = "Hotkey to toggle the PvP HUD on and off.",
		section = generalSection,
		position = 7
	)
	default Keybind hudToggleKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer1Key",
		name = "Timer 1 Hotkey",
		description = "Press to start/stop manual countdown timer 1. Shown as T1 in the action strip.",
		section = generalSection,
		position = 8
	)
	default Keybind timer1Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer1Duration",
		name = "Timer 1 Duration (s)",
		description = "How long timer 1 counts down in seconds.",
		section = generalSection,
		position = 9
	)
	default int timer1Duration()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "timer2Key",
		name = "Timer 2 Hotkey",
		description = "Press to start/stop manual countdown timer 2. Shown as T2 in the action strip.",
		section = generalSection,
		position = 10
	)
	default Keybind timer2Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer2Duration",
		name = "Timer 2 Duration (s)",
		description = "How long timer 2 counts down in seconds.",
		section = generalSection,
		position = 11
	)
	default int timer2Duration()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "barrageHighlightColor",
		name = "Barrage Highlight Colour",
		description = "Draws a coloured border around the freeze icon when you are frozen. None = disabled.",
		section = generalSection,
		position = 12
	)
	default Color barrageHighlightColor()
	{
		return null;
	}

	@ConfigItem(
		keyName = "tbHighlightColor",
		name = "Teleblock Highlight Colour",
		description = "Draws a coloured border around the Tele Block icon when you are teleblocked. None = disabled.",
		section = generalSection,
		position = 13
	)
	default Color tbHighlightColor()
	{
		return null;
	}

	@ConfigItem(
		keyName = "hudBackgroundColor",
		name = "HUD Background Colour",
		description = "Base fill colour for all HUD panel backgrounds. Opacity is controlled per-layout.",
		section = generalSection,
		position = 14
	)
	default Color hudBackgroundColor()
	{
		return new Color(20, 20, 20);
	}

	// ── Chat Locked section ──────────────────────────────────────────────────
	// Key names intentionally match the previous (unsectioned) release so that
	// existing user settings survive the refactor without a migration step.

	@ConfigSection(
		name = "Chat Locked",
		description = "Presentation settings for the Chat Locked layout",
		position = 10,
		closedByDefault = true
	)
	String chatLockedSection = "chatLocked";

	@ConfigItem(
		keyName = "buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		section = chatLockedSection,
		position = 0
	)
	default BuffStyle buffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "backgroundOpacity",
		name = "Background Opacity",
		description = "HUD background opacity: 0 = fully transparent, 255 = fully opaque.",
		section = chatLockedSection,
		position = 1
	)
	default int backgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "showBoostRow",
		name = "Show Boost Row",
		description = "Show the ATK/STR/DEF/RNG/MAG boost row at the bottom of the HUD.",
		section = chatLockedSection,
		position = 2
	)
	default boolean showBoostRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16).",
		section = chatLockedSection,
		position = 3
	)
	default boolean boostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = chatLockedSection,
		position = 4
	)
	default BarDisplayStyle hpBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "prayerBarStyle",
		name = "Prayer Bar",
		description = "How Prayer is displayed in the YOU panel.",
		section = chatLockedSection,
		position = 5
	)
	default BarDisplayStyle prayerBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "runBarStyle",
		name = "Run Energy Bar",
		description = "How run energy is displayed in the YOU panel.",
		section = chatLockedSection,
		position = 6
	)
	default BarDisplayStyle runBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "specBarStyle",
		name = "Spec Bar",
		description = "How special attack energy is displayed in the YOU panel.",
		section = chatLockedSection,
		position = 7
	)
	default BarDisplayStyle specBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	// ── Horizontal Float section ─────────────────────────────────────────────

	@ConfigSection(
		name = "Horizontal Float",
		description = "Presentation settings for the Horizontal Float layout",
		position = 20,
		closedByDefault = true
	)
	String horizFloatSection = "horizFloat";

	@ConfigItem(
		keyName = "hf_buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		section = horizFloatSection,
		position = 0
	)
	default BuffStyle hfBuffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "hf_backgroundOpacity",
		name = "Background Opacity",
		description = "HUD background opacity: 0 = fully transparent, 255 = fully opaque.",
		section = horizFloatSection,
		position = 1
	)
	default int hfBackgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "hf_showBoostRow",
		name = "Show Boost Row",
		description = "Show the ATK/STR/DEF/RNG/MAG boost row at the bottom of the HUD.",
		section = horizFloatSection,
		position = 2
	)
	default boolean hfShowBoostRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hf_boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16).",
		section = horizFloatSection,
		position = 3
	)
	default boolean hfBoostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hf_reserveBoostDockWhenHidden",
		name = "Reserve Boost Row Space When Hidden",
		description = "Keep the boost row band visible as an empty docking pocket for other overlays when Show Boost Row is off.",
		section = horizFloatSection,
		position = 4
	)
	default boolean hfReserveBoostDockWhenHidden()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hf_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = horizFloatSection,
		position = 5
	)
	default BarDisplayStyle hfHpBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "hf_prayerBarStyle",
		name = "Prayer Bar",
		description = "How Prayer is displayed in the YOU panel.",
		section = horizFloatSection,
		position = 6
	)
	default BarDisplayStyle hfPrayerBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "hf_runBarStyle",
		name = "Run Energy Bar",
		description = "How run energy is displayed in the YOU panel.",
		section = horizFloatSection,
		position = 7
	)
	default BarDisplayStyle hfRunBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "hf_specBarStyle",
		name = "Spec Bar",
		description = "How special attack energy is displayed in the YOU panel.",
		section = horizFloatSection,
		position = 8
	)
	default BarDisplayStyle hfSpecBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	// ── Vertical Float section ───────────────────────────────────────────────

	@ConfigSection(
		name = "Vertical Float",
		description = "Presentation settings for the Vertical Float layout",
		position = 30,
		closedByDefault = true
	)
	String vertFloatSection = "vertFloat";

	@ConfigItem(
		keyName = "vf_buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		section = vertFloatSection,
		position = 0
	)
	default BuffStyle vfBuffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "vf_backgroundOpacity",
		name = "Background Opacity",
		description = "HUD background opacity: 0 = fully transparent, 255 = fully opaque.",
		section = vertFloatSection,
		position = 1
	)
	default int vfBackgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "vf_showBoostRow",
		name = "Show Boost Row",
		description = "Show the ATK/STR/DEF/RNG/MAG boost row at the bottom of the HUD.",
		section = vertFloatSection,
		position = 2
	)
	default boolean vfShowBoostRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "vf_boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16).",
		section = vertFloatSection,
		position = 3
	)
	default boolean vfBoostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "vf_reserveBoostDockWhenHidden",
		name = "Reserve Boost Row Space When Hidden",
		description = "Keep the boost row band visible as an empty docking pocket for other overlays when Show Boost Row is off.",
		section = vertFloatSection,
		position = 4
	)
	default boolean vfReserveBoostDockWhenHidden()
	{
		return false;
	}

	@ConfigItem(
		keyName = "vf_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = vertFloatSection,
		position = 5
	)
	default BarDisplayStyle vfHpBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "vf_prayerBarStyle",
		name = "Prayer Bar",
		description = "How Prayer is displayed in the YOU panel.",
		section = vertFloatSection,
		position = 6
	)
	default BarDisplayStyle vfPrayerBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "vf_runBarStyle",
		name = "Run Energy Bar",
		description = "How run energy is displayed in the YOU panel.",
		section = vertFloatSection,
		position = 7
	)
	default BarDisplayStyle vfRunBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "vf_specBarStyle",
		name = "Spec Bar",
		description = "How special attack energy is displayed in the YOU panel.",
		section = vertFloatSection,
		position = 8
	)
	default BarDisplayStyle vfSpecBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	// ── Inventory Hug section ────────────────────────────────────────────────

	@ConfigSection(
		name = "Inventory Hug",
		description = "Presentation settings for the Inventory Hug layout",
		position = 40,
		closedByDefault = true
	)
	String invyHugSection = "invyHug";

	@ConfigItem(
		keyName = "ih_buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		section = invyHugSection,
		position = 0
	)
	default BuffStyle ihBuffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "ih_backgroundOpacity",
		name = "Background Opacity",
		description = "HUD background opacity: 0 = fully transparent, 255 = fully opaque.",
		section = invyHugSection,
		position = 1
	)
	default int ihBackgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "ih_showBoostRow",
		name = "Show Boost Row",
		description = "Show the ATK/STR/DEF/RNG/MAG boost row at the bottom of the HUD.",
		section = invyHugSection,
		position = 2
	)
	default boolean ihShowBoostRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ih_boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16).",
		section = invyHugSection,
		position = 3
	)
	default boolean ihBoostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ih_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = invyHugSection,
		position = 4
	)
	default BarDisplayStyle ihHpBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "ih_prayerBarStyle",
		name = "Prayer Bar",
		description = "How Prayer is displayed in the YOU panel.",
		section = invyHugSection,
		position = 5
	)
	default BarDisplayStyle ihPrayerBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "ih_runBarStyle",
		name = "Run Energy Bar",
		description = "How run energy is displayed in the YOU panel.",
		section = invyHugSection,
		position = 6
	)
	default BarDisplayStyle ihRunBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "ih_specBarStyle",
		name = "Spec Bar",
		description = "How special attack energy is displayed in the YOU panel.",
		section = invyHugSection,
		position = 7
	)
	default BarDisplayStyle ihSpecBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	// ── Experimental / Pending RuneLite Ruling ───────────────────────────────
	// Features in this section are incomplete or awaiting RuneLite team guidance.
	//
	// Zone Info (W# / MLT): reads Varbits.WILDERNESS_LEVEL and the multi-combat
	//   region flag. Disabled by default — still evaluating layout fit.
	//
	// Combat Locks (PJ / LOG / LCK): the underlying ProtectionState is tracked
	//   correctly, but the timer accuracy has not been fully verified in-game
	//   across all edge cases (splash hits, multi-combat, same-tick exchanges).
	//   Disabled by default pending confirmation testing.
	//
	// Streamer Output: renders the HUD into a separate OBS/window-capture
	//   window sharing the same PvpHudState; no duplicate combat logic.
	//   Awaiting RuneLite ruling on standalone Swing window output.
	//
	// Opponent Attack Cycle: live countdown to the opponent's next possible
	//   attack, derived from an observed animation with no prediction or
	//   prayer recommendation. Awaiting RuneLite ruling on combat timing aids.

	@ConfigSection(
		name = "Experimental — Pending RuneLite Ruling",
		description = "Features here are incomplete or awaiting RuneLite review. "
			+ "Streamer Output and Opponent Attack Cycle are NOT implemented and cannot be enabled. "
			+ "Combat Locks can be toggled but accuracy has not been fully verified in-game.",
		position = 50,
		closedByDefault = true
	)
	String pendingRulingSection = "pendingRuling";

	@ConfigItem(
		keyName = "showZoneInfo",
		name = "Show Zone Info (W# / MLT)  [EXPERIMENTAL]",
		description = "Show wilderness level (W##) and multi-combat zone (MLT) indicators in the action strip.",
		section = pendingRulingSection,
		position = 0
	)
	default boolean showZoneInfo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCombatLocks",
		name = "Show Combat Locks (PJ / LOG / LCK)  [EXPERIMENTAL]",
		description = "<html>Show PJ-safe timer, combat logout lock (LOG), and under-attack lock (LCK) "
			+ "in the action strip.<br>"
			+ "Incomplete: timer accuracy under all edge cases has not been verified in-game.</html>",
		section = pendingRulingSection,
		position = 1
	)
	default boolean showCombatLocks()
	{
		return false;
	}

	// ── Debug section ─────────────────────────────────────────────────────────
	// Enables a separate overlay showing all 4 layouts simultaneously with
	// injected state so rendering bugs can be identified without going in-game.
	// All keys are prefixed "dbg_" to avoid clashing with real config keys.

	@ConfigSection(
		name = "Debug — Layout Preview",
		description = "Renders all 4 HUD layouts simultaneously with injected state. "
			+ "Use this to identify rendering bugs without being in combat.",
		position = 100,
		closedByDefault = true
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "dbg_enabled",
		name = "Enable Debug Preview",
		description = "Show the debug overlay with all 4 layouts rendered at once.",
		section = debugSection,
		position = 0
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_buffStyle",
		name = "Buff Display Style (all panels)",
		description = "Which buff style to use across all debug panels.",
		section = debugSection,
		position = 1
	)
	default BuffStyle debugBuffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@ConfigItem(
		keyName = "dbg_presetAll",
		name = "Preset: All Effects",
		description = "When checked, forces all effects on at representative values. Overrides all other debug sliders.",
		section = debugSection,
		position = 2
	)
	default boolean debugPresetAllEffects()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_injectHits",
		name = "Inject Fake Combat Events",
		description = "Populate the fight event log with sample hits (outgoing, incoming, smite drain, CHANCE!).",
		section = debugSection,
		position = 3
	)
	default boolean debugInjectHits()
	{
		return false;
	}

	// ── Self: HP / Prayer / Run ───────────────────────────────────────────────

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "dbg_hp",
		name = "Current HP",
		description = "Injected current HP for the YOU panel.",
		section = debugSection,
		position = 10
	)
	default int debugHp()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "dbg_maxHp",
		name = "Max HP",
		description = "Injected max HP for the YOU panel.",
		section = debugSection,
		position = 11
	)
	default int debugMaxHp()
	{
		return 99;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "dbg_prayer",
		name = "Current Prayer",
		description = "Injected current prayer points for the YOU panel.",
		section = debugSection,
		position = 12
	)
	default int debugPrayer()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "dbg_maxPrayer",
		name = "Max Prayer",
		description = "Injected max prayer points for the YOU panel.",
		section = debugSection,
		position = 13
	)
	default int debugMaxPrayer()
	{
		return 99;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "dbg_run",
		name = "Run Energy",
		description = "Injected run energy 0–100 for the YOU panel.",
		section = debugSection,
		position = 14
	)
	default int debugRunEnergy()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "dbg_spec",
		name = "Spec Energy",
		description = "Injected special attack energy 0–100.",
		section = debugSection,
		position = 15
	)
	default int debugSpecEnergy()
	{
		return 100;
	}

	// ── Self: Status effects ──────────────────────────────────────────────────

	@ConfigItem(
		keyName = "dbg_freezeType",
		name = "Freeze Type",
		description = "Inject a freeze of the selected spell type.",
		section = debugSection,
		position = 20
	)
	default DebugFreezeType debugFreezeType()
	{
		return DebugFreezeType.NONE;
	}

	@Range(min = 0, max = 32)
	@ConfigItem(
		keyName = "dbg_freezeTicks",
		name = "Freeze Ticks Remaining",
		description = "How many ticks remain on the injected freeze.",
		section = debugSection,
		position = 21
	)
	default int debugFreezeTicks()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "dbg_teleblock",
		name = "Inject Teleblock",
		description = "Show a 3-minute teleblock timer in the buff strip.",
		section = debugSection,
		position = 22
	)
	default boolean debugTeleblock()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_vengeance",
		name = "Vengeance Ready",
		description = "Show the vengeance-ready indicator.",
		section = debugSection,
		position = 23
	)
	default boolean debugVengeance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_poison",
		name = "Poison / Immunity State",
		description = "Inject the selected poison or immunity state.",
		section = debugSection,
		position = 24
	)
	default DebugPoisonType debugPoisonState()
	{
		return DebugPoisonType.NONE;
	}

	// ── Potions ───────────────────────────────────────────────────────────────

	@ConfigItem(
		keyName = "dbg_dsc",
		name = "Divine Super Combat",
		description = "Show a Divine Super Combat timer.",
		section = debugSection,
		position = 30
	)
	default boolean debugDivineSupercombat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_drg",
		name = "Divine Ranging",
		description = "Show a Divine Ranging timer.",
		section = debugSection,
		position = 31
	)
	default boolean debugDivineRanging()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_dmg",
		name = "Divine Magic",
		description = "Show a Divine Magic timer.",
		section = debugSection,
		position = 32
	)
	default boolean debugDivineMagic()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_bas",
		name = "Divine Bastion",
		description = "Show a Divine Bastion timer.",
		section = debugSection,
		position = 33
	)
	default boolean debugDivineBastion()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_btm",
		name = "Divine Battlemage",
		description = "Show a Divine Battlemage timer.",
		section = debugSection,
		position = 34
	)
	default boolean debugDivineBattlemage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_men",
		name = "Menaphite Remedy",
		description = "Show a Menaphite Remedy timer.",
		section = debugSection,
		position = 35
	)
	default boolean debugMenaphiteRemedy()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_stamina",
		name = "Stamina Potion",
		description = "Show a Stamina Potion timer.",
		section = debugSection,
		position = 36
	)
	default boolean debugStamina()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dbg_antifire",
		name = "Antifire",
		description = "Inject the selected antifire protection type.",
		section = debugSection,
		position = 37
	)
	default DebugAntifireType debugAntifire()
	{
		return DebugAntifireType.NONE;
	}

	// ── Stat boosts ───────────────────────────────────────────────────────────

	@Range(min = -15, max = 20)
	@ConfigItem(
		keyName = "dbg_atkBoost",
		name = "Attack Boost Delta",
		description = "ATK boost relative to base 99. Negative = debuff.",
		section = debugSection,
		position = 40
	)
	default int debugBoostAtk()
	{
		return 0;
	}

	@Range(min = -15, max = 20)
	@ConfigItem(
		keyName = "dbg_strBoost",
		name = "Strength Boost Delta",
		description = "STR boost relative to base 99. Negative = debuff.",
		section = debugSection,
		position = 41
	)
	default int debugBoostStr()
	{
		return 0;
	}

	@Range(min = -15, max = 20)
	@ConfigItem(
		keyName = "dbg_defBoost",
		name = "Defence Boost Delta",
		description = "DEF boost relative to base 99. Negative = debuff.",
		section = debugSection,
		position = 42
	)
	default int debugBoostDef()
	{
		return 0;
	}

	@Range(min = -15, max = 20)
	@ConfigItem(
		keyName = "dbg_rngBoost",
		name = "Ranged Boost Delta",
		description = "RNG boost relative to base 99. Negative = debuff.",
		section = debugSection,
		position = 43
	)
	default int debugBoostRng()
	{
		return 0;
	}

	@Range(min = -15, max = 20)
	@ConfigItem(
		keyName = "dbg_magBoost",
		name = "Magic Boost Delta",
		description = "MAG boost relative to base 99. Negative = debuff.",
		section = debugSection,
		position = 44
	)
	default int debugBoostMag()
	{
		return 0;
	}

	// ── Action clocks ─────────────────────────────────────────────────────────

	@Range(min = 0, max = 10)
	@ConfigItem(
		keyName = "dbg_atkDelay",
		name = "Attack Delay (ticks)",
		description = "Injected attack delay ticks for the action strip dot counter.",
		section = debugSection,
		position = 50
	)
	default int debugAttackDelay()
	{
		return 0;
	}

	@Range(min = 0, max = 3)
	@ConfigItem(
		keyName = "dbg_eatCooldown",
		name = "Eat Cooldown (ticks)",
		description = "Injected eat cooldown ticks.",
		section = debugSection,
		position = 51
	)
	default int debugEatCooldown()
	{
		return 0;
	}

	@Range(min = 0, max = 3)
	@ConfigItem(
		keyName = "dbg_potCooldown",
		name = "Pot Cooldown (ticks)",
		description = "Injected potion cooldown ticks.",
		section = debugSection,
		position = 52
	)
	default int debugPotCooldown()
	{
		return 0;
	}

	// ── Opponent ──────────────────────────────────────────────────────────────

	@ConfigItem(
		keyName = "dbg_oppName",
		name = "Opponent Name",
		description = "Name shown in the opponent panel.",
		section = debugSection,
		position = 60
	)
	default String debugOpponentName()
	{
		return "Opponent";
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "dbg_oppHp",
		name = "Opponent Current HP",
		description = "Injected current HP for the opponent panel.",
		section = debugSection,
		position = 61
	)
	default int debugOpponentHp()
	{
		return 75;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "dbg_oppMaxHp",
		name = "Opponent Max HP",
		description = "Injected max HP for the opponent panel.",
		section = debugSection,
		position = 62
	)
	default int debugOpponentMaxHp()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "dbg_oppPrayer",
		name = "Opponent Overhead Prayer",
		description = "Overhead prayer shown for the opponent.",
		section = debugSection,
		position = 63
	)
	default DebugOpponentPrayer debugOpponentPrayer()
	{
		return DebugOpponentPrayer.NONE;
	}

	@ConfigItem(
		keyName = "dbg_oppVeng",
		name = "Opponent Vengeance Ready",
		description = "Show the opponent vengeance-ready indicator.",
		section = debugSection,
		position = 64
	)
	default boolean debugOpponentVengeance()
	{
		return false;
	}

	// ── Protection timers ─────────────────────────────────────────────────────

	@Range(min = 0, max = 20)
	@ConfigItem(
		keyName = "dbg_pjTimer",
		name = "PJ Safe Ticks",
		description = "Injected PJ-safe ticks remaining (requires Show Combat Locks to be enabled).",
		section = debugSection,
		position = 70
	)
	default int debugPjTimer()
	{
		return 0;
	}

	@Range(min = 0, max = 16)
	@ConfigItem(
		keyName = "dbg_combatLock",
		name = "Combat Logout Lock Ticks",
		description = "Injected combat logout lock ticks (requires Show Combat Locks).",
		section = debugSection,
		position = 71
	)
	default int debugCombatLogoutLock()
	{
		return 0;
	}

	@Range(min = 0, max = 20)
	@ConfigItem(
		keyName = "dbg_atkLock",
		name = "Under-Attack Lock Ticks",
		description = "Injected under-attack lock ticks (requires Show Combat Locks).",
		section = debugSection,
		position = 72
	)
	default int debugUnderAttackLock()
	{
		return 0;
	}

	// ── Zone info ─────────────────────────────────────────────────────────────

	@Range(min = 0, max = 56)
	@ConfigItem(
		keyName = "dbg_wildLevel",
		name = "Wilderness Level",
		description = "Injected wilderness level (0 = not in wilderness). Requires Show Zone Info.",
		section = debugSection,
		position = 80
	)
	default int debugWildernessLevel()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "dbg_multiCombat",
		name = "Multi-Combat Zone",
		description = "Show the MLT multi-combat indicator. Requires Show Zone Info.",
		section = debugSection,
		position = 81
	)
	default boolean debugMultiCombat()
	{
		return false;
	}
}
