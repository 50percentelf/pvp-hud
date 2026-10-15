package com.pvphud;

import com.pvphud.debug.DebugAntifireType;
import com.pvphud.debug.DebugFreezeType;
import com.pvphud.debug.DebugPoisonType;
import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.EffectState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.OpponentStats;
import com.pvphud.state.PoisonState;
import com.pvphud.state.ProtectionState;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.PrayerEffect;
import com.pvphud.state.SelfState;
import net.runelite.api.HeadIcon;
import net.runelite.api.SpriteID;

/**
 * Populates a PvpHudState from debug config values.
 * Called every render frame; fullReset() ensures no stale state bleeds between frames.
 */
class DebugStateBuilder
{
	/** Apply debug config to state. Resets state fully before writing. */
	static void apply(PvpHudConfig config, PvpHudState state, int currentTick)
	{
		state.fullReset();
		// fullReset() does not clear manual timers — reset explicitly so preset state
		// from a previous frame doesn't bleed through when the preset is toggled off.
		state.getTimer1().clear();
		state.getTimer2().clear();

		if (config.debugPresetAllEffects())
		{
			applyPreset(state, currentTick);
			return;
		}

		applySelf(config, state, currentTick);
		applyEffects(config, state, currentTick);
		applyPoison(config, state, currentTick);
		applyBoosts(config, state);
		applyActionClocks(config, state);
		applyOpponent(config, state);
		applyProtection(config, state);
		applyContext(config, state);

		if (config.debugInjectHits())
		{
			applyFakeSession(config, state, currentTick);
		}
	}

	// ── Individual sections ───────────────────────────────────────────────────

	private static void applySelf(PvpHudConfig config, PvpHudState state, int currentTick)
	{
		SelfState self = state.getSelf();
		self.setCurrentHp(config.debugHp());
		self.setMaxHp(config.debugMaxHp());
		self.setCurrentPrayer(config.debugPrayer());
		self.setMaxPrayer(config.debugMaxPrayer());
		self.setRunEnergy(config.debugRunEnergy());
		self.setVengActive(config.debugVengeance());

		DebugFreezeType ft = config.debugFreezeType();
		if (ft != DebugFreezeType.NONE)
		{
			int ticks = config.debugFreezeTicks();
			if (ticks > 0)
			{
				// Freeze started last tick, expires 'ticks' ticks from now.
				self.applyFreeze(currentTick - 1, ticks + 1, ft.spriteId);
			}
		}

		if (config.debugTeleblock())
		{
			self.setTeleBlockTicksRemaining(300);
		}
	}

	private static void applyEffects(PvpHudConfig config, PvpHudState state, int currentTick)
	{
		EffectState fx = state.getEffects();
		fx.setSpecEnergy(config.debugSpecEnergy());

		if (config.debugDivineSupercombat())  fx.setDivineSupercombatTicks(500);
		if (config.debugDivineRanging())       fx.setDivineRangingTicks(500);
		if (config.debugDivineMagic())         fx.setDivineMagicTicks(500);
		if (config.debugDivineBastion())       fx.setDivineBastionTicks(500);
		if (config.debugDivineBattlemage())    fx.setDivineBattlemageTicks(500);

		if (config.debugMenaphiteRemedy())
		{
			fx.getMenaphite().setTotalTicksRemaining(300);
			fx.getMenaphite().setNextProcTicks(20);
		}

		if (config.debugStamina())
		{
			fx.setStaminaEffectTicks(200);
		}

		switch (config.debugAntifire())
		{
			case NORMAL:
				fx.setNextAntifireTick(currentTick + 30);
				fx.setAntifireVarbitValue(20);
				break;
			case EXTENDED:
				fx.setNextAntifireTick(currentTick + 30);
				fx.setAntifireVarbitValue(40);
				break;
			case SUPER:
				fx.setNextSuperAntifireTick(currentTick + 20);
				fx.setSuperAntifireVarbitValue(15);
				break;
			case SUPER_EXTENDED:
				fx.setNextSuperAntifireTick(currentTick + 20);
				fx.setSuperAntifireVarbitValue(30);
				break;
			default:
				break;
		}
	}

