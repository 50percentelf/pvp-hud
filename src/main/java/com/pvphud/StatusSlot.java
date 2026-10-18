package com.pvphud;

import java.awt.Color;

/** Immutable semantic projection unit. One slot per visible effect row in the HUD. */
public final class StatusSlot
{
	/**
	 * Stable identifier used by {@link PvpHudOverlay#getIconFor} to resolve the effect icon.
	 * Also used as a diagnostic key in tests and manifests.
	 */
	public final String         slotId;
	public final StatusPriority priority;

	/** Short label for icon-assisted modes (VERTICAL_BAR, ICON_TRAY). e.g. "1:42", "12s". */
	public final String         label;

	/**
	 * Full label for TEXT mode where no icon provides context. e.g. "DSC 1:42", "ICE 12s".
	 * {@code null} means TEXT mode falls back to {@link #label}.
	 */
	public final String         textLabel;

	/** Colour applied to the label text and any placeholder icon box. */
	public final Color          color;

	/** Optional highlight border colour for the icon cell. {@code null} = no highlight. */
	public final Color          highlightColor;

	/**
	 * True for compound sub-entries (e.g. "next proc" or "next hit" lines).
	 * ICON_TRAY and 2-col renderers skip secondary entries; TEXT and VERTICAL_BAR show them.
	 */
	public final boolean        isSecondary;

	public StatusSlot(String slotId, StatusPriority priority, String label, String textLabel,
		Color color, Color highlightColor, boolean isSecondary)
	{
		this.slotId         = slotId;
		this.priority       = priority;
		this.label          = label;
		this.textLabel      = textLabel;
		this.color          = color;
		this.highlightColor = highlightColor;
		this.isSecondary    = isSecondary;
	}

	public StatusSlot(String slotId, StatusPriority priority, String label, String textLabel,
		Color color, boolean isSecondary)
	{
		this(slotId, priority, label, textLabel, color, null, isSecondary);
	}

	/** Returns the label to use in TEXT mode. */
	public String getTextLabel()
	{
		return textLabel != null ? textLabel : label;
	}
}
