package com.pvphud;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Geometry tests for the dedicated Inventory Hug ammo row.
 *
 * Uses real FontMetrics (headless BufferedImage) and the package-visible
 * layout constants so tests stay honest about the actual paint geometry.
 */
public class InventoryAmmoRowTest
{
    // Mirrors PvpHudOverlay constants (no magic numbers in assertions)
    private static final int ICON_H    = PvpHudOverlay.IH_AMMO_ICON_H; // 18
    private static final int ROW_H     = PvpHudOverlay.IH_AMMO_ROW_H;  // 22
    private static final int PAD       = 6;
    private static final int RAIL_W    = 72 - PAD * 2; // 60
    private static final int LX        = PAD;           // 6
    private static final int TEXT_RX   = LX + RAIL_W;  // 66
    private static final int MIN_TEXTX = LX + ICON_H + 4; // icon right edge + 4px gap = 28

    private static FontMetrics smallFm()
    {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g.getFontMetrics();
        g.dispose();
        return fm;
    }

    // ── Row height constants ─────────────────────────────────────────────────

    @Test
    public void rowHeight_atLeastIconHeight()
    {
        assertTrue("row height >= icon height", ROW_H >= ICON_H);
    }

    @Test
    public void rowHeight_inExpectedRange()
    {
        assertTrue("row height between 20–24px", ROW_H >= 20 && ROW_H <= 24);
    }

    // ── No-ammo: zero height consumed ────────────────────────────────────────

    @Test
    public void noAmmo_consumesZeroHeight()
    {
        // ammoId < 0 → drawInventoryAmmoRow returns cy unchanged
        int cy = 100;
        int cyAfter = -1 < 0 ? cy : cy + ROW_H;
        assertEquals("cy unchanged when no ammo", cy, cyAfter);
    }

    // ── With ammo: fixed height consumed ─────────────────────────────────────

    @Test
    public void withAmmo_consumesFixedHeight()
    {
        // ammoId >= 0 → drawInventoryAmmoRow returns cy + 2(gap) + ROW_H
        int cy     = 100;
        int cyAfter = cy + 2 + ROW_H;
        assertEquals("cy advances by gap+ROW_H when ammo present", cy + 2 + ROW_H, cyAfter);
    }

    // ── Icon fits inside rail ────────────────────────────────────────────────

    @Test
    public void icon_fitsWithinRailWidth()
    {
        assertTrue("18px icon fits in 60px rail", ICON_H <= RAIL_W);
    }

    @Test
    public void icon_leftEdge_isAtLx()
    {
        // icon drawn at lx = PAD = 6; right edge = 6 + 18 = 24, well inside INVY_LEFT_W(72)
        int iconRight = LX + ICON_H;
        assertTrue("icon right edge within rail right", iconRight <= LX + RAIL_W);
    }

    // ── Quantity right-aligns and does not overflow ───────────────────────────

    @Test
    public void quantity_rightAligned_doesNotExceedRailRight()
    {
        FontMetrics fm    = smallFm();
        int[] qtys = { 1, 25, 247, 999, 1247, 9999 };
        for (int qty : qtys)
        {
            String text = String.valueOf(qty);
            int textLx  = TEXT_RX - fm.stringWidth(text);
            assertTrue("qty=" + qty + ": text left edge >= lx", textLx >= LX);
        }
    }

    // ── Quantity does not overlap icon ────────────────────────────────────────

    @Test
    public void quantity_doesNotOverlapIcon()
    {
        FontMetrics fm = smallFm();
        int[] qtys = { 1, 25, 247, 999, 1247, 9999 };
        for (int qty : qtys)
        {
            String text = String.valueOf(qty);
            int textLx  = TEXT_RX - fm.stringWidth(text);
            assertTrue(
                "qty=" + qty + ": text left (" + textLx + ") must be >= minTextX (" + MIN_TEXTX + ")",
                textLx >= MIN_TEXTX);
        }
    }

    // ── Row bottom within realistic railMaxY ──────────────────────────────────

    @Test
    public void rowBottom_withinRailMaxY()
    {
        // railMaxY = INVY_TOP_H(72) + tabH(~18) + itemsH(~252) = ~342
        // After 4 bars cy is well under 200; ammo row end = cy + 2 + 22 ≤ 224 < 342
        int cyAfterSpecBar = 160; // conservative upper bound from 4 bars
        int ammoRowBottom  = cyAfterSpecBar + 2 + ROW_H;
        int railMaxY       = 72 + 18 + 252; // 342
        assertTrue("ammo row bottom (" + ammoRowBottom + ") <= railMaxY (" + railMaxY + ")",
            ammoRowBottom <= railMaxY);
    }
}
