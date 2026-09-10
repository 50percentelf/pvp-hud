package com.pvphud;

import com.pvphud.PvpHudPlugin;
import com.pvphud.state.SelfState;
import net.runelite.api.GraphicID;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies that the freeze timer:
 *  - starts only from the confirmed "frozen" event (chat message),
 *  - is stored as startTick/endTick so remaining time is derived from the
 *    current tick rather than blindly decremented,
 *  - is NOT extended or reset by repeated ice impacts while already frozen,
 *  - uses RuneLite-matching base durations for all freeze spell types, and
 *  - is cleared by reset() / clearFreeze().
 */
public class SelfFreezeTimerTest
{
	// ── Base durations (must match PvpHudPlugin.freezeTicksForGraphic) ──────────

	@Test
	public void iceRush_baseDuration()
	{
		assertEquals(8, PvpHudPlugin.freezeTicksForGraphic(GraphicID.ICE_RUSH));
	}

	@Test
	public void iceBurst_baseDuration()
	{
		assertEquals(16, PvpHudPlugin.freezeTicksForGraphic(GraphicID.ICE_BURST));
	}

	@Test
	public void iceBlitz_baseDuration()
	{
		assertEquals(24, PvpHudPlugin.freezeTicksForGraphic(GraphicID.ICE_BLITZ));
	}

	@Test
	public void iceBarrage_baseDuration()
	{
		assertEquals(32, PvpHudPlugin.freezeTicksForGraphic(GraphicID.ICE_BARRAGE));
	}

	@Test
	public void bind_baseDuration()
	{
		assertEquals(8, PvpHudPlugin.freezeTicksForGraphic(SpotanimID.BIND_IMPACT));
	}

	@Test
	public void snare_baseDuration()
	{
		assertEquals(16, PvpHudPlugin.freezeTicksForGraphic(SpotanimID.SNARE_IMPACT));
	}

	@Test
	public void entangle_baseDuration()
	{
		assertEquals(24, PvpHudPlugin.freezeTicksForGraphic(SpotanimID.ENTANGLE_IMPACT));
	}

	@Test
	public void unknownGraphic_returnsZero()
	{
		assertEquals(0, PvpHudPlugin.freezeTicksForGraphic(999));
		assertEquals(0, PvpHudPlugin.freezeTicksForGraphic(0));
		assertEquals(0, PvpHudPlugin.freezeTicksForGraphic(-1));
	}

	// ── applyFreeze — startTick/endTick semantics ─────────────────────────────

	@Test
	public void applyFreeze_setsTimerCorrectly()
	{
		SelfState s = new SelfState();
		s.applyFreeze(10, 32, SpriteID.SPELL_ICE_BARRAGE);
		assertEquals(32, s.getFreezeTicksRemaining(10));
		assertEquals(1,  s.getFreezeTicksRemaining(41));
		assertEquals(0,  s.getFreezeTicksRemaining(42)); // exactly expired
	}

	@Test
	public void applyFreeze_remainingDerivesFromCurrentTick()
	{
		// Derived — not a counter: asking at different ticks gives correct answers
		// without needing any tick() / decrement calls.
		SelfState s = new SelfState();
		s.applyFreeze(0, 24, SpriteID.SPELL_ICE_BLITZ);
		assertEquals(24, s.getFreezeTicksRemaining(0));
		assertEquals(20, s.getFreezeTicksRemaining(4));
		assertEquals(12, s.getFreezeTicksRemaining(12));
		assertEquals(1,  s.getFreezeTicksRemaining(23));
		assertEquals(0,  s.getFreezeTicksRemaining(24));
	}

	@Test
	public void applyFreeze_pastEndTick_returnsZero()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 8, SpriteID.SPELL_ICE_RUSH);
		assertEquals(0, s.getFreezeTicksRemaining(100)); // well past expiry
	}

	// ── Repeated ice impacts while frozen must NOT extend the timer ──────────

	@Test
	public void repeatedImpact_doesNotExtendTimer()
	{
		SelfState s = new SelfState();
		s.applyFreeze(10, 32, SpriteID.SPELL_ICE_BARRAGE); // barrage at tick 10, ends at 42
		s.applyFreeze(15, 32, SpriteID.SPELL_ICE_BARRAGE); // second barrage at tick 15 — ignored
		// Timer must still end at tick 42, not 47
		assertEquals(27, s.getFreezeTicksRemaining(15));
		assertEquals(0,  s.getFreezeTicksRemaining(42));
	}

	@Test
	public void repeatedImpact_differentSpell_doesNotResetDuration()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE); // barrage at tick 0
		s.applyFreeze(5, 8, SpriteID.SPELL_ICE_RUSH);     // rush at tick 5 — ignored while frozen
		assertEquals(27, s.getFreezeTicksRemaining(5));    // 32 - 5 = 27, not 8
	}

	@Test
	public void newFreezeApplied_afterPreviousExpires()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 8, SpriteID.SPELL_ICE_RUSH);   // expires at tick 8
		s.applyFreeze(8, 32, SpriteID.SPELL_ICE_BARRAGE); // tick 8 = new freeze allowed
		assertEquals(32, s.getFreezeTicksRemaining(8));
	}

	// ── isFrozen ─────────────────────────────────────────────────────────────

	@Test
	public void isFrozen_trueWhileActive()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 16, SpriteID.SPELL_ICE_BURST);
		assertTrue(s.isFrozen(0));
		assertTrue(s.isFrozen(15));
	}

	@Test
	public void isFrozen_falseAtExpiry()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 16, SpriteID.SPELL_ICE_BURST);
		assertFalse(s.isFrozen(16));
		assertFalse(s.isFrozen(100));
	}

	@Test
	public void isFrozen_falseWhenNoFreezeApplied()
	{
		SelfState s = new SelfState();
		assertFalse(s.isFrozen(0));
		assertFalse(s.isFrozen(1000));
	}

	// ── Cancellation / reset ─────────────────────────────────────────────────

	@Test
	public void clearFreeze_cancelsActiveTimer()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.clearFreeze();
		assertFalse(s.isFrozen(5));
		assertEquals(0, s.getFreezeTicksRemaining(5));
		assertEquals(0, s.getFreezeSpriteId());
	}

	@Test
	public void reset_clearsActiveFreezeTimer()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.reset();
		assertFalse(s.isFrozen(10));
		assertEquals(0, s.getFreezeTicksRemaining(10));
		assertEquals(0, s.getFreezeSpriteId());
	}

	@Test
	public void afterClear_newFreezeCanBeApplied()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.clearFreeze();
		s.applyFreeze(5, 24, SpriteID.SPELL_ICE_BLITZ); // new freeze after cancellation
		assertEquals(24, s.getFreezeTicksRemaining(5));
	}

	// ── No freeze by default ─────────────────────────────────────────────────

	@Test
	public void noFreezeByDefault()
	{
		SelfState s = new SelfState();
		assertEquals(0, s.getFreezeTicksRemaining(0));
		assertEquals(0, s.getFreezeSpriteId());
		assertFalse(s.isFrozen(0));
	}
}
