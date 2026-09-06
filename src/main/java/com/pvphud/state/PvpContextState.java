package com.pvphud.state;

import com.pvphud.HudMode;
import lombok.Getter;
import lombok.Setter;

/** Top-level context: whether PvP mode is active and how it was triggered. */
public class PvpContextState
{
	@Getter @Setter
	private boolean pvpActive;

	@Getter @Setter
	private HudMode mode = HudMode.MANUAL;

	/** True while the player is in the Wilderness or a PvP world. */
	@Getter @Setter
	private boolean inPvpZone;

	/** True when the normal chatbox should be shown instead of the HUD. */
	@Getter @Setter
	private boolean chatVisible;

	public void reset()
	{
		pvpActive = false;
		inPvpZone = false;
		chatVisible = false;
	}
}
