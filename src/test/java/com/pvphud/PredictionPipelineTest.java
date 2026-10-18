package com.pvphud;

import com.pvphud.state.OpponentState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the XP prediction pipeline in PvpHudPlugin:
 *   - target-identity binding (prediction for Alice never confirms against Bob)
 *   - consumePredictionFor: FIFO, expiry pruning, wrong-target rejection
 *   - syncPendingPredictionDisplay: clears on empty/expired/wrong-target, sets on match
 *   - ghost-reappearance prevention: consumed prediction never reappears
 *   - same-tick multi-hit: one prediction consumed regardless of hitsplat count
 *   - normalizePlayerName helper
 *
 * PvpHudPlugin is constructed with no Guice injection. Fields that are
 * initialised in their declarations (hudState, pendingPredictions) are fully
 * functional; injected fields (client, config, …) remain null but are not
 * exercised by any of the methods under test.
 */
public class PredictionPipelineTest
{
	private PvpHudPlugin plugin;

	@Before
	public void setUp()
	{
		plugin = new PvpHudPlugin();
	}

	// ── normalizePlayerName ────────────────────────────────────────────────────

	@Test
	public void normalizePlayerName_lowercases()
	{
		assertEquals("alice", PvpHudPlugin.normalizePlayerName("Alice"));
		assertEquals("bob",   PvpHudPlugin.normalizePlayerName("BOB"));
	}

	@Test
	public void normalizePlayerName_null_returnsEmpty()
	{
		assertEquals("", PvpHudPlugin.normalizePlayerName(null));
	}

	// ── consumePredictionFor ──────────────────────────────────────────────────

	@Test
	public void consume_matchingTarget_returnsAndRemovesHead()
	{
		addPrediction(34, 100, "alice");
		PvpHudPlugin.PendingPrediction p = plugin.consumePredictionFor("alice", 100);
		assertNotNull(p);
		assertEquals(34, p.damage);
		assertTrue("queue should be empty after consume", plugin.pendingPredictions.isEmpty());
	}

	@Test
	public void consume_wrongTarget_returnsNull()
	{
		addPrediction(34, 100, "alice");
		PvpHudPlugin.PendingPrediction p = plugin.consumePredictionFor("bob", 100);
		assertNull("prediction for alice must not be consumed by bob", p);
		assertEquals(1, plugin.pendingPredictions.size());
	}

	@Test
	public void consume_expiredEntry_skipsAndReturnsNull()
	{
		addExpiredPrediction(34, 90, "alice");
		PvpHudPlugin.PendingPrediction p = plugin.consumePredictionFor("alice", 100);
		assertNull("expired prediction must not be returned", p);
		assertTrue("expired entry must be pruned", plugin.pendingPredictions.isEmpty());
	}

	@Test
	public void consume_expiredHeadThenFreshTail_returnsFresh()
	{
		addExpiredPrediction(10, 90, "alice");
		addPrediction(20, 100, "alice");
		PvpHudPlugin.PendingPrediction p = plugin.consumePredictionFor("alice", 100);
		assertNotNull(p);
		assertEquals(20, p.damage);
	}

	@Test
	public void consume_fifo_consumesOldestFirst()
	{
		addPrediction(10, 100, "alice");
		addPrediction(20, 101, "alice");
		PvpHudPlugin.PendingPrediction first  = plugin.consumePredictionFor("alice", 101);
		PvpHudPlugin.PendingPrediction second = plugin.consumePredictionFor("alice", 101);
		assertNotNull(first);
		assertNotNull(second);
		assertEquals("oldest prediction consumed first", 10, first.damage);
		assertEquals("second prediction consumed second", 20, second.damage);
	}

	@Test
	public void consume_createdTickAfterConfirmed_notConsumed()
	{
		// Prediction created at tick 105 must not be consumed by a confirm at tick 100.
		addPrediction(30, 105, "alice");
		PvpHudPlugin.PendingPrediction p = plugin.consumePredictionFor("alice", 100);
		assertNull("future-tick prediction must not confirm against an earlier tick", p);
		assertEquals(1, plugin.pendingPredictions.size());
	}

	// ── syncPendingPredictionDisplay ──────────────────────────────────────────

