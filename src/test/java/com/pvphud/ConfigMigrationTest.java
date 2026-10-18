package com.pvphud;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the one-shot v0 → v1 config migration logic.
 *
 * The real migration lives in PvpHudPlugin.migrateOldConfig() and runs against a
 * live ConfigManager at startUp(). This test uses a Map-backed fake to exercise
 * the exact same migration rules without a RuneLite container.
 */
public class ConfigMigrationTest
{
	/** Minimal ConfigManager-like fake backed by a Map. */
	static class FakeConfig
	{
		private final Map<String, String> store = new HashMap<>();

		String get(String key)                   { return store.get(key); }
		void   set(String key, String val)        { store.put(key, val); }
		void   setIfAbsent(String oldKey, String newKey)
		{
			String oldVal = get(oldKey);
			String newVal = get(newKey);
			if (oldVal != null && newVal == null)
				set(newKey, oldVal);
		}

		/** Simulates the full migration. Returns whether migration ran. */
		boolean migrate()
		{
			if ("1".equals(get("configVersion"))) return false;

			String layoutRaw = get("hudLayout");
			if ("CHAT_LOCKED".equals(layoutRaw))
			{
				set("hudLayout", "HORIZONTAL_FLOAT");
				set("hf_chatLocked", "true");
				setIfAbsent("buffStyle",         "hf_buffStyle");
				setIfAbsent("backgroundOpacity", "hf_backgroundOpacity");
				setIfAbsent("hpBarStyle",        "hf_hpBarStyle");
				setIfAbsent("prayerBarStyle",    "hf_prayerBarStyle");
				setIfAbsent("runBarStyle",       "hf_runBarStyle");
				setIfAbsent("specBarStyle",      "hf_specBarStyle");
			}
			set("configVersion", "1");
			return true;
		}
	}

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

	@Test
	public void nonChatLocked_noLayoutChange()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "HORIZONTAL_FLOAT");
		cfg.migrate();
		assertEquals("HORIZONTAL_FLOAT", cfg.get("hudLayout"));
		assertNull(cfg.get("hf_chatLocked"));
	}

	@Test
	public void freshInstall_noHudLayout_noMigration()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.migrate();
		assertNull(cfg.get("hudLayout"));
		assertEquals("1", cfg.get("configVersion"));
	}

	@Test
	public void idempotent_secondMigrationIsNoOp()
	{
		FakeConfig cfg = new FakeConfig();
		cfg.set("hudLayout", "CHAT_LOCKED");
		cfg.migrate();
		assertEquals("HORIZONTAL_FLOAT", cfg.get("hudLayout"));

		cfg.set("hudLayout", "VERTICAL_FLOAT");
		cfg.migrate();
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
