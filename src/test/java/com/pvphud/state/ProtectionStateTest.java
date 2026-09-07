package com.pvphud.state;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProtectionStateTest
{
	// ── PJ safe timer ──────────────────────────────────────────────────────────

	@Test
	public void pjSafe_setOnAttack_wilderness()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(20);
		assertTrue(p.isPjSafe());
		assertEquals(20, p.getPjSafeTicksRemaining());
	}

	@Test
	public void pjSafe_setOnAttack_pvpWorld()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(16);
		assertTrue(p.isPjSafe());
		assertEquals(16, p.getPjSafeTicksRemaining());
	}

	@Test
	public void pjSafe_refreshedByEveryAttack()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(20);
		p.tick();
		p.tick();
		// 18 ticks remaining — another exchange resets it to 20
		p.onAttackExchanged(20);
		assertEquals(20, p.getPjSafeTicksRemaining());
	}

	@Test
	public void pjSafe_expiresAfterTicks()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(20);
		for (int i = 0; i < 20; i++) p.tick();
		assertFalse(p.isPjSafe());
		assertEquals(0, p.getPjSafeTicksRemaining());
	}

	// ── Combat logout lock (incoming only) ────────────────────────────────────

	@Test
	public void logoutLock_setOnIncomingHit()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		assertTrue(p.isInCombatLogoutLock());
		assertEquals(16, p.getCombatLogoutTicksRemaining());
	}

	@Test
	public void logoutLock_expiresAfter16Ticks()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		for (int i = 0; i < 16; i++) p.tick();
		assertFalse(p.isInCombatLogoutLock());
	}

	@Test
	public void logoutLock_notRefreshedByAttackExchanged()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		p.tick(); p.tick(); p.tick(); // 13 remaining
		p.onAttackExchanged(20);      // outgoing attack — must NOT refresh logout lock
		assertEquals(13, p.getCombatLogoutTicksRemaining());
	}

	// ── Under-attack lock ─────────────────────────────────────────────────────

	@Test
	public void underAttackLock_setOnIncomingHit()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		assertTrue(p.isUnderAttackLocked());
		assertEquals(20, p.getUnderAttackLockTicksRemaining());
	}

	@Test
	public void underAttackLock_expiresAfter20Ticks()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		for (int i = 0; i < 20; i++) p.tick();
		assertFalse(p.isUnderAttackLocked());
	}

	@Test
	public void underAttackLock_resetBySubsequentIncomingHit()
	{
		ProtectionState p = new ProtectionState();
		p.onIncomingHit();
		p.tick(); p.tick(); // 18 remaining
		p.onIncomingHit();  // reset
		assertEquals(20, p.getUnderAttackLockTicksRemaining());
	}

	// ── LMS post-kill immunity (LMS only) ─────────────────────────────────────

	@Test
	public void lmsImmune_setOnKillInLms()
	{
		ProtectionState p = new ProtectionState();
		p.onKillInLms();
		assertTrue(p.isLmsImmune());
		assertEquals(33, p.getLmsImmuneTicksRemaining());
	}

	@Test
	public void lmsImmune_expiresAfter33Ticks()
	{
		ProtectionState p = new ProtectionState();
		p.onKillInLms();
		for (int i = 0; i < 33; i++) p.tick();
		assertFalse(p.isLmsImmune());
	}

	@Test
	public void lmsImmune_notSetByNormalKill()
	{
		ProtectionState p = new ProtectionState();
		// Normal kill — only PJ refreshes, NOT LMS immunity
		p.onAttackExchanged(20);
		assertFalse(p.isLmsImmune());
		assertEquals(0, p.getLmsImmuneTicksRemaining());
	}

	// ── reset ─────────────────────────────────────────────────────────────────

	@Test
	public void reset_clearsAllTimers()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(20);
		p.onIncomingHit();
		p.onKillInLms();
		p.reset();
		assertFalse(p.isPjSafe());
		assertFalse(p.isInCombatLogoutLock());
		assertFalse(p.isUnderAttackLocked());
		assertFalse(p.isLmsImmune());
		assertEquals(0, p.getPjSafeTicksRemaining());
		assertEquals(0, p.getCombatLogoutTicksRemaining());
		assertEquals(0, p.getUnderAttackLockTicksRemaining());
		assertEquals(0, p.getLmsImmuneTicksRemaining());
	}

	// ── tick ──────────────────────────────────────────────────────────────────

	@Test
	public void tick_decrementsAllActiveTimers()
	{
		ProtectionState p = new ProtectionState();
		p.onAttackExchanged(20);
		p.onIncomingHit();
		p.onKillInLms();
		p.tick();
		assertEquals(19, p.getPjSafeTicksRemaining());
		assertEquals(15, p.getCombatLogoutTicksRemaining());
		assertEquals(19, p.getUnderAttackLockTicksRemaining());
		assertEquals(32, p.getLmsImmuneTicksRemaining());
	}

	@Test
	public void tick_doesNotGoNegative()
	{
		ProtectionState p = new ProtectionState();
		p.tick(); // nothing active
		assertEquals(0, p.getPjSafeTicksRemaining());
		assertEquals(0, p.getCombatLogoutTicksRemaining());
		assertEquals(0, p.getUnderAttackLockTicksRemaining());
		assertEquals(0, p.getLmsImmuneTicksRemaining());
	}
}
