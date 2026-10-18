package com.pvphud;

import com.pvphud.state.SelfState;
import net.runelite.api.SpriteID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validates the two distinct freeze-termination paths:
 *   A. expireFreeze — natural timer expiry → grants refreeze immunity
 *   B. breakFreezeEarly — movement / opponent-death → no immunity
 */
public class FreezeLifecycleTest
{
	// A — natural expiry

	@Test
	public void expireFreeze_clearsFreezeState()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.expireFreeze(32);
		assertFalse(s.isFrozen(32));
		assertEquals(0, s.getFreezeTicksRemaining(32));
		assertEquals(-1, s.getFreezeStartTick());
	}

	@Test
	public void expireFreeze_grantsRefreezeImmunity()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.expireFreeze(32);
		assertTrue("refreeze immunity must be granted after natural expiry", s.isRefreezeImmune(32));
		assertTrue(s.isRefreezeImmune(36));   // immunity still active
		assertFalse(s.isRefreezeImmune(37));  // immunity ended at 32 + 5 = 37
	}

	// B — early break (movement / opponent death)

	@Test
	public void breakFreezeEarly_clearsFreezeState()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.breakFreezeEarly();
		assertFalse(s.isFrozen(10));
		assertEquals(-1, s.getFreezeStartTick());
		assertEquals(0, s.getFreezeSpriteId());
	}

	@Test
	public void breakFreezeEarly_doesNotGrantImmunity()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.breakFreezeEarly();
		assertFalse("breakFreezeEarly must not grant refreeze immunity", s.isRefreezeImmune(1));
		assertFalse(s.isRefreezeImmune(10));
	}

	@Test
	public void breakFreezeEarly_whenNotFrozen_isNoop()
	{
		SelfState s = new SelfState();
		s.breakFreezeEarly(); // should not throw; should not affect immunity
		assertFalse(s.isRefreezeImmune(0));
		assertEquals(-1, s.getFreezeStartTick());
	}

	// C — clearFreeze (logout / death) — always existed, verify still works

	@Test
	public void clearFreeze_clearsStateAndImmunity()
	{
		SelfState s = new SelfState();
		s.applyFreeze(0, 32, SpriteID.SPELL_ICE_BARRAGE);
		s.expireFreeze(32); // gain immunity
		assertTrue(s.isRefreezeImmune(32));
		s.clearFreeze();    // should wipe immunity too
		assertFalse(s.isRefreezeImmune(32));
		assertEquals(-1, s.getFreezeStartTick());
	}
}
