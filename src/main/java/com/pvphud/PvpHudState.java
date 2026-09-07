package com.pvphud;

import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.ProtectionState;
import com.pvphud.state.PvpContextState;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.SelfState;
import lombok.Getter;

/**
 * Root state container for the PvP HUD.
 *
 * The overlay reads from this object; event handlers in the plugin update it.
 * All sub-states are pre-allocated at startup so combat initiation is cheap.
 *
 * Fight-specific data (event log, totals) live in the nullable currentSession.
 * The session persists across brief InteractingChanged nulls and only ends on
 * deliberate termination (death, disengagement timeout, logout).
 */
@Getter
public class PvpHudState
{
	private final PvpContextState  context     = new PvpContextState();
	private final OpponentState    opponent    = new OpponentState();
	private final SelfState        self        = new SelfState();
	private final ActionClockState actionClock = new ActionClockState();
	private final BoostState       boosts      = new BoostState();
	private final EffectState      effects     = new EffectState();
	private final ManualTimerState timer1      = new ManualTimerState();
	private final ManualTimerState timer2      = new ManualTimerState();
	private final HudLayoutState   layout      = new HudLayoutState();
	private final ProtectionState  protection  = new ProtectionState();

	/** Active fight session; null between fights. */
	private PvpFightSession currentSession;

	/** Wall-clock ms when the last session ended; -1 = no session has ended yet. */
	@Getter private long sessionEndedMs = -1;

	/** Totals from the most recently ended session (for post-fight display). */
	@Getter private int lastTotalOutgoing;
	@Getter private int lastTotalIncoming;

	/** Start a new fight session. Any previous session is discarded. */
	public void beginSession(String opponentName, int tick)
	{
		currentSession = new PvpFightSession(opponentName, tick);
	}

	/** Terminate the active session (fight ended). Null-safe. */
	public void endSession()
	{
		if (currentSession != null)
		{
			lastTotalOutgoing = currentSession.getTotalOutgoing();
			lastTotalIncoming = currentSession.getTotalIncoming();
			currentSession.terminate();
			currentSession = null;
			sessionEndedMs = System.currentTimeMillis();
		}
	}

	/** Full reset on logout, world-hop, or plugin shutdown. */
	public void fullReset()
	{
		context.reset();
		opponent.reset();
		self.reset();
		actionClock.reset();
		boosts.reset();
		effects.reset();
		layout.reset();
		protection.reset();
		endSession();
		sessionEndedMs = -1;
		lastTotalOutgoing = 0;
		lastTotalIncoming = 0;
	}
}
