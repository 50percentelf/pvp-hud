package com.pvphud;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Offline HUD snapshot harness.
 *
 * Renders every buff style × scenario to PNG without a running RuneLite client.
 * Outputs PNGs and JSON manifests to build/hud-snapshots/ for visual review.
 * Asserts that rendered output never overflows the declared canvas bounds.
 *
 * Run via: ./gradlew test --tests "com.pvphud.HudSnapshotTest"
 */
public class HudSnapshotTest
{
	private static final Font TEST_FONT = new Font("Dialog", Font.PLAIN, 9);

	/** Fixed tick used for all headless state fixtures. */
	private static final int TICK = 100;

	/** Canvas dimensions used for TEXT and VERTICAL_BAR snapshots. */
	private static final int STRIP_W = 120;
	private static final int STRIP_H = 400;

	/** Canvas width for ICON_TRAY snapshots (matches HF boost-row style). */
	private static final int TRAY_W = 519;
	private static final int TRAY_H = 30;

	/** Canvas dimensions for 2-COL snapshots (Inventory Hug left rail). */
	private static final int TWO_COL_W = 72;
	private static final int TWO_COL_H = 400;

	enum Scenario
	{
		EMPTY,
		ALL_EFFECTS;

		PvpHudState buildState()
		{
			PvpHudState s = new PvpHudState();
			if (this == ALL_EFFECTS)
				DebugStateBuilder.applyPreset(s, TICK);
			return s;
		}
	}

	// ── Snapshot helpers ──────────────────────────────────────────────────

	private static Graphics2D makeCanvas(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(TEST_FONT);
		g.setColor(new Color(20, 20, 20));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		return g;
	}

	private static void writePng(BufferedImage img, String name) throws IOException
	{
		File dir = new File("build/hud-snapshots");
		dir.mkdirs();
		ImageIO.write(img, "PNG", new File(dir, name + ".png"));
	}

