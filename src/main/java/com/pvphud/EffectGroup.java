package com.pvphud;

/**
 * Semantic category for a status effect, used to drive display priority,
 * visual grouping, and overflow truncation.
 *
 * Render order: CRITICAL → PROTECTION → CYCLE.
 * Overflow drops CYCLE first, then PROTECTION; CRITICAL is never truncated.
 */
public enum EffectGroup
{
	/** Immediate threats or hard restrictions: freeze, teleblock, poison, venom. */
	CRITICAL,

	/** Ongoing defensive or sustain effects: anti-poison/venom, divines, stamina, antifire. */
	PROTECTION,

	/** Periodic or informational cycle timers: next proc, stat drain, debuff restore. */
	CYCLE
}
