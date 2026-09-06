package com.pvphud.state;

import lombok.Value;

/** Immutable snapshot of a single combat event for center-panel display. */
@Value
public class CombatEvent
{
	CombatEventType type;
	/** Primary damage value; 0 when not applicable. */
	int damage;
	/** Prayer points drained; 0 when not applicable. */
	int prayerDrain;
}
