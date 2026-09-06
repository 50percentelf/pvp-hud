package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

import java.awt.Rectangle;

/**
 * Cached layout geometry for the HUD. Recomputed only on client resize,
 * chatbox bounds change, or mode change — never every frame.
 */
public class HudLayoutState
{
	/** Bounds of the chatbox widget; null until first layout pass. */
	@Getter @Setter
	private Rectangle chatboxBounds;

	/** Set to true to trigger a layout recompute on the next render pass. */
	@Getter @Setter
	private boolean dirty = true;

	/** Derived panel regions within chatboxBounds. */
	@Getter @Setter private Rectangle opponentPanel;
	@Getter @Setter private Rectangle eventPanel;
	@Getter @Setter private Rectangle selfPanel;
	@Getter @Setter private Rectangle boostRow;
	@Getter @Setter private Rectangle actionStrip;

	public void markDirty()
	{
		dirty = true;
	}

	public void reset()
	{
		chatboxBounds = null;
		opponentPanel = null;
		eventPanel = null;
		selfPanel = null;
		boostRow = null;
		actionStrip = null;
		dirty = true;
	}
}
