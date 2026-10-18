package com.pvphud;

import com.pvphud.state.BoostState;
import com.pvphud.state.EffectState;
import com.pvphud.state.PoisonState;
import com.pvphud.state.SelfState;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.SpriteID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StatusProjector#project}.
 *
 * Uses {@code showAll=true} + {@code null} config so all effects emit regardless
 * of user settings, and the tests run without a RuneLite environment.
 */
public class StatusProjectorTest
{
	private static final int TICK = 100;

	// ── Helpers ──────────────────────────────────────────────────────────────

	private static List<StatusSlot> project(SelfState self, EffectState fx,
		PoisonState poison, BoostState boosts)
	{
		return StatusProjector.project(self, fx, poison, boosts, TICK, null, true);
	}

	private static List<String> slotIds(List<StatusSlot> slots)
	{
		return slots.stream().map(s -> s.slotId).collect(Collectors.toList());
	}

	private static StatusSlot find(List<StatusSlot> slots, String slotId)
	{
		return slots.stream().filter(s -> s.slotId.equals(slotId)).findFirst().orElse(null);
	}

	// ── Lifecycle: Freeze ↔ Refreeze Immunity ────────────────────────────────

	@Test
	public void frozen_showsFreezeSlot_notRefreezeImm()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 1, 20, SpriteID.SPELL_ICE_BARRAGE); // 19 ticks remaining

		List<StatusSlot> slots = project(self, new EffectState(), new PoisonState(), new BoostState());
		List<String> ids = slotIds(slots);

		assertTrue("freeze_barrage must appear when frozen", ids.contains("freeze_barrage"));
		assertFalse("freeze_imm must not appear while frozen", ids.contains("freeze_imm"));
	}

	@Test
	public void refreezeImm_showsRefreezeSlot_notFreeze()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 20, 20, SpriteID.SPELL_ICE_BARRAGE); // freeze expired
		self.thaw(TICK);                                              // grants immunity

		List<StatusSlot> slots = project(self, new EffectState(), new PoisonState(), new BoostState());
		List<String> ids = slotIds(slots);

		assertTrue("freeze_imm must appear after thaw", ids.contains("freeze_imm"));
		assertFalse("freeze_barrage must not appear when not frozen",
			ids.contains("freeze_barrage"));
	}

	@Test
	public void refreezeImm_labelUsesTicks_notSeconds()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 20, 20, SpriteID.SPELL_ICE_BARRAGE);
		self.thaw(TICK); // immunity = 5 ticks

		StatusSlot slot = find(
			project(self, new EffectState(), new PoisonState(), new BoostState()),
			"freeze_imm");
		assertNotNull(slot);
		assertEquals("REFRZ 5t", slot.textLabel);
	}

	@Test
	public void freeze_neverMoreThanOneLifecycleSlot()
	{
		// frozen: exactly one freeze slot
		SelfState frozen = new SelfState();
		frozen.applyFreeze(TICK - 1, 20, SpriteID.SPELL_ICE_BARRAGE);
		List<String> frozenIds = slotIds(
			project(frozen, new EffectState(), new PoisonState(), new BoostState()));
		long frozenCount = frozenIds.stream()
			.filter(id -> id.startsWith("freeze_")).count();
		assertEquals("exactly one freeze slot while frozen", 1, frozenCount);

		// immune: exactly one immunity slot
		SelfState immune = new SelfState();
		immune.applyFreeze(TICK - 20, 20, SpriteID.SPELL_ICE_BARRAGE);
		immune.thaw(TICK);
		List<String> immuneIds = slotIds(
			project(immune, new EffectState(), new PoisonState(), new BoostState()));
		long immuneCount = immuneIds.stream()
			.filter(id -> id.startsWith("freeze_")).count();
		assertEquals("exactly one freeze slot during immunity", 1, immuneCount);

		// normal: zero freeze slots
		SelfState normal = new SelfState();
		List<String> normalIds = slotIds(
			project(normal, new EffectState(), new PoisonState(), new BoostState()));
		long normalCount = normalIds.stream()
			.filter(id -> id.startsWith("freeze_")).count();
		assertEquals("zero freeze slots when not frozen or immune", 0, normalCount);
	}

	@Test
	public void freeze_iceBurst_usesBurstSlotId()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 1, 16, SpriteID.SPELL_ICE_BURST);

		List<String> ids = slotIds(project(self, new EffectState(), new PoisonState(), new BoostState()));
		assertTrue(ids.contains("freeze_burst"));
	}

	// ── Lifecycle: Teleblock ↔ TB Immunity ───────────────────────────────────

	@Test
	public void tbd_showsTbSlot_notTbImm()
	{
		SelfState self = new SelfState();
		self.setTeleBlockTicksRemaining(300);
		self.setTbImmunityActive(false);

		List<String> ids = slotIds(project(self, new EffectState(), new PoisonState(), new BoostState()));
		assertTrue("tb must appear when TB'd", ids.contains("tb"));
		assertFalse("tb_imm must not appear when TB'd", ids.contains("tb_imm"));
	}

	@Test
	public void tbImm_showsTbImmSlot_notTb()
	{
		SelfState self = new SelfState();
		self.setTeleBlockTicksRemaining(0);
		self.setTbImmunityActive(true);

		List<String> ids = slotIds(project(self, new EffectState(), new PoisonState(), new BoostState()));
		assertFalse("tb must not appear when only immune", ids.contains("tb"));
		assertTrue("tb_imm must appear when TB immune", ids.contains("tb_imm"));
	}

	// ── Lifecycle: Ward ↔ Ward CD ────────────────────────────────────────────

	@Test
	public void ward_active_showsWard_notWardCd()
	{
		EffectState fx = new EffectState();
		fx.applyWardOfArceuus(TICK - 10, 80);  // 70 ticks remaining
		fx.applyWardCooldown(TICK - 25);        // also has CD — ward takes precedence

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertTrue("ward must appear when active", ids.contains("ward"));
		assertFalse("ward_cd must not appear while ward is active", ids.contains("ward_cd"));
	}

	@Test
	public void ward_expired_showsWardCd()
	{
		EffectState fx = new EffectState();
		// ward expired; only CD is running
		fx.applyWardCooldown(TICK - 10); // 40 ticks remaining

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertFalse("ward must not appear when expired", ids.contains("ward"));
		assertTrue("ward_cd must appear when ward is on cooldown", ids.contains("ward_cd"));
	}

	// ── Lifecycle: Mark ↔ Mark CD ────────────────────────────────────────────

	@Test
	public void mark_active_showsMark_notMarkCd()
	{
		EffectState fx = new EffectState();
		fx.applyMarkOfDarkness(TICK - 1, 99); // ~98 ticks remaining
		fx.applyMarkCooldown(TICK - 2);        // CD would only show if mark expired

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertTrue("mark must appear when active", ids.contains("mark"));
		assertFalse("mark_cd must not appear while mark is active", ids.contains("mark_cd"));
	}

	@Test
	public void mark_expired_showsMarkCd()
	{
		EffectState fx = new EffectState();
		fx.applyMarkCooldown(TICK - 2); // 8 ticks remaining

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertFalse("mark must not appear when expired", ids.contains("mark"));
		assertTrue("mark_cd must appear when mark is on cooldown", ids.contains("mark_cd"));
	}

	// ── Compound: Corruption + next drain ────────────────────────────────────

	@Test
	public void corruption_withKnownInterval_includesSecondaryEntry()
	{
		EffectState fx = new EffectState();
		fx.setCorruptionTargetIterations(8);
		fx.setCorruptionCurrentIterations(2);
		fx.setCorruptionBaseDamage(3);
		fx.onCorruptionIterationChanged(TICK - 15);
		fx.onCorruptionIterationChanged(TICK);     // detects 15-tick interval

		List<StatusSlot> slots = project(new SelfState(), fx, new PoisonState(), new BoostState());
		List<String> ids = slotIds(slots);

		assertTrue("corrupt slot must appear", ids.contains("corrupt"));
		assertTrue("corrupt_next must appear when interval is known", ids.contains("corrupt_next"));

		StatusSlot next = find(slots, "corrupt_next");
		assertNotNull(next);
		assertTrue("corrupt_next must be secondary", next.isSecondary);
		assertEquals("corrupt_next must have same priority as corrupt",
			StatusPriority.CRITICAL, next.priority);
	}

	@Test
	public void corruption_withoutKnownInterval_noSecondaryEntry()
	{
		EffectState fx = new EffectState();
		fx.setCorruptionTargetIterations(5);
		fx.setCorruptionCurrentIterations(0);
		fx.setCorruptionBaseDamage(3);
		// only one iteration tick recorded — interval not yet detected

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertTrue("corrupt slot must appear", ids.contains("corrupt"));
		assertFalse("corrupt_next must not appear when interval is unknown",
			ids.contains("corrupt_next"));
	}

	// ── Compound: Menaphite + next proc ──────────────────────────────────────

	@Test
	public void menaphite_withNextProcTick_includesBothEntries()
	{
		EffectState fx = new EffectState();
		fx.getMenaphite().setTotalTicksRemaining(300);
		fx.getMenaphite().setNextProcTicks(20);

		List<StatusSlot> slots = project(new SelfState(), fx, new PoisonState(), new BoostState());
		List<String> ids = slotIds(slots);

		assertTrue("menaphite slot must appear", ids.contains("menaphite"));
		assertTrue("menaphite_next must appear when nextProcTicks > 0", ids.contains("menaphite_next"));

		StatusSlot next = find(slots, "menaphite_next");
		assertNotNull(next);
		assertTrue("menaphite_next must be secondary (hidden in icon-tray and 2-col)", next.isSecondary);
	}

	@Test
	public void menaphite_withNoNextProcTick_noSecondaryEntry()
	{
		EffectState fx = new EffectState();
		fx.getMenaphite().setTotalTicksRemaining(300);
		fx.getMenaphite().setNextProcTicks(0);

		List<String> ids = slotIds(project(new SelfState(), fx, new PoisonState(), new BoostState()));
		assertTrue("menaphite slot must appear", ids.contains("menaphite"));
		assertFalse("menaphite_next must not appear when nextProcTicks == 0",
			ids.contains("menaphite_next"));
	}

	// ── Compound: Burn + next hit ─────────────────────────────────────────────

	@Test
	public void burn_withNextHit_includesSecondaryEntry()
	{
		EffectState fx = new EffectState();
		fx.setBurnDamageTotal(12);
		fx.setBurnDamageNextHit(2);

		List<StatusSlot> slots = project(new SelfState(), fx, new PoisonState(), new BoostState());
		assertTrue(slotIds(slots).contains("burn"));
		assertTrue(slotIds(slots).contains("burn_next"));
		assertTrue("burn_next must be secondary", find(slots, "burn_next").isSecondary);
	}

	// ── Compound: Prayer Regen + next proc ───────────────────────────────────

	@Test
	public void prayerRegen_withNextProc_includesSecondaryEntry()
	{
		EffectState fx = new EffectState();
		fx.getPrayerRegen().setTotalTicksRemaining(36);
		fx.getPrayerRegen().setNextProcTicks(10);

		List<StatusSlot> slots = project(new SelfState(), fx, new PoisonState(), new BoostState());
		assertTrue(slotIds(slots).contains("prayer_regen"));
		assertTrue(slotIds(slots).contains("prayer_regen_next"));
		assertTrue("prayer_regen_next must be secondary", find(slots, "prayer_regen_next").isSecondary);
	}

	// ── Priority ordering ─────────────────────────────────────────────────────

	@Test
	public void priorityOrder_criticalBeforeTacticalBeforeSustainBeforeReadiness()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 1, 20, SpriteID.SPELL_ICE_BARRAGE);

		EffectState fx = new EffectState();
		fx.setBurnDamageTotal(10);                     // TACTICAL
		fx.setDivineSupercombatTicks(500);             // SUSTAIN
		fx.setImbuedHeartCooldownTicks(400);           // READINESS

		List<StatusSlot> slots = project(self, fx, new PoisonState(), new BoostState());

		StatusPriority last = null;
		for (StatusSlot s : slots)
		{
			if (last != null && s.priority.ordinal() < last.ordinal())
				fail("Priority out of order: " + last + " followed by " + s.priority
					+ " (slot: " + s.slotId + ")");
			last = s.priority;
		}
	}

	@Test
	public void emptyState_producesEmptyList()
	{
		List<StatusSlot> slots = project(new SelfState(), new EffectState(),
			new PoisonState(), new BoostState());
		assertTrue("empty state must yield no slots", slots.isEmpty());
	}

	// ── Venom > Poison precedence ─────────────────────────────────────────────

	@Test
	public void venomed_showsVenom_notPoison()
	{
		PoisonState poison = new PoisonState();
		poison.setVenomed(true);
		poison.setPoisoned(true); // would normally not happen, but defensive check

		List<String> ids = slotIds(project(new SelfState(), new EffectState(), poison, new BoostState()));
		assertTrue("venom must appear when venomed", ids.contains("venom"));
		assertFalse("poison must not appear when venomed", ids.contains("poison"));
	}

	@Test
	public void poisoned_notVenomed_showsPoison()
	{
		PoisonState poison = new PoisonState();
		poison.setPoisoned(true);

		List<String> ids = slotIds(project(new SelfState(), new EffectState(), poison, new BoostState()));
		assertFalse("venom must not appear when only poisoned", ids.contains("venom"));
		assertTrue("poison must appear when poisoned", ids.contains("poison"));
	}

	// ── Anti-venom > Anti-poison precedence ──────────────────────────────────

	@Test
	public void antiVenom_prevailsOverAntiPoison()
	{
		PoisonState poison = new PoisonState();
		poison.setAntiVenomActive(true);
		poison.setPoisonVarpValue(-58);
		poison.setNextPoisonTick(TICK + 30);

		List<String> ids = slotIds(project(new SelfState(), new EffectState(), poison, new BoostState()));
		assertTrue(ids.contains("anti_venom"));
		assertFalse(ids.contains("anti_poison"));
	}

	// ── Priority assignments ──────────────────────────────────────────────────

	@Test
	public void freeze_isCritical()
	{
		SelfState self = new SelfState();
		self.applyFreeze(TICK - 1, 20, SpriteID.SPELL_ICE_BARRAGE);
		StatusSlot s = find(project(self, new EffectState(), new PoisonState(), new BoostState()),
			"freeze_barrage");
		assertNotNull(s);
		assertEquals(StatusPriority.CRITICAL, s.priority);
	}

	@Test
	public void venom_isTactical()
	{
		PoisonState p = new PoisonState();
		p.setVenomed(true);
		StatusSlot s = find(project(new SelfState(), new EffectState(), p, new BoostState()), "venom");
		assertNotNull(s);
		assertEquals(StatusPriority.TACTICAL, s.priority);
	}

	@Test
	public void divineSupercombat_isSustain()
	{
		EffectState fx = new EffectState();
		fx.setDivineSupercombatTicks(500);
		StatusSlot s = find(project(new SelfState(), fx, new PoisonState(), new BoostState()), "dsc");
		assertNotNull(s);
		assertEquals(StatusPriority.SUSTAIN, s.priority);
	}

	@Test
	public void imbuedHeartCd_isReadiness()
	{
		EffectState fx = new EffectState();
		fx.setImbuedHeartCooldownTicks(400);
		StatusSlot s = find(project(new SelfState(), fx, new PoisonState(), new BoostState()), "ih_cd");
		assertNotNull(s);
		assertEquals(StatusPriority.READINESS, s.priority);
	}

	@Test
	public void sotd_isTactical()
	{
		EffectState fx = new EffectState();
		fx.applyStaffProt(TICK - 10); // ~90 ticks remaining
		StatusSlot s = find(project(new SelfState(), fx, new PoisonState(), new BoostState()), "sotd");
		assertNotNull(s);
		assertEquals(StatusPriority.TACTICAL, s.priority);
	}
}
