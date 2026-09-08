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
		name = "Show HUD (testing)",
		description = "Temporarily force the HUD visible for visual testing.",
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

	// ── Chat Locked section ──────────────────────────────────────────────────
	// Key names intentionally match the previous (unsectioned) release so that
	// existing user settings survive the refactor without a migration step.

	@ConfigSection(
		name = "Chat Locked",
		description = "Presentation settings for the Chat Locked layout",
		position = 10
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
		position = 20
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
		position = 30
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
		position = 40
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
}
