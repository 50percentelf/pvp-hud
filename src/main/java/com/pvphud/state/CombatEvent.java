package com.pvphud.state;

import lombok.Value;

/** Immutable snapshot of a single combat event. */
@Value
public class CombatEvent
{
	CombatEventType type;
	/** Primary damage value; 0 when not applicable. */
	int damage;
	/** Prayer points drained (e.g. Smite, Sara Strike, Clear Mind). 0 = none. */
	int prayerDrain;
	/** Prayer points restored by the effect (e.g. Clear Mind). 0 = none. */
	int prayerRestore;
	/** Which prayer-impact mechanic caused the drain/restore, or NONE. */
	PrayerEffect effectType;
	/** True when this hit was confirmed as a max-hit (CHANCE! event). */
	boolean chance;
	/** True when this hit landed same-tick as another hit in the opposite direction. */
	boolean stack;
	/** Wall-clock time of the event (System.currentTimeMillis). */
	long timestampMs;

	/** Convenience factory: plain outgoing hit with no special effects. */
	public static CombatEvent outgoing(int damage, long ts)
	{
		return new CombatEvent(CombatEventType.OUTGOING_HIT, damage,
			0, 0, PrayerEffect.NONE, false, false, ts);
	}

	/** Convenience factory: incoming hit with a prayer-impact effect. */
	public static CombatEvent incoming(int damage, int prayerDrain,
		int prayerRestore, PrayerEffect effect, long ts)
	{
		return new CombatEvent(CombatEventType.INCOMING_HIT, damage,
			prayerDrain, prayerRestore, effect, false, false, ts);
	}

	/** Convenience factory: plain incoming hit (no prayer impact). */
	public static CombatEvent incoming(int damage, long ts)
	{
		return incoming(damage, 0, 0, PrayerEffect.NONE, ts);
	}
}
