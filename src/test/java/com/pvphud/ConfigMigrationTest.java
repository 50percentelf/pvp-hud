package com.pvphud;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the one-shot v0 → v1 config migration logic.
 *
 * The real migration lives in {@code PvpHudPlugin.migrateOldConfig()} and runs
 * against a live {@code ConfigManager} at startUp(). This test uses a
 * Map-backed fake that mirrors the exact production rules.
 */
public class ConfigMigrationTest
{
	/** Minimal ConfigManager-like fake backed by a Map. */
	static class FakeConfig
	{
		private final Map<String, String> store = new HashMap<>();

		String get(String key)                   { return store.get(key); }
		void   set(String key, String val)        { store.put(key, val); }
		void   copyIfAbsent(String oldKey, String newKey)
		{
			String oldVal = get(oldKey);
			String newVal = get(newKey);
			if (oldVal != null && newVal == null)
				set(newKey, oldVal);
		}

		/** Mirrors {@code PvpHudPlugin.migrateOldConfig} exactly. */
		boolean migrate()
		{
			if ("1".equals(get("configVersion"))) return false;

			String layoutRaw = get("hudLayout");
			if ("CHAT_LOCKED".equals(layoutRaw))
			{
				set("hudLayout", "HORIZONTAL_FLOAT");
				set("hf_chatLocked", "true");
			}

			// Unconditionally copy old style keys so users who kept the old
			// CHAT_LOCKED default (no saved hudLayout) still get their
			// customisations carried into the hf_* namespace.
			copyIfAbsent("buffStyle",         "hf_buffStyle");
			copyIfAbsent("backgroundOpacity", "hf_backgroundOpacity");
			copyIfAbsent("hpBarStyle",        "hf_hpBarStyle");
			copyIfAbsent("prayerBarStyle",    "hf_prayerBarStyle");
			copyIfAbsent("runBarStyle",       "hf_runBarStyle");
			copyIfAbsent("specBarStyle",      "hf_specBarStyle");

			set("configVersion", "1");
			return true;
		}
	}

	// ── Explicit CHAT_LOCKED path ─────────────────────────────────────────────

	@Test
	public void chatLocked_migratesLayout()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		cfg.migrate();
		assertEquals("HORIZONTAL_FLOAT", cfg.get("hudLayout"));
		assertEquals("true", cfg.get("hf_chatLocked"));
	}

	@Test
	public void chatLocked_copiesBuffStyle_whenNotSet()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		cfg.set("buffStyle", "TEXT");
		cfg.migrate();
		assertEquals("TEXT", cfg.get("hf_buffStyle"));
	}

	@Test
	public void chatLocked_doesNotOverwrite_existingHfBuffStyle()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		cfg.set("buffStyle",    "TEXT");
		cfg.set("hf_buffStyle", "ICON_TRAY");
		cfg.migrate();
		assertEquals("ICON_TRAY", cfg.get("hf_buffStyle"));
	}

	// ── Unsaved default path (hudLayout key never written) ────────────────────

	@Test
	public void freshInstall_noHudLayout_configVersionSet_noForcedChatLocked()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.migrate();
		assertNull("no hudLayout written on fresh install", cfg.get("hudLayout"));
		assertEquals("1", cfg.get("configVersion"));
		assertNull("hf_chatLocked not forced — default of true handles it", cfg.get("hf_chatLocked"));
	}

	@Test
	public void oldDefault_withBuffStyleSet_copiedToHf()
	{
		// Classic regression: user ran old plugin, kept CHAT_LOCKED default,
		// changed Status Display Style. hudLayout key is unset but buffStyle is.
		FakeConfig cfg = new FakeConfig();
		cfg.set("buffStyle", "TEXT");
		cfg.migrate();
		assertEquals("TEXT", cfg.get("hf_buffStyle"));
		assertNull("hudLayout not forced for unsaved default", cfg.get("hudLayout"));
	}

	@Test
	public void oldDefault_doesNotOverwriteExistingHfBuffStyle()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("buffStyle",    "TEXT");
		cfg.set("hf_buffStyle", "ICON_TRAY");
		cfg.migrate();
		assertEquals("ICON_TRAY", cfg.get("hf_buffStyle"));
	}

	// ── Non-overwriting layouts ───────────────────────────────────────────────

	@Test
	public void explicitHorizontalFloat_noLayoutChange()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "HORIZONTAL_FLOAT");
		cfg.migrate();
		assertEquals("HORIZONTAL_FLOAT", cfg.get("hudLayout"));
		assertNull("hf_chatLocked not forced for non-CHAT_LOCKED layouts",
			cfg.get("hf_chatLocked"));
	}

	@Test
	public void explicitVerticalFloat_noLayoutChange()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "VERTICAL_FLOAT");
		cfg.migrate();
		assertEquals("VERTICAL_FLOAT", cfg.get("hudLayout"));
		assertNull(cfg.get("hf_chatLocked"));
	}

	@Test
	public void explicitInventoryHug_noLayoutChange()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "INVENTORY_HUG");
		cfg.migrate();
		assertEquals("INVENTORY_HUG", cfg.get("hudLayout"));
		assertNull(cfg.get("hf_chatLocked"));
	}

	// ── Idempotency ───────────────────────────────────────────────────────────

	@Test
	public void migrationIsIdempotent()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		assertTrue(cfg.migrate());
		assertEquals("HORIZONTAL_FLOAT", cfg.get("hudLayout"));

		// A second migration must not change the layout again.
		cfg.set("hudLayout", "VERTICAL_FLOAT");
		assertFalse("second migrate call must be a no-op", cfg.migrate());
		assertEquals("VERTICAL_FLOAT", cfg.get("hudLayout"));
	}

	@Test
	public void versionAlreadySet_migrationSkipped()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		cfg.set("configVersion", "1");
		cfg.migrate();
		assertEquals("CHAT_LOCKED", cfg.get("hudLayout"));
	}
}
