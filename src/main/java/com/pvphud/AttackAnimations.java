package com.pvphud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;

/**
 * Positive classifier for player attack animations.
 *
 * The attack clock must only advance on confirmed combat animations — unknown
 * animations are treated as non-attacks. Combined with the "interacting with a
 * Player" guard in the plugin, this eliminates false positives from eating,
 * teleports, prayer swaps, emotes, and skilling.
 *
 * All animation IDs are sourced from RuneLite's canonical gameval
 * {@link AnimationID} constants where available, or documented by weapon name
 * for IDs that have no distinct constant.
 */
public final class AttackAnimations
{
	private AttackAnimations() {}

	/**
	 * Known melee/ranged/magic combat animations for player-wielded weapons.
	 *
	 * Includes the canonical weapon-class entries from
	 * {@link net.runelite.api.gameval.AnimationID}:
	 * sword, dragon dagger, dragon spear, two-handed sword, axe, blunt
	 * (mace/warhammer), staff, staff-with-orb, unarmed, dharok's, scythe,
	 * spear, halberd, zamorakian spear, bow, crossbow, dart/thrown, knife,
	 * and the generic magic-cast animations (strike/wave/surge/entangle,
	 * their staff variants, ancient Zaros casting, teleblock cast, etc.).
	 *
	 * Also includes well-known weapon specials (dragon claws, godsword, dragon
	 * scimitar spec, dragon halberd spec, abyssal bludgeon/dagger specs,
	 * voidwaker, accursed sceptre, webweaver, elder maul, nightmare staff
	 * volatile/eldritch/spec) and weapon-specific attacks that use their own
	 * distinct animations (abyssal whip, granite maul, inquisitor's mace,
	 * nightmare staff crush, dinh's bulwark, dragon hunter lance, soulreaper
	 * axe, blowpipe variants, ballista normal + spec, modern crossbow fire-
	 * and-reload, Osmumten's fang).
	 */
	private static final Set<Integer> ATTACK_IDS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(
			// ── Melee: dragon dagger ───────────────────────────────────────
			AnimationID.HUMAN_DDAGGER_LUNGE,         // 376
			AnimationID.HUMAN_DDAGGER_HACK,          // 377

			// ── Melee: dragon spear ────────────────────────────────────────
			AnimationID.HUMAN_DSPEAR_SLASH,          // 380
			AnimationID.HUMAN_DSPEAR_STAB,           // 381
			AnimationID.HUMAN_DSPEAR_LUNGE,          // 382

			// ── Melee: generic sword / scimitar / longsword ────────────────
			AnimationID.HUMAN_SWORD_STAB,            // 386
			AnimationID.HUMAN_SWORD_SLASH,           // 390
			AnimationID.HUMAN_SWORD_LUNGE,           // 392

			// ── Melee: axe (battleaxe) ─────────────────────────────────────
			AnimationID.HUMAN_AXE_CHOP,              // 393
			AnimationID.HUMAN_AXE_HACK,              // 395
			AnimationID.HUMAN_AXE_SMASH,             // 396

			// ── Melee: blunt (mace / warhammer) ────────────────────────────
			AnimationID.HUMAN_BLUNT_SPIKE,           // 400
			AnimationID.HUMAN_BLUNT_POUND,           // 401
			AnimationID.HUMAN_BLUNT_PUMMEL,          // 402

			// ── Melee: 2h sword (DH greataxe, armadyl godsword etc.) ───────
			AnimationID.HUMAN_DHSWORD_STAB,          // 405
			AnimationID.HUMAN_DHSWORD_CHOP,          // 406
			AnimationID.HUMAN_DHSWORD_SLASH,         // 407
			AnimationID.HUMAN_DHSWORD_LUNGE,         // 408
			AnimationID.HUMAN_DHSWORD_SPIN,          // 409

			// ── Melee: staff (bash) ────────────────────────────────────────
			AnimationID.HUMAN_STAFF_SPIKE,           // 412
			AnimationID.HUMAN_STAFF_POUND,           // 413
			AnimationID.HUMAN_STAFF_PUMMEL,          // 414
			AnimationID.HUMAN_STAFFORB_SPIKE,        // 417
			AnimationID.HUMAN_STAFFORB_POUND,        // 418
			AnimationID.HUMAN_STAFFORB_PUMMEL,       // 419

			// ── Melee: unarmed ─────────────────────────────────────────────
			AnimationID.HUMAN_UNARMEDPUNCH,          // 422
			AnimationID.HUMAN_UNARMEDKICK,           // 423

			// ── Ranged: bow (shortbow, longbow, rune, magic, 3a, tbow) ─────
			AnimationID.HUMAN_BOW,                   // 426

			// ── Ranged: crossbow (rune xbow, armadyl xbow, dragon xbow) ────
			AnimationID.HUMAN_CROSSBOW,              // 427

			// ── Melee: spear (regular / leaf-bladed) ───────────────────────
			AnimationID.HUMAN_SPEAR_SPIKE,           // 428
			AnimationID.HUMAN_SPEAR_LUNGE,           // 429
			AnimationID.HUMAN_SPEAR_THROW,           // 432

			// ── Melee: farmer's fork (included for completeness) ───────────
			AnimationID.HUMAN_FARMERSFORK_STAB,      // 433

			// ── Melee: scythe (scythe of vitur) ────────────────────────────
			AnimationID.HUMAN_SCYTHE_SLASH,          // 437
			AnimationID.HUMAN_SCYTHE_LUNGE,          // 438
			AnimationID.HUMAN_SCYTHE_SPIN,           // 439
			AnimationID.HUMAN_SCYTHE_SWEEP,          // 440

			// ── Magic: standard-spellbook casts ────────────────────────────
			// Crumble Undead is intentionally NOT included — it is an undead-only
			// spell and cannot hit players, so including it in the attack
			// allowlist would let a cast at a training dummy spoof the attack
			// clock while a player is interacted.
			AnimationID.HUMAN_CASTZAP,               // 707  (fire bolt family / wind / water / earth)
			AnimationID.HUMAN_CASTIBANBLAST,         // 708
			AnimationID.HUMAN_CASTENTANGLE,          // 710  (bind / snare / entangle)
			AnimationID.HUMAN_CASTSTRIKE,            // 711  (strike / bolt / blast)
			AnimationID.HUMAN_CASTCONFUSE,           // 716
			AnimationID.HUMAN_CASTWEAKEN,            // 717
			AnimationID.HUMAN_CASTCURSE,             // 718
			AnimationID.HUMAN_CASTWAVE,              // 727
			AnimationID.HUMAN_CASTENFEEBLE,          // 728
			AnimationID.HUMAN_CASTSTUN,              // 729

			// ── Magic: standard-spellbook staff variants ───────────────────
			AnimationID.HUMAN_CASTENTANGLE_STAFF,    // 1161
			AnimationID.HUMAN_CASTSTRIKE_STAFF,      // 1162
			AnimationID.HUMAN_CASTCONFUSE_STAFF,     // 1163
			AnimationID.HUMAN_CASTWEAKEN_STAFF,      // 1164
			AnimationID.HUMAN_CASTCURSE_STAFF,       // 1165
			AnimationID.HUMAN_CASTWAVE_STAFF,        // 1167
			AnimationID.HUMAN_CASTENFEEBLE_STAFF,    // 1168
			AnimationID.HUMAN_CASTSTUN_STAFF,        // 1169

			// ── Magic: tele-block (both wielded and no-staff) ──────────────
			AnimationID.HUMAN_CASTING_TELE_BLOCK,       // 1819
			AnimationID.HUMAN_CASTING_TELE_BLOCK_STAFF, // 1820

			// ── Magic: ancient spellbook casts (ice/blood/smoke/shadow) ────
			AnimationID.ZAROS_CASTING,               // 1978 (horizontal cast)
			AnimationID.ZAROS_VERTICAL_CASTING,      // 1979 (vertical cast — barrage etc.)

			// ── Magic: generic castings used by other wands/staves ─────────
			AnimationID.HUMAN_CASTING,               // 811  (generic cast ready/throw)
			AnimationID.HUMAN_CAST_SURGE,            // 7855

			// ── Specials: dragon dagger / dragon scimitar / dragon halberd ─
			AnimationID.SP_ATTACK_DRAGON_SCIMITAR,   // 1872
			AnimationID.DRAGON_HALBERD_SPECIAL_ATTACK, // 1203
			AnimationID.ARMADYL_SPECIAL_ATTACK,      // 1714 (armadyl godsword spec)

			// ── Specials: dragon claws ─────────────────────────────────────
			AnimationID.HUMAN_DRAGON_CLAWS_SPEC,     // 7514

			// ── Specials: abyssal weapons ──────────────────────────────────
			AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK,  // 1658
			AnimationID.SLAYER_WHIP_SP_ATTACK,       // 1669
			AnimationID.ABYSSAL_DAGGER_LUNGE,        // 3297
			AnimationID.ABYSSAL_DAGGER_SPECIAL,      // 3300
			AnimationID.ABYSSAL_BLUDGEON_CRUSH,      // 3298
			AnimationID.ABYSSAL_BLUDGEON_SPECIAL_ATTACK, // 3299

			// ── Specials: granite maul ─────────────────────────────────────
			AnimationID.SLAYER_GRANITE_MAUL_ATTACK,  // 1665
			AnimationID.SLAYER_GRANITE_MAUL_SPECIAL_ATTACK, // 1667
			AnimationID.SLAYER_MAUL_SP_ATTACK,       // 1668

			// ── Specials: weapon-specific unique attacks ───────────────────
			AnimationID.HUMAN_INQUISITORS_MACE_CRUSH,  // 4503
			AnimationID.HUMAN_NIGHTMARE_STAFF_CRUSH,   // 4505
			AnimationID.HUMAN_DINHS_BULWARK_CRUSH,     // 7507
			AnimationID.HUMAN_DHUNTER_LANCE_SLASH,     // 8289
			AnimationID.HUMAN_DHUNTER_LANCE_CRUSH,     // 8290
			AnimationID.HUMAN_SPELLSPEAR_STAB,         // 11119
			AnimationID.HUMAN_KNIFE_SLASH,             // 3747
			AnimationID.HUMAN_KNIFE_CHOP,              // 911

			// ── Specials: zamorakian/hasta spears ──────────────────────────
			AnimationID.HUMAN_ZAMORAKSPEAR_LUNGE,    // 1710
			AnimationID.HUMAN_ZAMORAKSPEAR_STAB,     // 1711
			AnimationID.HUMAN_ZAMORAKSPEAR_SLASH,    // 1712

			// ── Specials: 2h axe family (soulreaper axe) ───────────────────
			AnimationID.HUMAN_2H_AXE_ATTACK,         // 10602
			AnimationID.HUMAN_2H_AXE_SPECIAL,        // 10601

			// ── Ranged: blowpipes ──────────────────────────────────────────
			AnimationID.TOXIC_BLOWPIPE_SPECIAL_UPDATED, // 876
			AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK,      // 5061
			AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK_ORNAMENT, // 10656

			// ── Ranged: ballista ───────────────────────────────────────────
			AnimationID.BALLISTA_ATTACK,             // 7218 (ballista normal shot)
			AnimationID.BALLISTA_SPECIAL_ATTACK,     // 7222

			// ── Ranged: modern crossbow fire+reload (used by armadyl/zcb/dcb) ─
			AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD,          // 4230
			AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD_NO_STALL, // 11465 (no-animation-stall variant)

			// ── Ranged: Zaryte crossbow (distinct animation from generic xbow) ─
			AnimationID.ZCB_ATTACK,                  // 9166
			AnimationID.ZCB_ATTACK_PVN,              // 9168

			// ── Ranged: dragon knife / dragon knife (p) ────────────────────
			AnimationID.HUMAN_DRAGON_KNIFE,          // 8194
			AnimationID.HUMAN_DRAGON_KNIFE_P,        // 8195

			// ── Ranged: heavy ballista (light ballista normal+spec already above) ─
			AnimationID.BALLISTA02_ATTACK,           // 9465
			AnimationID.BALLISTA02_SPECIAL_ATTACK,   // 9467

			// ── Ranged: Venator bow ────────────────────────────────────────
			AnimationID.HUMAN_WEAPON_BOW_VENATOR01_SHOOT, // 9858

			// ── Ranged: Morrigan's javelin / throwing axe specials ─────────
			AnimationID.WEAPON_MORRIGANS_JAVELIN_SPECIAL01,    // 11467
			AnimationID.WEAPON_MORRIGANS_THROWINGAXE_SPECIAL01, // 11468

			// ── Ranged: Eclipse atlatl normal + spec ───────────────────────
			AnimationID.HUMAN_ATLATL_ATTACK_RANGED_01, // 11057
			AnimationID.HUMAN_SPECIAL_ATLATL_01,       // 11060

			// ── Ranged: dart / thrownaxe / throwing knife ──────────────────
			AnimationID.HUMAN_THROW,                 // 385
			AnimationID.II_HUMAN_DART_THROW,         // 6600

			// ── Specials: AGS — old (1714) + modern player animations ──────
			// ARMADYL_SPECIAL_ATTACK (1714) is the original AGS spec animation.
			// Current OSRS fires AGS_SPECIAL_PLAYER (7644) for the updated visuals.
			AnimationID.AGS_SPECIAL_PLAYER,          // 7644
			AnimationID.AGS_SPECIAL_ORNATE_PLAYER,   // 7645
			AnimationID.AGS_SPECIAL_BLIGHTED_PLAYER, // 10427

			// ── The Godsword special (newer graphics) ──────────────────────
			AnimationID.THE_GODSWORD_SPECIAL_ATTACK, // 11893

			// ── Melee: Osmumten's fang ─────────────────────────────────────
			AnimationID.HUMAN_OSMUMTENS_FANG,        // 9471

			// ── Melee: elder maul ──────────────────────────────────────────
			AnimationID.HUMAN_ELDER_MAUL_ATTACK,     // 7516
			AnimationID.HUMAN_ELDER_MAUL_SPEC,       // 11124

			// ── Specials: voidwaker ────────────────────────────────────────
			AnimationID.HUMAN_SPECIAL_VOIDWAKER,     // 11240
			AnimationID.HUMAN_SPECIAL02_VOIDWAKER,   // 11275

			// ── Specials: accursed sceptre ─────────────────────────────────
			AnimationID.HUMAN_SPECIAL_ACCURSED,      // 9961

			// ── Specials: webweaver bow ────────────────────────────────────
			// Craw's bow uses the generic HUMAN_BOW animation; no distinct
			// constant exists for Craw's. Dark bow likewise uses HUMAN_BOW.
			AnimationID.HUMAN_SPECIAL01_WEBWEAVER,   // 9964

			// ── Magic: Volatile / Eldritch nightmare staff casts & spec ────
			AnimationID.NIGHTMARE_STAFF_SPECIAL,              // 8532
			AnimationID.NIGHTMARE_STAFF_VOLATILE_CAST,        // 8546
			AnimationID.NIGHTMARE_STAFF_ELDRITCH_PLAYER_CAST  // 8548
		))
	);

	/**
	 * Non-attack animations that must never be mistaken for one. Retained so
	 * callers can still distinguish consuming / teleport / prayer transitions
	 * from "unknown" and "known attack" states.
	 */
	private static final Set<Integer> NON_ATTACK_IDS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(
			AnimationID.HUMAN_EAT,                    // 829 — eat / drink
			AnimationID.HUMAN_CASTLOWLVLALCHEMY,      // 712
			AnimationID.HUMAN_CASTHIGHLVLALCHEMY,     // 713
			AnimationID.HUMAN_CASTTELEPORT,           // 714
			AnimationID.HUMAN_CASTTELEPORT_REVERSE,   // 715
			AnimationID.HUMAN_PRAY,                   // 645
			AnimationID.HUMAN_DEATH                   // 836
		))
	);

	/**
	 * Returns true when {@code animId} is a known player attack animation.
	 * This is a positive confirmation — only IDs on the explicit allowlist
	 * return true. Unknown animations and non-attack animations return false.
	 */
	public static boolean isAttack(int animId)
	{
		if (animId <= 0) return false;
		return ATTACK_IDS.contains(animId);
	}

	/**
	 * Returns true when {@code animId} is a known non-attack animation
	 * (eat/drink, teleport, prayer, death). Retained for diagnostic use.
	 */
	public static boolean isNonAttack(int animId)
	{
		return NON_ATTACK_IDS.contains(animId);
	}
}
