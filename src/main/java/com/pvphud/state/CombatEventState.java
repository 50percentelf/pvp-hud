package com.pvphud.state;

import lombok.Getter;

/** Holds the currently displayed center-panel combat event and its expiry. */
public class CombatEventState
{
	@Getter
	private CombatEvent currentEvent;
	private long eventExpiryMs = -1;

	public boolean hasActiveEvent()
	{
		return currentEvent != null && System.currentTimeMillis() < eventExpiryMs;
	}

	public void post(CombatEvent event, long displayDurationMs)
	{
		this.currentEvent = event;
		this.eventExpiryMs = System.currentTimeMillis() + displayDurationMs;
	}

	public void clear()
	{
		currentEvent = null;
		eventExpiryMs = -1;
	}
}
