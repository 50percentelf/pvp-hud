package com.pvphud;

import org.junit.Test;
import static org.junit.Assert.*;

/** Exhaustive tests for {@link PvpHudOverlay#formatCompactAmmoQuantity}. */
public class AmmoFormatTest
{
    // ── Below 1000: shown as-is ──────────────────────────────────────────────

    @Test public void qty1()     { assertEquals("1",   PvpHudOverlay.formatCompactAmmoQuantity(1));   }
    @Test public void qty25()    { assertEquals("25",  PvpHudOverlay.formatCompactAmmoQuantity(25));  }
    @Test public void qty247()   { assertEquals("247", PvpHudOverlay.formatCompactAmmoQuantity(247)); }
    @Test public void qty999()   { assertEquals("999", PvpHudOverlay.formatCompactAmmoQuantity(999)); }

    // ── Round thousands ──────────────────────────────────────────────────────

    @Test public void qty1000()  { assertEquals("1k",   PvpHudOverlay.formatCompactAmmoQuantity(1000));  }
    @Test public void qty10000() { assertEquals("10k",  PvpHudOverlay.formatCompactAmmoQuantity(10000)); }
    @Test public void qty50000() { assertEquals("50k",  PvpHudOverlay.formatCompactAmmoQuantity(50000)); }

    // ── Fractional thousands (one decimal) ───────────────────────────────────

    @Test public void qty1200()  { assertEquals("1.2k",  PvpHudOverlay.formatCompactAmmoQuantity(1200));  }
    @Test public void qty9800()  { assertEquals("9.8k",  PvpHudOverlay.formatCompactAmmoQuantity(9800));  }
    @Test public void qty12500() { assertEquals("12.5k", PvpHudOverlay.formatCompactAmmoQuantity(12500)); }

    // ── No trailing ".0" (round thousands must not use decimal form) ─────────

    @Test public void qty2000_noDecimal() { assertFalse(PvpHudOverlay.formatCompactAmmoQuantity(2000).contains(".")); }
    @Test public void qty5000_noDecimal() { assertFalse(PvpHudOverlay.formatCompactAmmoQuantity(5000).contains(".")); }

    // ── Rounding boundary: 99999 rounds up to 100k ──────────────────────────

    @Test public void qty99999_roundsTo100k() { assertEquals("100k", PvpHudOverlay.formatCompactAmmoQuantity(99999)); }

    // ── Boundary: 999 stays below-k, 1000 flips to k ────────────────────────

    @Test public void boundary_999_noK()  { assertFalse(PvpHudOverlay.formatCompactAmmoQuantity(999).contains("k"));  }
    @Test public void boundary_1000_hasK() { assertTrue(PvpHudOverlay.formatCompactAmmoQuantity(1000).contains("k")); }
}
