package com.pvphud;

import com.pvphud.state.DamageExchange;
import org.junit.Test;
import static org.junit.Assert.*;

public class DamageExchangeFormatTest
{
	@Test
	public void singleHit_returnsHitValue()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addOutgoing(34, 0);
		assertEquals("34", PvpHudOverlay.formatExchangeDamage(ex, true));
	}

	@Test
	public void twoHits_returnsPlusFormat()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addOutgoing(20, 0);
		ex.addOutgoing(8, 0);
		assertEquals("20+8", PvpHudOverlay.formatExchangeDamage(ex, true));
	}

	@Test
	public void threeHits_returnsTotalAndCount()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addOutgoing(20, 0);
		ex.addOutgoing(8, 0);
		ex.addOutgoing(4, 0);
		assertEquals("32 (3)", PvpHudOverlay.formatExchangeDamage(ex, true));
	}

	@Test
	public void zeroHits_returnsEmpty()
	{
		DamageExchange ex = new DamageExchange(1);
		assertEquals("", PvpHudOverlay.formatExchangeDamage(ex, true));
	}

	@Test
	public void incoming_singleHit()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addIncoming(52);
		assertEquals("52", PvpHudOverlay.formatExchangeDamage(ex, false));
	}

	@Test
	public void incoming_twoHits_plusFormat()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addIncoming(35);
		ex.addIncoming(12);
		assertEquals("35+12", PvpHudOverlay.formatExchangeDamage(ex, false));
	}

	@Test
	public void outgoingStack_orderPreserved()
	{
		DamageExchange ex = new DamageExchange(1);
		ex.addOutgoing(20, 0);
		ex.addOutgoing(8, 0);
		String label = PvpHudOverlay.formatExchangeDamage(ex, true);
		// first stored hit should be the first added (20), second should be 8
		assertEquals("20+8", label);
	}
}
