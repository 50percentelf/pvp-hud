package com.pvphud;

/**
 * Four-tier priority for HUD status slots.
 * Render order: CRITICAL → TACTICAL → SUSTAIN → READINESS.
 * Overflow policy: CRITICAL is never displaced; lower tiers drop first.
 */
public enum StatusPriority
{
	/** Hard restrictions that immediately cap what the player can do: freeze, teleblock, prayer lock, corruption. */
	CRITICAL,

	/** Active threats and time-limited tactical buffs: poison/venom, burn, SOTD, ward, mark. */
	TACTICAL,

	/** Ongoing sustain and protection effects: anti-venom/poison, divines, menaphite, prayer regen, stamina, antifire. */
	SUSTAIN,

	/** Passive cooldowns with no urgency: imbued heart cooldown, surge potion. */
	READINESS
}
