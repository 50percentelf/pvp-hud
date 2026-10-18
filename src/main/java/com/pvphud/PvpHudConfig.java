package com.pvphud;

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
		position = 0
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
		return true;
	}

	@ConfigItem(
		keyName = "hudLayout",
		name = "HUD Layout",
		description = "Horizontal Float: draggable chatbox-sized panel (enable Pin to Chatbox to lock it). Vertical Float: narrow side panel. Inventory Hug: anchors to inventory.",
		section = generalSection,
		position = 2
	)
	default HudLayout hudLayout()
	{
		return HudLayout.HORIZONTAL_FLOAT;
	}

	@ConfigItem(
		keyName = "showFreezeTimer",
		name = "Show Freeze Timer",
		description = "Show the self-freeze (ICE) countdown timer in the buff strip.",
		section = generalSection,
		position = 3
	)
	default boolean showFreezeTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineTimers",
		name = "Potion Effect Timers",
		description = "Show divine potion countdown timers (DSC, DRG, DMG, BAS, BTM, MEN) in the buff strip.",
		section = generalSection,
		position = 4
	)
	default boolean showDivineTimers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showZoneInfo",
		name = "Show Zone Info (W# / MLT)",
		description = "Show wilderness level (W##) and multi-combat zone (MLT) indicators in the action strip.",
		section = generalSection,
		position = 5
	)
	default boolean showZoneInfo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hudToggleKey",
		name = "Toggle HUD Hotkey",
		description = "Hotkey to toggle the PvP HUD on and off.",
		section = generalSection,
		position = 6
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
		position = 7
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
		position = 8
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
		position = 9
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
		position = 10
	)
	default int timer2Duration()
	{
		return 300;
	}

	// ── Appearance section ───────────────────────────────────────────────────
	// Visual tweaks: shake, icon size, highlight colours, background.

	@ConfigSection(
		name = "Appearance",
		description = "Visual tweaks: shake effect, icon size, highlight colours, background.",
		position = 10,
		closedByDefault = true
	)
	String appearanceSection = "appearance";

	@ConfigItem(
		keyName = "showHpShake",
		name = "HP Bar Shake on Hit",
		description = "Shake the HP bar and text briefly when taking damage.",
		section = appearanceSection,
		position = 0
	)
	default boolean showHpShake()
	{
		return true;
	}

	@Range(min = 16, max = 40)
	@ConfigItem(
		keyName = "opponentPrayerIconSize",
		name = "Opponent Prayer Icon Size",
		description = "Size of the opponent overhead prayer icon in the opponent panel (pixels).",
		section = appearanceSection,
		position = 1
	)
	default int opponentPrayerIconSize()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "barrageHighlightColor",
		name = "Freeze Highlight Colour",
		description = "Draws a coloured border around the freeze icon when you are frozen. None = disabled.",
		section = appearanceSection,
		position = 2
	)
	default Color barrageHighlightColor()
	{
		return null;
	}

	@ConfigItem(
		keyName = "tbHighlightColor",
		name = "Teleblock Highlight Colour",
		description = "Draws a coloured border around the Tele Block icon when you are teleblocked. None = disabled.",
		section = appearanceSection,
		position = 3
	)
	default Color tbHighlightColor()
	{
		return null;
	}

	@ConfigItem(
		keyName = "hudBackgroundColor",
		name = "HUD Background Colour",
		description = "Base fill colour for all HUD panel backgrounds. Opacity is controlled per-layout.",
		section = appearanceSection,
		position = 4
	)
	default Color hudBackgroundColor()
	{
		return new Color(20, 20, 20);
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
		keyName = "hf_chatLocked",
		name = "Pin to Chatbox",
		description = "Pin the HUD to the chatbox — it cannot be dragged and always matches the chatbox size and position. When off, the HUD floats freely.",
		section = horizFloatSection,
		position = 0
	)
	default boolean hfChatLocked()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hf_buffStyle",
		name = "Status Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		section = horizFloatSection,
		position = 1
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
		position = 2
	)
	default int hfBackgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "hf_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = horizFloatSection,
		position = 3
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
		position = 4
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
		position = 5
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
		position = 6
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
		name = "Status Display Style",
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
		keyName = "vf_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = vertFloatSection,
		position = 2
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
		position = 3
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
		position = 4
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
		position = 5
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
		name = "Status Display Style",
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
		keyName = "ih_hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = invyHugSection,
		position = 2
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
		position = 3
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
		position = 4
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
		position = 5
	)
	default BarDisplayStyle ihSpecBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}
}
