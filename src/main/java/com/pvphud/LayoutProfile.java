package com.pvphud;

/** Immutable snapshot of the presentation settings for one HUD layout. */
class LayoutProfile
{
	final BuffStyle       buffStyle;
	final int             backgroundOpacity;
	final boolean         showBoostRow;
	final boolean         boostXOverX;
	final boolean         reserveBoostDockWhenHidden;
	final BarDisplayStyle hpBarStyle;
	final BarDisplayStyle prayerBarStyle;
	final BarDisplayStyle runBarStyle;
	final BarDisplayStyle specBarStyle;

	LayoutProfile(BuffStyle buffStyle, int backgroundOpacity,
		boolean showBoostRow, boolean boostXOverX, boolean reserveBoostDockWhenHidden,
		BarDisplayStyle hpBarStyle, BarDisplayStyle prayerBarStyle,
		BarDisplayStyle runBarStyle, BarDisplayStyle specBarStyle)
	{
		this.buffStyle                  = buffStyle;
		this.backgroundOpacity          = backgroundOpacity;
		this.showBoostRow               = showBoostRow;
		this.boostXOverX                = boostXOverX;
		this.reserveBoostDockWhenHidden = reserveBoostDockWhenHidden;
		this.hpBarStyle                 = hpBarStyle;
		this.prayerBarStyle             = prayerBarStyle;
		this.runBarStyle                = runBarStyle;
		this.specBarStyle               = specBarStyle;
	}

	/** Reads the profile for {@code layout} from the given config. */
	static LayoutProfile forLayout(HudLayout layout, PvpHudConfig config)
	{
		switch (layout)
		{
			case HORIZONTAL_FLOAT:
				return new LayoutProfile(
					config.hfBuffStyle(),
					config.hfBackgroundOpacity(),
					config.hfShowBoostRow(),
					config.hfBoostXOverX(),
					config.hfReserveBoostDockWhenHidden(),
					config.hfHpBarStyle(),
					config.hfPrayerBarStyle(),
					config.hfRunBarStyle(),
					config.hfSpecBarStyle()
				);
			case VERTICAL_FLOAT:
				return new LayoutProfile(
					config.vfBuffStyle(),
					config.vfBackgroundOpacity(),
					config.vfShowBoostRow(),
					config.vfBoostXOverX(),
					config.vfReserveBoostDockWhenHidden(),
					config.vfHpBarStyle(),
					config.vfPrayerBarStyle(),
					config.vfRunBarStyle(),
					config.vfSpecBarStyle()
				);
			case INVENTORY_HUG:
				return new LayoutProfile(
					config.ihBuffStyle(),
					config.ihBackgroundOpacity(),
					config.ihShowBoostRow(),
					config.ihBoostXOverX(),
					false,
					config.ihHpBarStyle(),
					config.ihPrayerBarStyle(),
					config.ihRunBarStyle(),
					config.ihSpecBarStyle()
				);
			case CHAT_LOCKED:
			default:
				return new LayoutProfile(
					config.buffStyle(),
					config.backgroundOpacity(),
					config.showBoostRow(),
					config.boostXOverX(),
					false,
					config.hpBarStyle(),
					config.prayerBarStyle(),
					config.runBarStyle(),
					config.specBarStyle()
				);
		}
	}
}
