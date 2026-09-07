package com.pvphud;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("pvp-hud")
public interface PvpHudConfig extends Config
{
	@ConfigItem(
		keyName = "hudMode",
		name = "HUD Mode",
		description = "When the PvP HUD is displayed: in PvP areas, automatically during combat, or manually via hotkey",
		position = 0
	)
	default HudMode hudMode()
	{
		return HudMode.MANUAL;
	}

	@ConfigItem(
		keyName = "hudVisible",
		name = "Show HUD (testing)",
		description = "Temporarily force the HUD visible for visual testing.",
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
		position = 2
	)
	default HudLayout hudLayout()
	{
		return HudLayout.CHAT_LOCKED;
	}

	@ConfigItem(
		keyName = "buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: horizontal icon strip.",
		position = 3
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
		position = 4
	)
	default int backgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "showHpShake",
		name = "HP Bar Shake on Hit",
		description = "Shake the HP bar and text briefly when taking damage.",
		position = 5
	)
	default boolean showHpShake()
	{
		return true;
	}

	@ConfigItem(
		keyName = "boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16).",
		position = 6
	)
	default boolean boostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBoostRow",
		name = "Show Boost Row",
		description = "Show the ATK/STR/DEF/RNG/MAG boost row at the bottom of the HUD.",
		position = 7
	)
	default boolean showBoostRow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFreezeTimer",
		name = "Show Freeze Timer",
		description = "Show the self-freeze (ICE) countdown timer in the buff strip.",
		position = 8
	)
	default boolean showFreezeTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDivineTimers",
		name = "Show Divine Potion Timers",
		description = "Show divine potion countdown timers (DSC, DRG, DMG, BAS, BTM, MEN) in the buff strip.",
		position = 9
	)
	default boolean showDivineTimers()
	{
		return true;
	}

	// ── Bars section ─────────────────────────────────────────────────────────────

	@ConfigSection(
		name = "Bars",
		description = "Individual display options for each resource bar in the YOU panel",
		position = 10
	)
	String barsSection = "bars";

	@ConfigItem(
		keyName = "hpBarStyle",
		name = "HP Bar",
		description = "How HP is displayed in the YOU panel.",
		section = barsSection,
		position = 0
	)
	default BarDisplayStyle hpBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "prayerBarStyle",
		name = "Prayer Bar",
		description = "How Prayer is displayed in the YOU panel.",
		section = barsSection,
		position = 1
	)
	default BarDisplayStyle prayerBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "runBarStyle",
		name = "Run Energy Bar",
		description = "How run energy is displayed in the YOU panel.",
		section = barsSection,
		position = 2
	)
	default BarDisplayStyle runBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "specBarStyle",
		name = "Spec Bar",
		description = "How special attack energy is displayed in the YOU panel.",
		section = barsSection,
		position = 3
	)
	default BarDisplayStyle specBarStyle()
	{
		return BarDisplayStyle.BARS_AND_NUMBERS;
	}

	// ── Timers section ────────────────────────────────────────────────────────────

	@ConfigItem(
		keyName = "hudToggleKey",
		name = "Toggle HUD Hotkey",
		description = "Hotkey to toggle the PvP HUD on and off.",
		position = 18
	)
	default Keybind hudToggleKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer1Key",
		name = "Timer 1 Hotkey",
		description = "Press to start/stop manual countdown timer 1. Shown as T1 in the action strip.",
		position = 20
	)
	default Keybind timer1Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer1Duration",
		name = "Timer 1 Duration (s)",
		description = "How long timer 1 counts down in seconds.",
		position = 21
	)
	default int timer1Duration()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "timer2Key",
		name = "Timer 2 Hotkey",
		description = "Press to start/stop manual countdown timer 2. Shown as T2 in the action strip.",
		position = 22
	)
	default Keybind timer2Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer2Duration",
		name = "Timer 2 Duration (s)",
		description = "How long timer 2 counts down in seconds.",
		position = 23
	)
	default int timer2Duration()
	{
		return 300;
	}
}
