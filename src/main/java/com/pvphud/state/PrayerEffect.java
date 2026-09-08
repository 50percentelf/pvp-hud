package com.pvphud.state;

/**
 * Prayer impact type attached to a CombatEvent.
 * Only NONE and SMITE are set by v0.1 event handlers.
 * SARA_STRIKE and CLEAR_MIND are defined for future use but unreachable in v0.1.
 */
public enum PrayerEffect
{
	NONE,
	SMITE,
	/** Deferred post-v0.1 — no v0.1 code path sets this value. */
	SARA_STRIKE,
	/** Deferred post-v0.1 — no v0.1 code path sets this value. */
	CLEAR_MIND
}
