package com.pvphud.state;

import lombok.Getter;
import lombok.Setter;

/**
 * PvP-relevant stat levels. Stores real and current boosted values so the HUD
 * can display only the stats that have changed from base.
 */
public class BoostState
{
	@Getter @Setter private int attackReal, attackBoosted;
	@Getter @Setter private int strengthReal, strengthBoosted;
	@Getter @Setter private int defenceReal, defenceBoosted;
	@Getter @Setter private int rangedReal, rangedBoosted;
	@Getter @Setter private int magicReal, magicBoosted;

	public int getAttackDelta()   { return attackBoosted   - attackReal;   }
	public int getStrengthDelta() { return strengthBoosted - strengthReal; }
	public int getDefenceDelta()  { return defenceBoosted  - defenceReal;  }
	public int getRangedDelta()   { return rangedBoosted   - rangedReal;   }
	public int getMagicDelta()    { return magicBoosted    - magicReal;    }

	/** True if any tracked stat differs from base. */
	public boolean hasAnyBoost()
	{
		return getAttackDelta() != 0 || getStrengthDelta() != 0
			|| getDefenceDelta() != 0 || getRangedDelta() != 0
			|| getMagicDelta() != 0;
	}

	public void reset()
	{
		attackReal = attackBoosted = 0;
		strengthReal = strengthBoosted = 0;
		defenceReal = defenceBoosted = 0;
		rangedReal = rangedBoosted = 0;
		magicReal = magicBoosted = 0;
	}
}
