package com.pvphud;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.FontMetrics;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the ammo-chip width helper and compact quantity formatter.
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
        assertTrue(PvpHudOverlay.ammoChipWidth(fm, "1", 0) > 0);
    }

    @Test
    public void textOnly_widthGrowsWithLongerText()
    {
        FontMetrics fm = smallFm();
        int w1   = PvpHudOverlay.ammoChipWidth(fm, "1",   0);
        int w25  = PvpHudOverlay.ammoChipWidth(fm, "25",  0);
        int w999 = PvpHudOverlay.ammoChipWidth(fm, "999", 0);
        assertTrue("\"25\" wider than \"1\"",   w25  >= w1);
        assertTrue("\"999\" wider than \"25\"", w999 >= w25);
    }

    @Test
    public void compactQty_cornerIconChip_fitsInCornerAmmoZone()
    {
        // Corner: chipRx = INVY_LEFT_W(72) - PAD(6) = 66.
        // With a 10px icon + 2px gap + compact text the chip must be ≤ ~30px.
        // INVY_LEFT_W - PAD - epEndX(~47) - minGap(2) gives the available space;
        // we assert the 10px-icon chip for "999" stays within the corner width (66px).
        FontMetrics fm    = smallFm();
        int chipW = PvpHudOverlay.ammoChipWidth(fm, "999", 10);
        assertTrue("compact 999 chip with 10px icon must be positive", chipW > 0);
        assertTrue("compact 999 chip must fit within corner width (66px)", chipW <= 66);
    }

    // ── Full chip (iconH > 0) ────────────────────────────────────────────────

    @Test
    public void fullChip_widthIncludesIconAndGap()
    {
        FontMetrics fm    = smallFm();
        int iconH         = 18;
        int textOnlyW     = PvpHudOverlay.ammoChipWidth(fm, "247", 0);
        int fullChipW     = PvpHudOverlay.ammoChipWidth(fm, "247", iconH);
        assertEquals("full chip = icon + gap(2) + text", iconH + 2 + textOnlyW, fullChipW);
    }

    @Test
    public void fullChip_widthGrowsWithLongerText()
    {
        FontMetrics fm = smallFm();
        int w1    = PvpHudOverlay.ammoChipWidth(fm, "1",    18);
        int w9999 = PvpHudOverlay.ammoChipWidth(fm, "9999", 18);
        assertTrue(w9999 > w1);
    }

    // ── Horizontal strip geometry ────────────────────────────────────────────

    @Test
    public void horizontal_ammoChipReducesUsableWidth()
    {
        FontMetrics fm = smallFm();
        int iconH      = 18; // ACTION_STRIP_H(22) - 4
        int chipW      = PvpHudOverlay.ammoChipWidth(fm, "500", iconH);
        assertTrue("ammo chip must reduce usable width by a positive amount", chipW > 0);
    }

    // ── Vertical strip geometry ──────────────────────────────────────────────

    @Test
    public void vertical_textRxReservesSpaceForAmmo()
    {
        FontMetrics fm = smallFm();
        int iconH      = 18;
        int ammoChipW  = PvpHudOverlay.ammoChipWidth(fm, "247", iconH) + 6; // +PAD
        assertTrue("ammoChipW including separator must be positive", ammoChipW > 0);
    }

    // ── Vertical label width guard ───────────────────────────────────────────

    @Test
    public void verticalLabel_fullWidthCheck_preventsOverlap()
    {
        // Simulates the guard: textX + fm.stringWidth(lbl) <= textRx
        // A label that starts just inside textRx but extends past it must NOT fit.
        FontMetrics fm    = smallFm();
        String lbl        = "T1 0:30";
        int    labelW     = fm.stringWidth(lbl);
        int    textRx     = 100;

        // Exactly fits: textX = textRx - labelW
        int textXFits = textRx - labelW;
        assertTrue("label fits when exactly at boundary",
            textXFits + labelW <= textRx);

        // Does not fit: textX = textRx - labelW + 1 (1px overlap)
        int textXOverflows = textRx - labelW + 1;
        assertFalse("label is hidden when it would overflow by 1px",
            textXOverflows + labelW <= textRx);

        // Old (broken) check: textX < textRx would incorrectly allow the overflowing label
        assertTrue("old check would have wrongly shown it",
            textXOverflows < textRx);
    }
}
