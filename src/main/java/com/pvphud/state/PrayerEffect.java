package com.pvphud.state;

/**
 * Generalized prayer impact type attached to a CombatEvent.
 * The actual drain/restore amounts are carried in the event's prayerDrain/prayerRestore fields.
 */
public enum PrayerEffect
{
	NONE,
	SMITE,
	SARA_STRIKE,
	CLEAR_MIND
}
