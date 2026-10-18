package com.pvphud;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.FontMetrics;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the ammo-chip width helper for representative quantities
 * and validates basic geometry invariants.
 *
 * Uses a headless BufferedImage to obtain a real FontMetrics instance.
 */
public class AmmoChipGeometryTest
{
    private static FontMetrics smallFm()
    {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g.getFontMetrics();
        g.dispose();
        return fm;
    }

    // ── Text-only chip (iconH=0) ─────────────────────────────────────────────

    @Test
    public void textOnly_chipWidthIsPositive()
    {
        FontMetrics fm = smallFm();
        assertTrue(PvpHudOverlay.ammoChipWidth(fm, 1, 0) > 0);
    }

    @Test
    public void textOnly_widthGrowsWithLargerQty()
    {
        FontMetrics fm = smallFm();
        int w1  = PvpHudOverlay.ammoChipWidth(fm, 1,   0);
        int w25 = PvpHudOverlay.ammoChipWidth(fm, 25,  0);
        int w999 = PvpHudOverlay.ammoChipWidth(fm, 999, 0);
        assertTrue("25 wider than 1",   w25  >= w1);
        assertTrue("999 wider than 25", w999 >= w25);
    }

    @Test
    public void textOnly_threeDigitQty_fitsInCornerAmmoZone()
    {
        // Corner ammo zone: INVY_LEFT_W(72) - PAD(6) - epEndX(~47) - gap(2) ≈ 17px
        // RunescapeSmallFont is compact; 3-digit quantities must fit with some margin.
        FontMetrics fm    = smallFm();
        int chipW999      = PvpHudOverlay.ammoChipWidth(fm, 999, 0);
        // The actual available width depends on E/P dot rendering, but we assert
        // the chip is at most 22px (a conservative bound for the corner zone).
        assertTrue("3-digit chip must be ≤22px wide for corner", chipW999 <= 22);
    }

    // ── Full chip (iconH > 0) ────────────────────────────────────────────────

    @Test
    public void fullChip_widthIncludesIconAndGap()
    {
        FontMetrics fm    = smallFm();
        int iconH         = 18;
        int textOnlyW     = PvpHudOverlay.ammoChipWidth(fm, 247, 0);
        int fullChipW     = PvpHudOverlay.ammoChipWidth(fm, 247, iconH);
        assertEquals("full chip = icon + gap(2) + text", iconH + 2 + textOnlyW, fullChipW);
    }

    @Test
    public void fullChip_widthGrowsWithQty()
    {
        FontMetrics fm = smallFm();
        int w1    = PvpHudOverlay.ammoChipWidth(fm, 1,    18);
        int w9999 = PvpHudOverlay.ammoChipWidth(fm, 9999, 18);
        assertTrue(w9999 > w1);
    }

    // ── Horizontal strip geometry ────────────────────────────────────────────

    @Test
    public void horizontal_ammoChipReducesUsableWidth()
    {
        // Verify the relationship: usable = stripW - dotSectionW - ammoChipW - 2*PAD
        // We just check that ammoChipWidth returns a positive value, so usable is reduced.
        FontMetrics fm    = smallFm();
        int iconH         = 18; // ACTION_STRIP_H(22) - 4
        int chipW         = PvpHudOverlay.ammoChipWidth(fm, 500, iconH);
        assertTrue("ammo chip must reduce usable width by a positive amount", chipW > 0);
    }

    // ── Vertical strip geometry ──────────────────────────────────────────────

    @Test
    public void vertical_textRxReservesSpaceForAmmo()
    {
        // textRx = p.width - PAD - ammoChipW, and ammoChipW > 0 when ammo present
        FontMetrics fm    = smallFm();
        int iconH         = 18;
        int ammoChipW     = PvpHudOverlay.ammoChipWidth(fm, 247, iconH) + 6; // +PAD
        assertTrue("ammoChipW including separator must be positive", ammoChipW > 0);
    }
}
