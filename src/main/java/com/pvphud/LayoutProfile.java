package com.pvphud;

/** Immutable snapshot of the presentation settings for one HUD layout. */
class LayoutProfile
{
	final BuffStyle       buffStyle;
	final int             backgroundOpacity;
	final boolean         showBoostRow;
	final boolean         boostXOverX;
	final boolean         reserveBoostDockWhenHidden;
	final boolean         chatLocked;
	final BarDisplayStyle hpBarStyle;
	final BarDisplayStyle prayerBarStyle;
	final BarDisplayStyle runBarStyle;
	final BarDisplayStyle specBarStyle;

	LayoutProfile(BuffStyle buffStyle, int backgroundOpacity,
		boolean showBoostRow, boolean boostXOverX, boolean reserveBoostDockWhenHidden,
		boolean chatLocked,
		BarDisplayStyle hpBarStyle, BarDisplayStyle prayerBarStyle,
		BarDisplayStyle runBarStyle, BarDisplayStyle specBarStyle)
	{
		this.buffStyle                  = buffStyle;
		this.backgroundOpacity          = backgroundOpacity;
		this.showBoostRow               = showBoostRow;
		this.boostXOverX                = boostXOverX;
		this.reserveBoostDockWhenHidden = reserveBoostDockWhenHidden;
		this.chatLocked                 = chatLocked;
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
			{
				boolean chatLocked = config.hfChatLocked();
				return new LayoutProfile(
					config.hfBuffStyle(),
					config.hfBackgroundOpacity(),
					config.hfShowBoostRow(),
					config.hfBoostXOverX(),
					chatLocked ? false : config.hfReserveBoostDockWhenHidden(),
					chatLocked,
					config.hfHpBarStyle(),
					config.hfPrayerBarStyle(),
					config.hfRunBarStyle(),
					config.hfSpecBarStyle()
				);
			}
			case VERTICAL_FLOAT:
				return new LayoutProfile(
					config.vfBuffStyle(),
					config.vfBackgroundOpacity(),
					config.vfShowBoostRow(),
					config.vfBoostXOverX(),
					config.vfReserveBoostDockWhenHidden(),
					false,
					config.vfHpBarStyle(),
					config.vfPrayerBarStyle(),
					config.vfRunBarStyle(),
					config.vfSpecBarStyle()
				);
			case INVENTORY_HUG:
			default:
				return new LayoutProfile(
					config.ihBuffStyle(),
					config.ihBackgroundOpacity(),
					config.ihShowBoostRow(),
					config.ihBoostXOverX(),
					false,
					false,
					config.ihHpBarStyle(),
					config.ihPrayerBarStyle(),
					config.ihRunBarStyle(),
					config.ihSpecBarStyle()
				);
		}
	}
}
