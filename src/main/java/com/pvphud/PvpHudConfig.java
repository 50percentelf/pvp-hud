package com.pvphud;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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

	@Alpha
	@ConfigItem(
		keyName = "backgroundColor",
		name = "Background Color",
		description = "HUD background color and opacity. Lower alpha = more transparent."
	)
	default Color backgroundColor()
	{
		return new Color(20, 20, 20, 220);
	}
}
