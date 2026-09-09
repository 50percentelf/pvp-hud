package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/** Top-level context: zone and chat state for the local session. */
public class PvpContextState
{
	/** True while the player is in the Wilderness or a PvP world. */
	@Getter @Setter
	private volatile boolean inPvpZone;

	/** True when the normal chatbox should be shown instead of the HUD. */
	@Getter @Setter
	private boolean chatVisible;

	/** Current wilderness level (0 = not in wilderness). */
	@Getter @Setter
	private int wildernessLevel;

	/** True when standing in a multi-combat zone. */
	@Getter @Setter
	private boolean multiCombat;

	public void reset()
	{
		inPvpZone = false;
		chatVisible = false;
		wildernessLevel = 0;
		multiCombat = false;
	}
}
