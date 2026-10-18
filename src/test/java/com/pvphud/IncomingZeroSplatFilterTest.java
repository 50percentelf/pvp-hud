package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

public class IncomingZeroSplatFilterTest
{
	@Test
	public void engaged_sameObjectRef_returnsTrue()
	{
		Object local = new Object();
		assertTrue(PvpHudPlugin.isTrackedOpponentTargetingUs(local, local));
	}

	@Test
	public void notEngaged_differentObjects_returnsFalse()
	{
		assertFalse(PvpHudPlugin.isTrackedOpponentTargetingUs(new Object(), new Object()));
	}

	@Test
	public void nullInteracting_returnsFalse()
	{
		assertFalse(PvpHudPlugin.isTrackedOpponentTargetingUs(null, new Object()));
	}
}
