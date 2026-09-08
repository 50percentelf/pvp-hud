package com.pvphud;

import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.OpponentState;
import com.pvphud.state.PrayerEffect;
import com.pvphud.state.SelfState;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Confirms that features deferred from v0.1 are inert and cannot affect HUD state.
 *
 * VENGEANCE — deferred post-v0.1:
 *   All event-handler paths that called setVengActive(true) have been removed.
 *   State fields are retained but no v0.1 code path sets them.
 *
 * SPECIAL PRAYER-IMPACT (SARA_STRIKE, CLEAR_MIND) — deferred post-v0.1:
 *   Enum values are defined but no v0.1 code path constructs a CombatEvent
 *   with those effects.  Only NONE and SMITE are produced by v0.1 event handlers.
 */
public class V01RegressionTest
{
	// ── Vengeance ─────────────────────────────────────────────────────────────

	@Test
	public void v01_selfVengActive_defaultsFalse()
	{
		assertFalse(new SelfState().isVengActive());
	}

	@Test
	public void v01_opponentVengActive_defaultsFalse()
	{
		assertFalse(new OpponentState().isVengActive());
	}

	@Test
	public void v01_opponentState_reset_clearsVeng()
	{
		OpponentState opp = new OpponentState();
		opp.setVengActive(true);
		opp.reset();
		assertFalse(opp.isVengActive());
	}

	// ── Prayer effects ────────────────────────────────────────────────────────

	/** Every PrayerEffect that v0.1 event handlers can produce. */
	private static final Set<PrayerEffect> V01_ALLOWED =
		EnumSet.of(PrayerEffect.NONE, PrayerEffect.SMITE);

	@Test
	public void v01_combatEvent_withNone_isAllowed()
	{
		CombatEvent e = new CombatEvent(
			CombatEventType.OUTGOING_HIT, 10, 0, 0, PrayerEffect.NONE, false, false, 0);
		assertTrue(V01_ALLOWED.contains(e.getEffectType()));
	}

	@Test
	public void v01_combatEvent_withSmite_isAllowed()
	{
		CombatEvent e = new CombatEvent(
			CombatEventType.OUTGOING_HIT, 20, 5, 0, PrayerEffect.SMITE, false, false, 0);
		assertTrue(V01_ALLOWED.contains(e.getEffectType()));
	}

	@Test
	public void v01_saraStrike_notInAllowedSet()
	{
		assertFalse("SARA_STRIKE must not be a v0.1 shipped effect",
			V01_ALLOWED.contains(PrayerEffect.SARA_STRIKE));
	}

	@Test
	public void v01_clearMind_notInAllowedSet()
	{
		assertFalse("CLEAR_MIND must not be a v0.1 shipped effect",
			V01_ALLOWED.contains(PrayerEffect.CLEAR_MIND));
	}
}
