package com.pvphud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters animation IDs that should NOT reset the attack clock.
 *
 * An animation being absent from the blocklist is insufficient alone — callers
 * must also verify that the local player is interacting with a Player target.
 * Together these two guards eliminate the most common false positives in PvP:
 * eating/drinking while targeted, prayer changes, teleports, and emotes.
 */
public final class AttackAnimations
{
	private AttackAnimations() {}

	/**
	 * Animations that are definitively NOT attacks and must be ignored even when
	 * the player is interacting with an opponent.
	 */
	private static final Set<Integer> BLOCKLIST = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(
			// Eat / drink — various foods and potions share animation 829
			829,
			// Additional eat animations seen for some items
			2099,
			1157,
			// High alch / low alch
			713,
			// Teleport gfx / animation variants
			1816, 8939, 714,
			// Prayer overhead switching
			645,
			// Death
			836,
			// Emote-triggered animations that occasionally fire while targeted
			0   // idle / reset
		))
	);

	/**
	 * Returns true if this animation should prevent the attack clock from being set.
	 * A return value of false does NOT guarantee the animation is an attack — it only
	 * means it is not on the known non-attack list.
	 */
	public static boolean isBlocked(int animId)
	{
		return animId == -1 || BLOCKLIST.contains(animId);
	}
}