	private static void applyPoison(PvpHudConfig config, PvpHudState state, int currentTick)
	{
		PoisonState poison = state.getPoison();
		switch (config.debugPoisonState())
		{
			case POISONED:
				poison.setPoisoned(true);
				break;
			case VENOMED:
				poison.setVenomed(true);
				break;
			case ANTI_POISON:
				poison.setAntiPoisonActive(true);
				poison.setPoisonVarpValue(-19); // mid-range anti-poison
				poison.setNextPoisonTick(currentTick + 30);
				break;
			case ANTI_VENOM:
				poison.setAntiVenomActive(true);
				poison.setPoisonVarpValue(-58); // mid-range anti-venom
				poison.setNextPoisonTick(currentTick + 30);
				break;
			default:
				break;
		}
	}

	private static void applyBoosts(PvpHudConfig config, PvpHudState state)
	{
		BoostState boosts = state.getBoosts();
		final int base = 99;
		boosts.setAttackReal(base);
		boosts.setAttackBoosted(base + config.debugBoostAtk());
		boosts.setStrengthReal(base);
		boosts.setStrengthBoosted(base + config.debugBoostStr());
		boosts.setDefenceReal(base);
		boosts.setDefenceBoosted(base + config.debugBoostDef());
		boosts.setRangedReal(base);
		boosts.setRangedBoosted(base + config.debugBoostRng());
		boosts.setMagicReal(base);
		boosts.setMagicBoosted(base + config.debugBoostMag());
	}

	private static void applyActionClocks(PvpHudConfig config, PvpHudState state)
	{
		ActionClockState clock = state.getActionClock();
		clock.setAttackDelayTicks(config.debugAttackDelay());
		clock.setLastWeaponSpeedTicks(Math.max(config.debugAttackDelay(), 5));
		clock.setEatCooldownTicks(config.debugEatCooldown());
		clock.setPotCooldownTicks(config.debugPotCooldown());
	}

	private static void applyOpponent(PvpHudConfig config, PvpHudState state)
	{
		String name = config.debugOpponentName();
		if (name == null || name.trim().isEmpty())
		{
			name = "Opponent";
		}
		OpponentState opp = state.getOpponent();
		opp.setName(name);
		opp.setEstimatedHp(config.debugOpponentHp());
		opp.setMaxHp(config.debugOpponentMaxHp());
		opp.setVengActive(config.debugOpponentVengeance());
		opp.setOverheadPrayer(config.debugOpponentPrayer().toHeadIcon());

		OpponentStats stats = opp.getStats();
		stats.setAttack(99);
		stats.setStrength(99);
		stats.setDefence(99);
		stats.setRanged(99);
		stats.setMagic(99);
		stats.setHitpoints(99);
	}

	private static void applyProtection(PvpHudConfig config, PvpHudState state)
	{
		ProtectionState prot = state.getProtection();
		prot.setPjSafeTicksRemaining(config.debugPjTimer());
		prot.setCombatLogoutTicksRemaining(config.debugCombatLogoutLock());
		prot.setUnderAttackLockTicksRemaining(config.debugUnderAttackLock());
	}

	private static void applyContext(PvpHudConfig config, PvpHudState state)
	{
		state.getContext().setWildernessLevel(config.debugWildernessLevel());
		state.getContext().setMultiCombat(config.debugMultiCombat());
	}

	private static void applyFakeSession(PvpHudConfig config, PvpHudState state, int currentTick)
	{
		String name = config.debugOpponentName();
		if (name == null || name.trim().isEmpty()) name = "Opponent";
		state.beginSession(name, currentTick - 5);
		PvpFightSession session = state.getCurrentSession();
		long now = System.currentTimeMillis();
		session.onOutgoingHit(CombatEvent.outgoing(42, now - 3600), currentTick - 3);
		session.onIncomingHit(CombatEvent.incoming(17, now - 3600), currentTick - 3);
		session.onOutgoingHit(CombatEvent.outgoing(28, now - 2400), currentTick - 2);
		session.onIncomingHit(
			new CombatEvent(CombatEventType.INCOMING_HIT, 35, 8, 0, PrayerEffect.SMITE, false, false, now - 2400),
			currentTick - 2);
		session.onOutgoingHit(
			new CombatEvent(CombatEventType.OUTGOING_HIT, 99, 0, 0, PrayerEffect.NONE, true, false, now - 1200),
			currentTick - 1);
	}

