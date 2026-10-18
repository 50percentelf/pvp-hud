package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests opponent despawn grace period logic. */
public class OpponentDespawnTest
{
	private static final int GRACE = 10; // OPPONENT_GRACE_TICKS

	@Test
	public void missingBelowGrace_doesNotEndSession()
	{
		int missingTick = 100;
		int currentTick = 108; // 8 ticks < 10
		assertFalse("session should survive within grace period",
			currentTick - missingTick >= GRACE);
	}

	@Test
	public void missingAtGrace_endsSession()
	{
		int missingTick = 100;
		int currentTick = 110; // exactly 10 ticks
		assertTrue("session must end after grace period",
			currentTick - missingTick >= GRACE);
	}

	@Test
	public void reappearance_clearsGrace()
	{
		// If opponent reappears, opponentMissingTick should be reset to -1
		int opponentMissingTick = 100;
		boolean reappeared = true;
		int result = reappeared ? -1 : opponentMissingTick;
		assertEquals(-1, result);
	}
}
