package com.pvphud;

import com.pvphud.state.BoostState;
import com.pvphud.state.EffectState;
import com.pvphud.state.PoisonState;
import com.pvphud.state.SelfState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.SpriteID;

/**
 * Projects PvP state into an ordered list of {@link StatusSlot}s for HUD rendering.
 *
 * <p>Lifecycle slots: Freeze↔Refreeze Immunity, TB↔TB Immunity, Ward↔Ward CD,
 * Mark↔Mark CD each occupy a single logical slot — only one state is ever visible.
 *
 * <p>Compound statuses: Corruption, Burn, Prayer Regen, and Menaphite emit a
 * primary slot followed by an {@code isSecondary=true} sub-entry for the next
 * proc/drain/hit countdown.
 *
 * <p>Icons are not resolved here; callers use the {@code slotId} field to look
 * up icons in {@link PvpHudOverlay#getIconFor}.
 */
public final class StatusProjector
{
	// ── Colour palette ────────────────────────────────────────────────────────
	static final Color LIGHT_BLUE  = new Color(100, 185, 255);
	static final Color ORANGE      = new Color(255, 145,   0);
	static final Color RED         = new Color(220,  60,  60);
	static final Color TOXIC_GREEN = new Color(110, 230,  60);
	static final Color YELLOW      = new Color(255, 203,   5);
	static final Color GREEN       = new Color( 10, 210,  80);

	private StatusProjector() {}