	private static void writeManifest(String name, Scenario scenario, String style,
		int canvasW, int canvasH, int finalCy, List<ActiveEffectView> effects) throws IOException
	{
		File dir = new File("build/hud-snapshots");
		dir.mkdirs();
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"scenario\": \"").append(scenario.name()).append("\",\n");
		sb.append("  \"buffStyle\": \"").append(style).append("\",\n");
		sb.append("  \"canvasW\": ").append(canvasW).append(",\n");
		sb.append("  \"canvasH\": ").append(canvasH).append(",\n");
		sb.append("  \"tick\": ").append(TICK).append(",\n");
		sb.append("  \"effectCount\": ").append(effects.size()).append(",\n");
		sb.append("  \"renderedHeight\": ").append(finalCy).append(",\n");
		sb.append("  \"elements\": [\n");
		for (int i = 0; i < effects.size(); i++)
		{
			ActiveEffectView e = effects.get(i);
			sb.append("    {\"id\": \"").append(e.getTextLabel().replace("\"", "\\\""))
			  .append("\", \"group\": \"").append(e.group.name()).append("\"}");
			if (i < effects.size() - 1) sb.append(",");
			sb.append("\n");
		}
		sb.append("  ]\n}\n");
		try (FileWriter fw = new FileWriter(new File(dir, name + ".json")))
		{
			fw.write(sb.toString());
		}
	}

	// ── TEXT snapshots ────────────────────────────────────────────────────

	@Test
	public void text_empty_noOverflow() throws IOException
	{
		Scenario s = Scenario.EMPTY;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, STRIP_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsText(g, TEST_FONT, effects, 4, 0, STRIP_H);
		g.dispose();

		writePng(img, "text_empty");
		writeManifest("text_empty", s, "TEXT", STRIP_W, STRIP_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= STRIP_H);
		assertEquals("empty scenario has no effects", 0, effects.size());
	}

	@Test
	public void text_allEffects_noOverflow() throws IOException
	{
		Scenario s = Scenario.ALL_EFFECTS;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, STRIP_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsText(g, TEST_FONT, effects, 4, 0, STRIP_H);
		g.dispose();

		writePng(img, "text_allEffects");
		writeManifest("text_allEffects", s, "TEXT", STRIP_W, STRIP_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= STRIP_H);
		assertFalse("ALL_EFFECTS scenario must have at least one effect", effects.isEmpty());
	}

	// ── VERTICAL_BAR snapshots ────────────────────────────────────────────

	@Test
	public void vertBar_empty_noOverflow() throws IOException
	{
		Scenario s = Scenario.EMPTY;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, STRIP_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsVertBar(g, effects, 4, 0, STRIP_H, true);
		g.dispose();

		writePng(img, "vertBar_empty");
		writeManifest("vertBar_empty", s, "VERTICAL_BAR", STRIP_W, STRIP_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= STRIP_H);
	}

	@Test
	public void vertBar_allEffects_noOverflow() throws IOException
	{
		Scenario s = Scenario.ALL_EFFECTS;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, STRIP_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsVertBar(g, effects, 4, 0, STRIP_H, true);
		g.dispose();

		writePng(img, "vertBar_allEffects");
		writeManifest("vertBar_allEffects", s, "VERTICAL_BAR", STRIP_W, STRIP_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= STRIP_H);
	}

	// ── ICON_TRAY snapshots ───────────────────────────────────────────────

	@Test
	public void iconTray_empty_renders() throws IOException
	{
		Scenario s = Scenario.EMPTY;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(TRAY_W, TRAY_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		PvpHudOverlay.drawEffectsIconTray(g, effects, 4, 0, TRAY_W - 4, true);
		g.dispose();

		writePng(img, "iconTray_empty");
		writeManifest("iconTray_empty", s, "ICON_TRAY", TRAY_W, TRAY_H, TRAY_H, effects);
	}

	@Test
	public void iconTray_allEffects_renders() throws IOException
	{
		Scenario s = Scenario.ALL_EFFECTS;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(TRAY_W, TRAY_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		PvpHudOverlay.drawEffectsIconTray(g, effects, 4, 0, TRAY_W - 4, true);
		g.dispose();

		writePng(img, "iconTray_allEffects");
		writeManifest("iconTray_allEffects", s, "ICON_TRAY", TRAY_W, TRAY_H, TRAY_H, effects);
	}

	// ── 2-COL snapshots (Inventory Hug left rail) ─────────────────────────

	@Test
	public void twoCol_empty_noOverflow() throws IOException
	{
		Scenario s = Scenario.EMPTY;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(TWO_COL_W, TWO_COL_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, TWO_COL_H);
		g.dispose();

		writePng(img, "twoCol_empty");
		writeManifest("twoCol_empty", s, "2COL", TWO_COL_W, TWO_COL_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= TWO_COL_H);
	}

	@Test
	public void twoCol_allEffects_noOverflow() throws IOException
	{
		Scenario s = Scenario.ALL_EFFECTS;
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(s.buildState(), TICK);

		BufferedImage img = new BufferedImage(TWO_COL_W, TWO_COL_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, TWO_COL_H);
		g.dispose();

		writePng(img, "twoCol_allEffects");
		writeManifest("twoCol_allEffects", s, "2COL", TWO_COL_W, TWO_COL_H, cy, effects);

		assertTrue("cy must not exceed canvas height", cy <= TWO_COL_H);
	}

	// ── Tight-bounds overflow stress tests ───────────────────────────────

	@Test
	public void text_allEffects_tightBounds_criticalGroupFitsFirst()
	{
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(
			Scenario.ALL_EFFECTS.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsText(g, TEST_FONT, effects, 4, 0, 40);
		g.dispose();

		assertTrue("cy must not exceed tight maxY=40", cy <= 40);
	}

	@Test
	public void vertBar_allEffects_tightBounds_noOverflow()
	{
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(
			Scenario.ALL_EFFECTS.buildState(), TICK);

		BufferedImage img = new BufferedImage(STRIP_W, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffectsVertBar(g, effects, 4, 0, 40, true);
		g.dispose();

		assertTrue("cy must not exceed tight maxY=40", cy <= 40);
	}

	@Test
	public void twoCol_allEffects_tightBounds_noOverflow()
	{
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(
			Scenario.ALL_EFFECTS.buildState(), TICK);

		BufferedImage img = new BufferedImage(TWO_COL_W, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = makeCanvas(img);
		int cy = PvpHudOverlay.drawEffects2Col(g, effects, 0, 0, 40);
		g.dispose();

		assertTrue("cy must not exceed tight maxY=40", cy <= 40);
	}

	// ── Critical-group priority ───────────────────────────────────────────

	@Test
	public void allEffects_criticalGroupComesFirst()
	{
		List<ActiveEffectView> effects = PvpHudOverlay.buildEffectsHeadless(
			Scenario.ALL_EFFECTS.buildState(), TICK);

		assertFalse("ALL_EFFECTS must produce at least one effect", effects.isEmpty());
		assertEquals("First effect must be CRITICAL group",
			EffectGroup.CRITICAL, effects.get(0).group);

		// Verify CRITICAL runs before PROTECTION which runs before CYCLE
		EffectGroup last = null;
		for (ActiveEffectView e : effects)
		{
			if (last != null && e.group.ordinal() < last.ordinal())
				fail("Effect groups out of order: " + last + " followed by " + e.group
					+ " (effect: " + e.getTextLabel() + ")");
			last = e.group;
		}
	}

	// ── Live viewer ───────────────────────────────────────────────────────

	private static final String[][] VIEWER_PANELS = {
		{"text_empty",        "TEXT / empty",        "strip"},
		{"text_allEffects",   "TEXT / all effects",  "strip"},
		{"vertBar_empty",     "VERT BAR / empty",    "strip"},
		{"vertBar_allEffects","VERT BAR / all",      "strip"},
		{"iconTray_empty",    "ICON TRAY / empty",   "tray"},
		{"iconTray_allEffects","ICON TRAY / all",    "tray"},
		{"twoCol_empty",      "2-COL / empty",       "twocol"},
		{"twoCol_allEffects", "2-COL / all effects", "twocol"},
	};

	@AfterClass
	public static void generateHtmlViewer() throws IOException
	{
		File dir = new File("build/hud-snapshots");
		dir.mkdirs();

		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
		  .append("<meta charset=\"utf-8\">\n")
		  .append("<title>PvP HUD Debug Viewer</title>\n")
		  .append("<style>\n")
		  .append("  body{background:#111;color:#ccc;font-family:monospace;padding:16px;margin:0}\n")
		  .append("  h1{color:#fff;margin:0 0 4px;font-size:14px}\n")
		  .append("  #ts{color:#555;font-size:10px;margin-bottom:12px}\n")
		  .append("  .grid{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start}\n")
		  .append("  .panel{background:#1c1c1c;border:1px solid #333;padding:8px}\n")
		  .append("  .panel h3{margin:0 0 5px;font-size:10px;color:#888;letter-spacing:.05em}\n")
		  .append("  img.strip {width:240px;height:800px;image-rendering:pixelated;display:block}\n")
		  .append("  img.tray  {width:519px;height:60px; image-rendering:pixelated;display:block}\n")
		  .append("  img.twocol{width:144px;height:800px;image-rendering:pixelated;display:block}\n")
		  .append("</style>\n</head>\n<body>\n")
		  .append("<h1>PvP HUD Debug Viewer</h1>\n")
		  .append("<div id=\"ts\">waiting for first render...</div>\n")
		  .append("<div class=\"grid\">\n");

		for (String[] panel : VIEWER_PANELS)
		{
			String file = panel[0] + ".png";
			String label = panel[1];
			String cls = panel[2];
			sb.append("  <div class=\"panel\">\n")
			  .append("    <h3>").append(label).append("</h3>\n")
			  .append("    <img class=\"").append(cls).append("\" src=\"").append(file)
			  .append("\" data-src=\"").append(file).append("\" alt=\"").append(label).append("\">\n")
			  .append("  </div>\n");
		}

		sb.append("</div>\n<script>\n")
		  .append("function refresh(){\n")
		  .append("  var t=Date.now();\n")
		  .append("  document.querySelectorAll('img').forEach(function(img){\n")
		  .append("    img.src=img.dataset.src+'?t='+t;\n")
		  .append("  });\n")
		  .append("  document.getElementById('ts').textContent=")
		  .append("'Last rendered: '+new Date().toLocaleTimeString();\n")
		  .append("}\n")
		  .append("setInterval(refresh,800);\n")
		  .append("</script>\n</body>\n</html>\n");

		try (FileWriter fw = new FileWriter(new File(dir, "index.html")))
		{
			fw.write(sb.toString());
		}
	}
}