	// ── Preset: all effects at representative values ──────────────────────────

	private static void applyPreset(PvpHudState state, int currentTick)
	{
		SelfState self = state.getSelf();
		self.setCurrentHp(57);
		self.setMaxHp(99);
		self.setCurrentPrayer(44);
		self.setMaxPrayer(99);
		self.setRunEnergy(65);
		self.setVengActive(true);
		self.applyFreeze(currentTick - 1, 25, SpriteID.SPELL_ICE_BARRAGE);
		self.setTeleBlockTicksRemaining(300);

		EffectState fx = state.getEffects();
		fx.setSpecEnergy(75);
		fx.setDivineSupercombatTicks(500);
		fx.setDivineRangingTicks(420);
		fx.setDivineMagicTicks(360);
		fx.setDivineBastionTicks(480);
		fx.setDivineBattlemageTicks(310);
		fx.getMenaphite().setTotalTicksRemaining(300);
		fx.getMenaphite().setNextProcTicks(20);
		fx.setStaminaEffectTicks(200);
		fx.setNextSuperAntifireTick(currentTick + 20);
		fx.setSuperAntifireVarbitValue(15);

		PoisonState poison = state.getPoison();
		poison.setAntiVenomActive(true);
		poison.setPoisonVarpValue(-58);
		poison.setNextPoisonTick(currentTick + 30);

		BoostState boosts = state.getBoosts();
		boosts.setAttackReal(99);   boosts.setAttackBoosted(118);
		boosts.setStrengthReal(99); boosts.setStrengthBoosted(118);
		boosts.setDefenceReal(99);  boosts.setDefenceBoosted(118);
		boosts.setRangedReal(99);   boosts.setRangedBoosted(118);
		boosts.setMagicReal(99);    boosts.setMagicBoosted(99);

		ActionClockState clock = state.getActionClock();
		clock.setAttackDelayTicks(3);
		clock.setLastWeaponSpeedTicks(5);
		clock.setEatCooldownTicks(2);
		clock.setPotCooldownTicks(1);

		OpponentState opp = state.getOpponent();
		opp.setName("DragonSlayer99");
		opp.setEstimatedHp(43);
		opp.setMaxHp(99);
		opp.setOverheadPrayer(HeadIcon.SMITE);
		opp.setVengActive(true);
		OpponentStats stats = opp.getStats();
		stats.setAttack(99); stats.setStrength(99); stats.setDefence(99);
		stats.setRanged(99); stats.setMagic(99);    stats.setHitpoints(99);

		ProtectionState prot = state.getProtection();
		prot.setPjSafeTicksRemaining(12);
		prot.setCombatLogoutTicksRemaining(8);
		prot.setUnderAttackLockTicksRemaining(15);

		state.getContext().setWildernessLevel(20);
		state.getContext().setMultiCombat(true);

		state.getTimer1().start(60_000L);
		state.getTimer2().start(90_000L);

		state.beginSession("DragonSlayer99", currentTick - 5);
		PvpFightSession session = state.getCurrentSession();
		long now = System.currentTimeMillis();
		session.onOutgoingHit(CombatEvent.outgoing(42, now - 3600), currentTick - 3);
		session.onIncomingHit(CombatEvent.incoming(17, now - 3600), currentTick - 3);
		session.onOutgoingHit(CombatEvent.outgoing(28, now - 2400), currentTick - 2);
		session.onIncomingHit(
			new CombatEvent(CombatEventType.INCOMING_HIT, 35, 8, 0, PrayerEffect.SMITE, false, false, now - 2400),
			currentTick - 2);
		session.onOutgoingHit(
			new CombatEvent(CombatEventType.OUTGOING_HIT, 99, 0, 0, PrayerEffect.NONE, true, false, now - 1200),
			currentTick - 1);
	}
}
