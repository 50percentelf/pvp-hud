package com.pvphud;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** Immutable render-ready description of one active effect in the buff strip. */
public final class ActiveEffectView
{
	/** Sprite/item icon, or {@code null} for text-only entries. */
	public final BufferedImage icon;

	/** Display label (e.g. {@code "VENG RDY"}, {@code "ICE 6s"}, {@code "DSC 4:42"}). */
	public final String label;

	/** Colour applied to both the label and any placeholder icon box. */
	public final Color color;

	public ActiveEffectView(BufferedImage icon, String label, Color color)
	{
		this.icon  = icon;
		this.label = label;
		this.color = color;
	}
}
