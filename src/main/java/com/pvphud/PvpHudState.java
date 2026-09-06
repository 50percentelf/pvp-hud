package com.pvphud;

import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEventState;
import com.pvphud.state.EffectState;
import com.pvphud.state.HudLayoutState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.PvpContextState;
import com.pvphud.state.SelfState;
import lombok.Getter;

/**
 * Root state container for the PvP HUD.
 *
 * The overlay reads from this object; event handlers in the plugin update it.
 * All sub-states are pre-allocated at startup so combat initiation is cheap.
 */
@Getter
public class PvpHudState
{
	private final PvpContextState context     = new PvpContextState();
	private final OpponentState   opponent    = new OpponentState();
	private final SelfState       self        = new SelfState();
	private final ActionClockState actionClock = new ActionClockState();
	private final BoostState      boosts      = new BoostState();
	private final EffectState     effects     = new EffectState();
	private final ManualTimerState timer1     = new ManualTimerState();
	private final ManualTimerState timer2     = new ManualTimerState();
	private final CombatEventState combatEvent = new CombatEventState();
	private final HudLayoutState  layout      = new HudLayoutState();

	/**
	 * Called when PvP combat begins. Resets transient combat state but
	 * preserves self-state (boosts, effects) which pre-existed combat.
	 */
	public void onCombatStart(String opponentName)
	{
		context.setPvpActive(true);
		opponent.reset();
		opponent.setName(opponentName);
		combatEvent.clear();
	}

	/** Called when PvP combat ends or the opponent is lost. */
	public void onCombatEnd()
	{
		context.setPvpActive(false);
		opponent.reset();
		combatEvent.clear();
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
		combatEvent.clear();
		layout.reset();
	}
}
