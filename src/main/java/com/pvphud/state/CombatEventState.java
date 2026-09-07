package com.pvphud.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Maintains a rolling log of the most recent combat events (last 5). */
public class CombatEventState
{
	private static final int MAX_LOG = 5;

	private final ArrayDeque<CombatEvent> log = new ArrayDeque<>(MAX_LOG);

	public void post(CombatEvent event)
	{
		if (log.size() >= MAX_LOG)
		{
			log.pollLast();
		}
		log.addFirst(event);
	}

	/** Returns events newest-first; never null, may be empty. */
	public List<CombatEvent> getRecentEvents()
	{
		return Collections.unmodifiableList(new ArrayList<>(log));
	}

	public boolean hasEvents()
	{
		return !log.isEmpty();
	}

	public void clear()
	{
		log.clear();
	}
}
