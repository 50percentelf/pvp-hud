package com.pvphud;

import net.runelite.client.game.ItemEquipmentStats;
import org.junit.Test;
import static org.junit.Assert.*;

public class AmmoClassificationTest
{
    @Test
    public void rstrPositive_isSupported()
    {
        ItemEquipmentStats eq = ItemEquipmentStats.builder().rstr(60).build();
        assertTrue(PvpHudPlugin.isSupportedAmmo(eq));
    }

    @Test
    public void rstrZero_withRangeAttack_notSupported()
    {
        // Blessing-like: has arange from an offhand effect but zero ranged strength
        ItemEquipmentStats eq = ItemEquipmentStats.builder().arange(8).build(); // rstr defaults to 0
        assertFalse(PvpHudPlugin.isSupportedAmmo(eq));
    }

    @Test
    public void prayerBonus_zeroRstr_notSupported()
    {
        ItemEquipmentStats eq = ItemEquipmentStats.builder().prayer(6).build();
        assertFalse(PvpHudPlugin.isSupportedAmmo(eq));
    }

    @Test
    public void nullStats_notSupported()
    {
        assertFalse(PvpHudPlugin.isSupportedAmmo(null));
    }

    @Test
    public void allZeroStats_notSupported()
    {
        ItemEquipmentStats eq = ItemEquipmentStats.builder().build(); // all zero
        assertFalse(PvpHudPlugin.isSupportedAmmo(eq));
    }

    @Test
    public void highRstr_typicalDragonArrow_isSupported()
    {
        // Dragon arrows have rstr=60 in OSRS
        ItemEquipmentStats eq = ItemEquipmentStats.builder().rstr(60).arange(20).build();
        assertTrue(PvpHudPlugin.isSupportedAmmo(eq));
    }
}