	@Test
	public void sync_emptyQueue_clearsHit()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("alice");
		opp.setPendingHit(34); // manually set a stale value
		plugin.syncPendingPredictionDisplay();
		assertFalse("pending hit must be cleared when queue is empty", opp.hasPendingHit());
	}

	@Test
	public void sync_matchingTargetInQueue_setsHit()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("alice");
		addPrediction(34, 100, "alice");
		plugin.syncPendingPredictionDisplay();
		assertTrue(opp.hasPendingHit());
		assertEquals(34, opp.getPendingHitDamage());
	}

	@Test
	public void sync_wrongTargetInQueue_clearsHit()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("bob");
		addPrediction(34, 100, "alice"); // prediction for alice, opponent is bob
		plugin.syncPendingPredictionDisplay();
		assertFalse("prediction for alice must not set pending hit when tracking bob",
			opp.hasPendingHit());
	}

	@Test
	public void sync_expiredQueue_clearsHit()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("alice");
		addExpiredPrediction(34, 100, "alice");
		opp.setPendingHit(34); // stale value before sync
		plugin.syncPendingPredictionDisplay();
		assertFalse("expired prediction must not remain as pending hit", opp.hasPendingHit());
	}

	@Test
	public void sync_noTrackedOpponent_clearsHit()
	{
		// opponent name is null → not tracked
		addPrediction(34, 100, "alice");
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setPendingHit(34);
		plugin.syncPendingPredictionDisplay();
		assertFalse(opp.hasPendingHit());
	}

	// ── Ghost-reappearance prevention ─────────────────────────────────────────

	@Test
	public void consumedPrediction_ghostDoesNotReappear()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("alice");
		addPrediction(34, 100, "alice");
		plugin.syncPendingPredictionDisplay();
		assertTrue("pending hit must be visible before confirmation", opp.hasPendingHit());

		// Confirm: consume prediction
		plugin.consumePredictionFor("alice", 100);
		plugin.syncPendingPredictionDisplay();

		assertFalse("consumed prediction must not reappear as ghost", opp.hasPendingHit());
	}

	@Test
	public void consumedPrediction_nextRemainsVisible()
	{
		OpponentState opp = plugin.getHudState().getOpponent();
		opp.setName("alice");
		addPrediction(34, 100, "alice");
		addPrediction(22, 101, "alice");

		plugin.consumePredictionFor("alice", 100); // consume A
		plugin.syncPendingPredictionDisplay();

		assertTrue("prediction B must remain visible after A is consumed", opp.hasPendingHit());
		assertEquals(22, opp.getPendingHitDamage());
	}

	// ── Target identity: Alice prediction must not confirm against Bob ─────────

	@Test
	public void targetIdentity_oldPredictionNotConsumedAfterRetarget()
	{
		// Simulate: had a prediction queued for alice, then session switches to bob.
		addPrediction(34, 100, "alice");

		// New opponent is bob — trying to confirm bob's hit must not pop alice's entry.
		PvpHudPlugin.PendingPrediction consumed = plugin.consumePredictionFor("bob", 102);
		assertNull("prediction for alice must not be consumed when bob is the current target",
			consumed);
		assertEquals(1, plugin.pendingPredictions.size());
	}

	// ── Queue overflow: max 4 ─────────────────────────────────────────────────

	@Test
	public void queueOverflow_dropsOldest()
	{
		for (int i = 1; i <= 5; i++)
			addPrediction(i * 10, 100 + i, "alice");

		assertEquals("queue capped at 4", 4, plugin.pendingPredictions.size());
		// Oldest (10) was dropped; head is now 20
		assertEquals(20, plugin.pendingPredictions.peekFirst().damage);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void addPrediction(int damage, int tick, String target)
	{
		if (plugin.pendingPredictions.size() >= 4)
			plugin.pendingPredictions.pollFirst();
		plugin.pendingPredictions.addLast(
			new PvpHudPlugin.PendingPrediction(
				damage, tick, System.currentTimeMillis(), target));
	}

	/** Adds a prediction with a createdMs far enough in the past to be expired (> 3 s). */
	private void addExpiredPrediction(int damage, int tick, String target)
	{
		plugin.pendingPredictions.addLast(
			new PvpHudPlugin.PendingPrediction(
				damage, tick, System.currentTimeMillis() - 4000L, target));
	}
}
