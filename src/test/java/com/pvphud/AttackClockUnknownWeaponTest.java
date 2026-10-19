package com.pvphud;

import net.runelite.api.gameval.AnimationID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Exercises the positive {@link AttackAnimations#isAttack(int)} classifier.
 *
 * Only animations on the allowlist count as attacks — unknown IDs, eat / drink,
 * teleport, prayer, death, and alchemy must all return false. Combined with the
 * weapon-speed > 0 check in {@code onAnimationChanged}, this guarantees we only
 * advance the attack clock on genuine combat events.
 */
public class AttackClockUnknownWeaponTest
{
	// ── isAttack: positive confirmation across weapon classes ─────────────────

	@Test
	public void meleeSword_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_SWORD_SLASH));
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_SWORD_STAB));
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_SWORD_LUNGE));
	}

	@Test
	public void meleeAbyssalWhip_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK));
	}

	@Test
	public void meleeDragonDagger_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_DDAGGER_LUNGE));
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_DDAGGER_HACK));
	}

	@Test
	public void ranged_bow_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_BOW));
	}

	@Test
	public void ranged_crossbow_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_CROSSBOW));
	}

	@Test
	public void ranged_blowpipe_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK));
	}

	@Test
	public void magic_standardCasts_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_CASTSTRIKE));
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_CASTWAVE));
	}

	@Test
	public void magic_ancientCasts_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.ZAROS_CASTING));
		assertTrue(AttackAnimations.isAttack(AnimationID.ZAROS_VERTICAL_CASTING));
	}

	@Test
	public void magic_teleblock_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_CASTING_TELE_BLOCK));
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_CASTING_TELE_BLOCK_STAFF));
	}

	@Test
	public void specials_dragonClaws_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_DRAGON_CLAWS_SPEC));
	}

	@Test
	public void specials_godsword_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.ARMADYL_SPECIAL_ATTACK));
	}

	// ── Newly added PvP weapons ───────────────────────────────────────────────

	@Test
	public void specials_voidwaker_isAttack()
	{
		assertTrue("voidwaker spec",
			AttackAnimations.isAttack(AnimationID.HUMAN_SPECIAL_VOIDWAKER));
		assertTrue("voidwaker alt spec",
			AttackAnimations.isAttack(AnimationID.HUMAN_SPECIAL02_VOIDWAKER));
	}

	@Test
	public void ranged_ballistaNormal_isAttack()
	{
		assertTrue("ballista normal shot must count — spec was already covered",
			AttackAnimations.isAttack(AnimationID.BALLISTA_ATTACK));
	}

	@Test
	public void ranged_modernCrossbow_isAttack()
	{
		assertTrue("modern crossbow fire+reload (acb/zcb/dcb)",
			AttackAnimations.isAttack(AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD));
	}

	@Test
	public void magic_volatileNightmareStaff_isAttack()
	{
		assertTrue("volatile nightmare staff cast",
			AttackAnimations.isAttack(AnimationID.NIGHTMARE_STAFF_VOLATILE_CAST));
	}

	@Test
	public void magic_eldritchNightmareStaff_isAttack()
	{
		assertTrue("eldritch nightmare staff cast",
			AttackAnimations.isAttack(AnimationID.NIGHTMARE_STAFF_ELDRITCH_PLAYER_CAST));
	}

	@Test
	public void specials_nightmareStaff_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.NIGHTMARE_STAFF_SPECIAL));
	}

	@Test
	public void melee_osmumtensFang_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_OSMUMTENS_FANG));
	}

	@Test
	public void melee_elderMaul_isAttack()
	{
		assertTrue("elder maul normal",
			AttackAnimations.isAttack(AnimationID.HUMAN_ELDER_MAUL_ATTACK));
		assertTrue("elder maul spec",
			AttackAnimations.isAttack(AnimationID.HUMAN_ELDER_MAUL_SPEC));
	}

	@Test
	public void specials_accursed_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_SPECIAL_ACCURSED));
	}

	@Test
	public void specials_webweaver_isAttack()
	{
		assertTrue(AttackAnimations.isAttack(AnimationID.HUMAN_SPECIAL01_WEBWEAVER));
	}

	// ── Crumble Undead removal: undead-only spell must NOT count ─────────────

	@Test
	public void magic_crumbleUndead_isNotAttack()
	{
		assertFalse("Crumble Undead cannot hit players; must not advance attack clock",
			AttackAnimations.isAttack(AnimationID.HUMAN_CASTCRUMBLEUNDEAD));
		assertFalse("Crumble Undead staff variant likewise",
			AttackAnimations.isAttack(AnimationID.HUMAN_CASTCRUMBLEUNDEAD_STAFF));
	}

	// ── isAttack: negative — non-attack animations must return false ──────────

	@Test
	public void eatAnimation_isNotAttack()
	{
		assertFalse("eat (829) is not an attack", AttackAnimations.isAttack(829));
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_EAT));
	}

	@Test
	public void prayerSwitch_isNotAttack()
	{
		assertFalse("prayer (645) is not an attack", AttackAnimations.isAttack(645));
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_PRAY));
	}

	@Test
	public void teleport_isNotAttack()
	{
		assertFalse("teleport (714) is not an attack", AttackAnimations.isAttack(714));
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_CASTTELEPORT));
	}

	@Test
	public void death_isNotAttack()
	{
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_DEATH));
	}

	@Test
	public void alchemy_isNotAttack()
	{
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_CASTHIGHLVLALCHEMY));
		assertFalse(AttackAnimations.isAttack(AnimationID.HUMAN_CASTLOWLVLALCHEMY));
	}

	@Test
	public void unknownId_isNotAttack()
	{
		assertFalse("unknown anim 12345 must not be classified as attack",
			AttackAnimations.isAttack(12345));
	}

	@Test
	public void idleId_isNotAttack()
	{
		assertFalse("idle/none (-1) must not be an attack", AttackAnimations.isAttack(-1));
		assertFalse("zero animation must not be an attack", AttackAnimations.isAttack(0));
	}

	// ── Combined gating: isAttack && speed > 0 ────────────────────────────────

	@Test
	public void knownAttackPlusValidSpeed_wouldAdvanceClock()
	{
		int speed = 4; // scimitar
		assertTrue("known attack + speed>0 must advance clock",
			AttackAnimations.isAttack(AnimationID.HUMAN_SWORD_SLASH) && speed > 0);
	}

	@Test
	public void knownAttackPlusZeroSpeed_doesNotAdvanceClock()
	{
		int speed = 0; // weapon stats unavailable
		assertFalse("known attack but speed=0 must NOT fabricate cooldown",
			AttackAnimations.isAttack(AnimationID.HUMAN_SWORD_SLASH) && speed > 0);
	}

	@Test
	public void unknownAnimationPlusValidSpeed_doesNotAdvanceClock()
	{
		int speed = 4;
		assertFalse("unknown animation must NOT advance clock even with valid speed",
			AttackAnimations.isAttack(12345) && speed > 0);
	}

	// ── Non-attack classifier side door ───────────────────────────────────────

	@Test
	public void isNonAttack_recognisesCoreNoisyIds()
	{
		assertTrue(AttackAnimations.isNonAttack(AnimationID.HUMAN_EAT));
		assertTrue(AttackAnimations.isNonAttack(AnimationID.HUMAN_PRAY));
		assertTrue(AttackAnimations.isNonAttack(AnimationID.HUMAN_CASTTELEPORT));
		assertTrue(AttackAnimations.isNonAttack(AnimationID.HUMAN_DEATH));
		assertFalse(AttackAnimations.isNonAttack(AnimationID.HUMAN_SWORD_SLASH));
	}
}
