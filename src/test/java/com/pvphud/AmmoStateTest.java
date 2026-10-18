package com.pvphud;

import com.pvphud.state.SelfState;
import org.junit.Test;
import static org.junit.Assert.*;

public class AmmoStateTest
{
    @Test
    public void initialState_ammoIdIsNegativeOne()
    {
        SelfState self = new SelfState();
        assertEquals(-1, self.getAmmoItemId());
    }

    @Test
    public void initialState_ammoQtyIsZero()
    {
        SelfState self = new SelfState();
        assertEquals(0, self.getAmmoQuantity());
    }

    @Test
    public void reset_clearsAmmoId()
    {
        SelfState self = new SelfState();
        self.setAmmoItemId(892); // dragon arrow item ID (example)
        self.reset();
        assertEquals(-1, self.getAmmoItemId());
    }

    @Test
    public void reset_clearsAmmoQty()
    {
        SelfState self = new SelfState();
        self.setAmmoQuantity(500);
        self.reset();
        assertEquals(0, self.getAmmoQuantity());
    }

    @Test
    public void setAndGet_ammoIdRoundtrip()
    {
        SelfState self = new SelfState();
        self.setAmmoItemId(9143); // rune crossbow bolt (example)
        assertEquals(9143, self.getAmmoItemId());
    }

    @Test
    public void setAndGet_ammoQtyRoundtrip()
    {
        SelfState self = new SelfState();
        self.setAmmoQuantity(247);
        assertEquals(247, self.getAmmoQuantity());
    }
}
