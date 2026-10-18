package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

public class FightPanelDensityTest
{
	@Test
	public void horizontal_uses3Rows()
	{
		assertEquals(3, PvpHudOverlay.exchangeRowCount(false));
	}

	@Test
	public void vertical_uses2Rows()
	{
		assertEquals(2, PvpHudOverlay.exchangeRowCount(true));
	}
}