	/**
	 * Builds the semantic slot list for one HUD frame.
	 *
	 * @param config   presentation config; may be {@code null} when {@code showAll=true}
	 * @param showAll  true in debug mode — bypasses all config visibility guards
	 */
	public static List<StatusSlot> project(
		SelfState self, EffectState fx, PoisonState poison, BoostState boosts,
		int tick, PvpHudConfig config, boolean showAll)
	{
		List<StatusSlot> out = new ArrayList<>(24);
		Color freezeHighlight = (config != null) ? config.barrageHighlightColor() : null;
		Color tbHighlight     = (config != null) ? config.tbHighlightColor()      : null;

		// ── CRITICAL ──────────────────────────────────────────────────────────
		// Freeze ↔ Refreeze Immunity: exactly one can show at a time
		int freeze = self.getFreezeTicksRemaining(tick);
		if (freeze > 0 && (showAll || config.showFreezeTimer()))
		{
			String t = ticksToSecs(freeze);
			out.add(new StatusSlot(freezeSlotId(self.getFreezeSpriteId()),
				StatusPriority.CRITICAL, t, "ICE " + t, LIGHT_BLUE, freezeHighlight, false));
		}
		else
		{
			int refreezeImm = self.getRefreezeImmunityTicks(tick);
			if (refreezeImm > 0 && (showAll || config.showFreezeTimer()))
			{
				String t = ticksToSecs(refreezeImm);
				out.add(new StatusSlot("freeze_imm", StatusPriority.CRITICAL, t, "REFRZ " + t,
					LIGHT_BLUE, false));
			}
		}

		// Teleblock ↔ TB Immunity: exactly one can show at a time
		int tb = self.getTeleBlockTicksRemaining();
		if (tb > 0)
		{
			String t = ticksToMSS(tb);
			out.add(new StatusSlot("tb", StatusPriority.CRITICAL, t, "TB " + t,
				ORANGE, tbHighlight, false));
		}
		else if (self.isTbImmunityActive())
		{
			out.add(new StatusSlot("tb_imm", StatusPriority.CRITICAL, "IMM", "TB-IMM",
				ORANGE, false));
		}

		// Prayer lock
		int prayerLockTicks = self.getPrayerLockTicksRemaining(tick);
		if (prayerLockTicks > 0)
		{
			out.add(new StatusSlot("plock", StatusPriority.CRITICAL,
				String.valueOf(prayerLockTicks), "PLOCK " + prayerLockTicks, RED, false));
		}

		// Corruption — parent + secondary (next drain tick)
		if (fx.isCorruptionActive())
		{
			int iters = fx.getCorruptionIterationsRemaining();
			int total = fx.getCorruptionTotalTicks(tick);
			String label     = total > 0 ? ticksToMSS(total) : iters + "x";
			String textLabel = "CORRUPT " + label;
			out.add(new StatusSlot("corrupt", StatusPriority.CRITICAL, label, textLabel, RED, false));

			int nextDrain = fx.getCorruptionNextDrainTicks(tick);
			if (nextDrain > 0)
			{
				String sec = "-" + fx.getCorruptionBaseDamage() + " " + nextDrain + "t";
				out.add(new StatusSlot("corrupt_next", StatusPriority.CRITICAL, sec,
					"NXDRAIN " + sec, RED, true));
			}
		}

		// ── TACTICAL ─────────────────────────────────────────────────────────
		// Venom takes priority over poison
		if (poison.isVenomed())
			out.add(new StatusSlot("venom", StatusPriority.TACTICAL, "", "VENOM", TOXIC_GREEN, false));
		else if (poison.isPoisoned())
			out.add(new StatusSlot("poison", StatusPriority.TACTICAL, "", "POISON", TOXIC_GREEN, false));

		// Burn — parent + secondary (next hit)
		if (fx.getBurnDamageTotal() > 0)
		{
			out.add(new StatusSlot("burn", StatusPriority.TACTICAL,
				String.valueOf(fx.getBurnDamageTotal()),
				"BURN " + fx.getBurnDamageTotal(), RED, false));
			if (fx.getBurnDamageNextHit() > 0)
			{
				out.add(new StatusSlot("burn_next", StatusPriority.TACTICAL,
					"BURN+" + fx.getBurnDamageNextHit(),
					"BURN NXT " + fx.getBurnDamageNextHit(), RED, true));
			}
		}

		// Staff of the Dead melee protection
		int sotdTicks = fx.getStaffProtTicks(tick);
		if (sotdTicks > 0)
			addTimed(out, "sotd", StatusPriority.TACTICAL, "SOTD", sotdTicks, false);

		// Ward of Arceuus ↔ Ward CD: lifecycle slot
		int wardTicks = fx.getWardOfArceuusTicks(tick);
		if (wardTicks > 0)
			addTimed(out, "ward", StatusPriority.TACTICAL, "WARD", wardTicks, false);
		else
		{
			int wardCd = fx.getWardCooldownTicks(tick);
			if (wardCd > 0)
				addTimed(out, "ward_cd", StatusPriority.TACTICAL, "WARD-CD", wardCd, false);
		}

		// Mark of Darkness ↔ Mark CD: lifecycle slot
		int markTicks = fx.getMarkOfDarknessTicks(tick);
		if (markTicks > 0)
			addTimed(out, "mark", StatusPriority.TACTICAL, "MARK", markTicks, false);
		else
		{
			int markCd = fx.getMarkCooldownTicks(tick);
			if (markCd > 0)
				addTimed(out, "mark_cd", StatusPriority.TACTICAL, "MARK-CD", markCd, false);
		}

		// ── SUSTAIN ───────────────────────────────────────────────────────────
		// Anti-venom takes priority over anti-poison
		if (poison.isAntiVenomActive())
			addTimed(out, "anti_venom", StatusPriority.SUSTAIN, "ANTI-V",
				poison.getImmunityTicksRemaining(tick), false);
		else if (poison.isAntiPoisonActive())
			addTimed(out, "anti_poison", StatusPriority.SUSTAIN, "ANTI-P",
				poison.getImmunityTicksRemaining(tick), false);

		// Divine potions
		if (showAll || config.showDivineTimers())
		{
			addTimed(out, "dsc", StatusPriority.SUSTAIN, "DSC", fx.getDivineSupercombatTicks(), false);
			addTimed(out, "drg", StatusPriority.SUSTAIN, "DRG", fx.getDivineRangingTicks(),     false);
			addTimed(out, "dmg", StatusPriority.SUSTAIN, "DMG", fx.getDivineMagicTicks(),       false);
			addTimed(out, "bas", StatusPriority.SUSTAIN, "BAS", fx.getDivineBastionTicks(),     false);
			addTimed(out, "btm", StatusPriority.SUSTAIN, "BTM", fx.getDivineBattlemageTicks(),  false);
			addTimed(out, "dsa", StatusPriority.SUSTAIN, "DSA", fx.getDivineSuperAttackTicks(),   false);
			addTimed(out, "dss", StatusPriority.SUSTAIN, "DSS", fx.getDivineSuperStrengthTicks(), false);
			addTimed(out, "dsd", StatusPriority.SUSTAIN, "DSD", fx.getDivineSuperDefenceTicks(),  false);

			// Menaphite Remedy — parent + secondary (next proc)
			if (fx.getMenaphite().isActive())
			{
				addTimed(out, "menaphite", StatusPriority.SUSTAIN, "MEN",
					fx.getMenaphite().getTotalTicksRemaining(), false);
				if (fx.getMenaphite().getNextProcTicks() > 0)
					addTimed(out, "menaphite_next", StatusPriority.SUSTAIN, "NXT",
						fx.getMenaphite().getNextProcTicks(), false);
			}
		}

		// Prayer Regeneration potion — parent + secondary (next proc)
		if (fx.getPrayerRegen().isActive())
		{
			addTimed(out, "prayer_regen", StatusPriority.SUSTAIN, "PRYR",
				fx.getPrayerRegen().getTotalTicksRemaining(), false);
			if (fx.getPrayerRegen().getNextProcTicks() > 0)
				addTimed(out, "prayer_regen_next", StatusPriority.SUSTAIN, "P-NXT",
					fx.getPrayerRegen().getNextProcTicks(), true);
		}

		// Stamina potion
		if (fx.getStaminaEffectTicks() > 0)
			addTimed(out, "stamina", StatusPriority.SUSTAIN, "STAM", fx.getStaminaEffectTicks(), false);

		// Antifire
		int afTicks  = fx.getAntifireTicks(tick);
		int safTicks = fx.getSuperAntifireTicks(tick);
		if (afTicks  > 0) addTimed(out, "af",  StatusPriority.SUSTAIN, "AF",  afTicks,  false);
		if (safTicks > 0) addTimed(out, "saf", StatusPriority.SUSTAIN, "SAF", safTicks, false);

		// Boost decay / debuff restore cycle timers
		int drainTicks = self.getBoostDecay().getTicksRemaining();
		if (drainTicks > 0 && self.getBoostDecay().isCalibrated() && boosts.hasAnyBoost())
			out.add(new StatusSlot("drain", StatusPriority.SUSTAIN,
				"drain " + drainTicks + "t", null, YELLOW, false));

		int rstTicks = self.getDebuffRestore().getTicksRemaining();
		if (rstTicks > 0 && self.getDebuffRestore().isCalibrated() && boosts.hasAnyDebuff())
			out.add(new StatusSlot("restore", StatusPriority.SUSTAIN,
				"rst " + rstTicks + "t", null, YELLOW, false));

		// ── READINESS ─────────────────────────────────────────────────────────
		if (fx.getImbuedHeartCooldownTicks() > 0)
			addTimed(out, "ih_cd", StatusPriority.READINESS, "IH-CD",
				fx.getImbuedHeartCooldownTicks(), false);

		int surgeTicks = fx.getSurgePotionTicks(tick);
		if (surgeTicks > 0)
			addTimed(out, "surge", StatusPriority.READINESS, "SURGE", surgeTicks, false);

		return out;
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private static void addTimed(List<StatusSlot> out, String slotId, StatusPriority priority,
		String prefix, int ticks, boolean isSecondary)
	{
		if (ticks <= 0) return;
		String t         = ticksToMSS(ticks);
		String textLabel = prefix + " " + t;
		out.add(new StatusSlot(slotId, priority, t, textLabel, timerColor(ticks), isSecondary));
	}

	private static String freezeSlotId(int spriteId)
	{
		switch (spriteId)
		{
			case SpriteID.SPELL_ICE_RUSH:    return "freeze_rush";
			case SpriteID.SPELL_ICE_BURST:   return "freeze_burst";
			case SpriteID.SPELL_ICE_BLITZ:   return "freeze_blitz";
			case SpriteID.SPELL_ICE_BARRAGE: return "freeze_barrage";
			default:                         return "freeze_barrage";
		}
	}

	// ── Format helpers (package-private for tests) ────────────────────────────

	static String ticksToSecs(int ticks)
	{
		return (int) Math.round(ticks * 0.6) + "s";
	}

	static String ticksToMSS(int ticks)
	{
		int s = ticks * 600 / 1000;
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}

	static Color timerColor(int ticks)
	{
		int s = ticks * 600 / 1000;
		return s > 60 ? GREEN : s > 30 ? YELLOW : RED;
	}
}
