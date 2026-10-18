package com.pvphud.state;

/**
 * Groups all damage hitsplats from a single game tick into one record.
 * Outgoing = damage we dealt (isMine() hitsplats on opponent).
 * Incoming = damage we received during an active session.
 *
 * Up to MAX_STORED individual hit values are retained per direction;
 * additional hits still accumulate into the totals.
 */
public class DamageExchange
{
	private static final int MAX_STORED = 2;

	private final int tick;

	private int outgoingTotal;
	private int outgoingHitCount;
	private final int[] outgoingHits = new int[MAX_STORED];
	private int outgoingPrayerDrain;

	private int incomingTotal;
	private int incomingHitCount;
	private final int[] incomingHits = new int[MAX_STORED];

	public DamageExchange(int tick)
	{
		this.tick = tick;
	}

	public void addOutgoing(int damage, int prayerDrain)
	{
		outgoingTotal       += damage;
		outgoingPrayerDrain += prayerDrain;
		if (outgoingHitCount < MAX_STORED)
			outgoingHits[outgoingHitCount] = damage;
		outgoingHitCount++;
	}

	public void addIncoming(int damage)
	{
		incomingTotal += damage;
		if (incomingHitCount < MAX_STORED)
			incomingHits[incomingHitCount] = damage;
		incomingHitCount++;
	}

	public int getTick()                 { return tick; }
	public int getOutgoingTotal()        { return outgoingTotal; }
	public int getOutgoingHitCount()     { return outgoingHitCount; }
	public int getOutgoingPrayerDrain()  { return outgoingPrayerDrain; }
	public int getIncomingTotal()        { return incomingTotal; }
	public int getIncomingHitCount()     { return incomingHitCount; }

	public boolean hasOutgoing() { return outgoingHitCount > 0; }
	public boolean hasIncoming() { return incomingHitCount > 0; }

	public int getOutgoingHit(int index)
	{
		if (index < 0 || index >= outgoingHitCount || index >= MAX_STORED) return 0;
		return outgoingHits[index];
	}

	public int getIncomingHit(int index)
	{
		if (index < 0 || index >= incomingHitCount || index >= MAX_STORED) return 0;
		return incomingHits[index];
	}
}
