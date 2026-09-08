package com.pvphud;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** Immutable render-ready description of one active effect in the buff strip. */
public final class ActiveEffectView
{
	/** Sprite/item icon, or {@code null} for text-only entries. */
	public final BufferedImage icon;

	/** Short label for icon-assisted modes (VERTICAL_BAR, ICON_TRAY). e.g. "VENG", "4:42". */
	public final String label;

	/**
	 * Full label for TEXT mode where no icon provides context. e.g. "VENG RDY", "DSC 4:42".
	 * {@code null} means TEXT mode falls back to {@link #label}.
	 */
	public final String textLabel;

	/** Colour applied to both the label text and any placeholder icon box. */
	public final Color color;

	/** Entry where TEXT and icon-mode labels differ. */
	public ActiveEffectView(BufferedImage icon, String label, String textLabel, Color color)
	{
		this.icon      = icon;
		this.label     = label;
		this.textLabel = textLabel;
		this.color     = color;
	}

	/** Entry where TEXT and icon-mode labels are identical. */
	public ActiveEffectView(BufferedImage icon, String label, Color color)
	{
		this(icon, label, null, color);
	}

	/** Returns the label to use in TEXT mode. */
	public String getTextLabel()
	{
		return textLabel != null ? textLabel : label;
	}
}
