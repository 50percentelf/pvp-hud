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

	/**
	 * Optional highlight border drawn around the icon cell. {@code null} = no highlight.
	 * Set for freeze and teleblock effects when the user has configured a highlight colour.
	 */
	public final Color highlightColor;

	/** Semantic category: drives display order, group separators, and overflow priority. */
	public final EffectGroup group;

	/** Full constructor. */
	public ActiveEffectView(BufferedImage icon, String label, String textLabel,
		Color color, Color highlightColor, EffectGroup group)
	{
		this.icon           = icon;
		this.label          = label;
		this.textLabel      = textLabel;
		this.color          = color;
		this.highlightColor = highlightColor;
		this.group          = group;
	}

	/** No highlight. */
	public ActiveEffectView(BufferedImage icon, String label, String textLabel,
		Color color, EffectGroup group)
	{
		this(icon, label, textLabel, color, null, group);
	}

	/** label == textLabel, no highlight. */
	public ActiveEffectView(BufferedImage icon, String label, Color color, EffectGroup group)
	{
		this(icon, label, null, color, null, group);
	}

	/** Returns the label to use in TEXT mode. */
	public String getTextLabel()
	{
		return textLabel != null ? textLabel : label;
	}
}
