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

	/** Semantic priority: drives display order, group separators, and overflow policy. */
	public final StatusPriority priority;

	/**
	 * True for compound sub-entries (e.g. "next proc" or "next hit" lines).
	 * ICON_TRAY and 2-col renderers skip secondary entries; TEXT and VERTICAL_BAR show them.
	 */
	public final boolean isSecondary;

	/** Full constructor. */
	public ActiveEffectView(BufferedImage icon, String label, String textLabel,
		Color color, Color highlightColor, StatusPriority priority, boolean isSecondary)
	{
		this.icon           = icon;
		this.label          = label;
		this.textLabel      = textLabel;
		this.color          = color;
		this.highlightColor = highlightColor;
		this.priority       = priority;
		this.isSecondary    = isSecondary;
	}

	/** No highlight. */
	public ActiveEffectView(BufferedImage icon, String label, String textLabel,
		Color color, StatusPriority priority, boolean isSecondary)
	{
		this(icon, label, textLabel, color, null, priority, isSecondary);
	}

	/** label == textLabel, no highlight, not secondary. */
	public ActiveEffectView(BufferedImage icon, String label, Color color, StatusPriority priority)
	{
		this(icon, label, null, color, null, priority, false);
	}

	/** Returns the label to use in TEXT mode. */
	public String getTextLabel()
	{
		return textLabel != null ? textLabel : label;
	}
}
