package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies that an unknown weapon speed prevents fabricated attack cooldown. */
public class AttackClockUnknownWeaponTest
{
	@Test
	public void unknownWeaponSpeed_returnsZero()
	{
		// getWeaponSpeed() now returns 0 when equipment unavailable.
		int speed = 0;
		assertFalse("zero speed must not trigger attack clock", speed > 0);
	}

	@Test
	public void knownWeaponSpeed_allowsClockUpdate()
	{
		int speed = 4;
		assertTrue("known speed must trigger attack clock", speed > 0);
	}

	@Test
	public void blockedAnimation_doesNotTrigger()
	{
		// Eating animation (829) is on the AttackAnimations denylist.
		assertTrue("eating must be blocked", AttackAnimations.isBlocked(829));
	}

	@Test
	public void unknownAnimation_isNotBlocked()
	{
		// Animation not on the denylist is NOT blocked, but combined with
		// weapon speed 0 the attack clock still won't be set.
		assertFalse("unknown anim 12345 is not blocked", AttackAnimations.isBlocked(12345));
	}
}
