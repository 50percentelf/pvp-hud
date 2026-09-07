package com.pvphud;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("pvp-hud")
public interface PvpHudConfig extends Config
{
	@ConfigItem(
		keyName = "hudMode",
		name = "HUD Mode",
		description = "When the PvP HUD is displayed: in PvP areas, automatically during combat, or manually via hotkey"
	)
	default HudMode hudMode()
	{
		return HudMode.MANUAL;
	}

	@ConfigItem(
		keyName = "hudVisible",
		name = "Show HUD (testing)",
		description = "Temporarily force the HUD visible for visual testing. Will be replaced by the hotkey toggle."
	)
	default boolean hudVisible()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hudLayout",
		name = "HUD Layout",
		description = "Chat Locked: fills the chatbox area. Horizontal Float: same size, freely draggable. Vertical Float: narrow side panel."
	)
	default HudLayout hudLayout()
	{
		return HudLayout.CHAT_LOCKED;
	}

	@ConfigItem(
		keyName = "buffStyle",
		name = "Buff Display Style",
		description = "Text: labels only. Vertical Bar: icon + timer per row. Icon Tray: all icons in a horizontal strip."
	)
	default BuffStyle buffStyle()
	{
		return BuffStyle.VERTICAL_BAR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "backgroundOpacity",
		name = "Background Opacity",
		description = "HUD background opacity: 0 = fully transparent, 255 = fully opaque. Default: 220."
	)
	default int backgroundOpacity()
	{
		return 220;
	}

	@ConfigItem(
		keyName = "selfBarStyle",
		name = "HP / Prayer Display",
		description = "How HP and Prayer are shown in the YOU panel."
	)
	default SelfBarStyle selfBarStyle()
	{
		return SelfBarStyle.BARS_AND_NUMBERS;
	}

	@ConfigItem(
		keyName = "boostXOverX",
		name = "Show Boosts as Boosted/Base",
		description = "Boost row shows the actual boosted level vs base level (e.g. 115/99) instead of the delta (+16)."
	)
	default boolean boostXOverX()
	{
		return false;
	}

	@ConfigItem(
		keyName = "specDisplay",
		name = "Spec Display",
		description = "Labeled: 'SPEC 75' in action strip. Number: '75%'. Bar: thin bar drawn on strip background."
	)
	default SpecDisplay specDisplay()
	{
		return SpecDisplay.LABELED;
	}

	@ConfigItem(
		keyName = "timer1Key",
		name = "Timer 1 Hotkey",
		description = "Press to start/stop manual countdown timer 1. Shown as T1 in the action strip while running."
	)
	default Keybind timer1Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer1Duration",
		name = "Timer 1 Duration (s)",
		description = "How long timer 1 counts down in seconds."
	)
	default int timer1Duration()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "timer2Key",
		name = "Timer 2 Hotkey",
		description = "Press to start/stop manual countdown timer 2. Shown as T2 in the action strip while running."
	)
	default Keybind timer2Key()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "timer2Duration",
		name = "Timer 2 Duration (s)",
		description = "How long timer 2 counts down in seconds."
	)
	default int timer2Duration()
	{
		return 300;
	}
}
