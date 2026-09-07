package com.pvphud;

import com.google.inject.Provides;
import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.EffectState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.PrayerEffect;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.SelfState;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.GraphicID;
import net.runelite.api.HeadIcon;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.HotkeyListener;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "PvP HUD",
	description = "A coherent PvP combat dashboard in the chatbox region",
	tags = {"pvp", "hud", "combat", "wilderness"}
)
public class PvpHudPlugin extends Plugin
{
	@Inject private Client       client;
	@Inject private PvpHudConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private PvpHudOverlay  overlay;
	@Inject private ItemManager    itemManager;
	@Inject private KeyManager     keyManager;

	@Getter
	private final PvpHudState hudState = new PvpHudState();

	/** Previous HP XP value — used only to drive the pending-hit animation. */
	private long prevHpXp;

	/**
	 * Vengeance cast spot-anim. More reliable than animation ID because the cast
	 * animation varies with equipped weapon; this graphic is consistent.
	 */
	private static final int SPOTANIM_VENGEANCE = 725;

	/**
	 * Candidate animation IDs for Vengeance cast (Lunar spellbook). Varies by
	 * weapon type; kept as fallback alongside graphic-based detection.
	 */
	private static final int[] ANIM_VENGEANCE_IDS = {4410, 4411, 4671, 4072, 4071};

	private final HotkeyListener timer1Listener = new HotkeyListener(() -> config.timer1Key())
	{
		@Override
		public void hotkeyPressed()
		{
			ManualTimerState t = hudState.getTimer1();
			if (t.isRunning()) t.clear();
			else t.start(config.timer1Duration() * 1000L);
		}
	};

	private final HotkeyListener timer2Listener = new HotkeyListener(() -> config.timer2Key())
	{
		@Override
		public void hotkeyPressed()
		{
			ManualTimerState t = hudState.getTimer2();
			if (t.isRunning()) t.clear();
			else t.start(config.timer2Duration() * 1000L);
		}
	};

	// ── Plugin lifecycle ─────────────────────────────────────────────────────

	@Override
	protected void startUp() throws Exception
	{
		hudState.fullReset();
		hudState.getContext().setMode(config.hudMode());
		hudState.getContext().setPvpActive(config.hudVisible());
		initSelfState();
		applyOverlayPosition();
		overlay.loadIcons();
		overlayManager.add(overlay);
		keyManager.registerKeyListener(timer1Listener);
		keyManager.registerKeyListener(timer2Listener);
		log.info("PvP HUD started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(timer1Listener);
		keyManager.unregisterKeyListener(timer2Listener);
		overlayManager.remove(overlay);
		hudState.fullReset();
		log.info("PvP HUD stopped");
	}

	private void applyOverlayPosition()
	{
		overlay.setPosition(OverlayPosition.BOTTOM_LEFT);
	}

	// ── Self state initialisation ─────────────────────────────────────────────

	private void initSelfState()
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;

		BoostState boosts = hudState.getBoosts();
		boosts.setAttackReal(client.getRealSkillLevel(Skill.ATTACK));
		boosts.setAttackBoosted(client.getBoostedSkillLevel(Skill.ATTACK));
		boosts.setStrengthReal(client.getRealSkillLevel(Skill.STRENGTH));
		boosts.setStrengthBoosted(client.getBoostedSkillLevel(Skill.STRENGTH));
		boosts.setDefenceReal(client.getRealSkillLevel(Skill.DEFENCE));
		boosts.setDefenceBoosted(client.getBoostedSkillLevel(Skill.DEFENCE));
		boosts.setRangedReal(client.getRealSkillLevel(Skill.RANGED));
		boosts.setRangedBoosted(client.getBoostedSkillLevel(Skill.RANGED));
		boosts.setMagicReal(client.getRealSkillLevel(Skill.MAGIC));
		boosts.setMagicBoosted(client.getBoostedSkillLevel(Skill.MAGIC));

		hudState.getEffects().setSpecEnergy(
			client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10);

		int poisonVal = client.getVarpValue(VarPlayer.POISON);
		SelfState self = hudState.getSelf();
		self.setVenomed(poisonVal >= 1_000_000);
		self.setPoisoned(poisonVal > 0 && poisonVal < 1_000_000);
		self.setAntiVenomActive(poisonVal <= -500_000);
		self.setAntiPoisonActive(poisonVal < 0 && poisonVal > -500_000);
		self.setCurrentHp(client.getBoostedSkillLevel(Skill.HITPOINTS));
		self.setMaxHp(client.getRealSkillLevel(Skill.HITPOINTS));
		self.setCurrentPrayer(client.getBoostedSkillLevel(Skill.PRAYER));
		self.setMaxPrayer(client.getRealSkillLevel(Skill.PRAYER));
		self.setVengActive(client.getVarbitValue(Varbits.VENGEANCE_ACTIVE) == 1);
		self.setTeleBlockTicksRemaining(client.getVarbitValue(Varbits.TELEBLOCK));

		EffectState fx = hudState.getEffects();
		fx.setDivineSupercombatTicks(client.getVarbitValue(Varbits.DIVINE_SUPER_COMBAT));
		fx.setDivineRangingTicks(client.getVarbitValue(Varbits.DIVINE_RANGING));
		fx.setDivineMagicTicks(client.getVarbitValue(Varbits.DIVINE_MAGIC));
		fx.setDivineBastionTicks(client.getVarbitValue(Varbits.DIVINE_BASTION));
		fx.setDivineBattlemageTicks(client.getVarbitValue(Varbits.DIVINE_BATTLEMAGE));
		fx.setMenaphiteRemedyTicks(client.getVarbitValue(Varbits.MENAPHITE_REMEDY));

		prevHpXp = client.getSkillExperience(Skill.HITPOINTS);
	}

	// ── Config / game-state events ────────────────────────────────────────────

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("pvp-hud")) return;
		hudState.getContext().setMode(config.hudMode());
		hudState.getContext().setPvpActive(config.hudVisible());
		if ("hudLayout".equals(event.getKey())) applyOverlayPosition();
		hudState.getLayout().markDirty();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN
			|| event.getGameState() == GameState.HOPPING)
		{
			hudState.fullReset();
		}
	}

	// ── Stat changes ──────────────────────────────────────────────────────────

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		BoostState boosts = hudState.getBoosts();
		SelfState  self   = hudState.getSelf();
		switch (event.getSkill())
		{
			case ATTACK:
			{
				int prev = boosts.getAttackBoosted();
				boosts.setAttackReal(event.getLevel());
				boosts.setAttackBoosted(event.getBoostedLevel());
				if (event.getBoostedLevel() < prev && event.getBoostedLevel() >= event.getLevel())
					recordStatDrain(self);
				break;
			}
			case STRENGTH:
			{
				int prev = boosts.getStrengthBoosted();
				boosts.setStrengthReal(event.getLevel());
				boosts.setStrengthBoosted(event.getBoostedLevel());
				if (event.getBoostedLevel() < prev && event.getBoostedLevel() >= event.getLevel())
					recordStatDrain(self);
				break;
			}
			case DEFENCE:
			{
				int prev = boosts.getDefenceBoosted();
				boosts.setDefenceReal(event.getLevel());
				boosts.setDefenceBoosted(event.getBoostedLevel());
				if (event.getBoostedLevel() < prev && event.getBoostedLevel() >= event.getLevel())
					recordStatDrain(self);
				break;
			}
			case RANGED:
			{
				int prev = boosts.getRangedBoosted();
				boosts.setRangedReal(event.getLevel());
				boosts.setRangedBoosted(event.getBoostedLevel());
				if (event.getBoostedLevel() < prev && event.getBoostedLevel() >= event.getLevel())
					recordStatDrain(self);
				break;
			}
			case MAGIC:
			{
				int prev = boosts.getMagicBoosted();
				boosts.setMagicReal(event.getLevel());
				boosts.setMagicBoosted(event.getBoostedLevel());
				if (event.getBoostedLevel() < prev && event.getBoostedLevel() >= event.getLevel())
					recordStatDrain(self);
				break;
			}
			case HITPOINTS:
			{
				int prevHp = self.getCurrentHp();
				self.setCurrentHp(event.getBoostedLevel());
				self.setMaxHp(event.getLevel());
				if (event.getBoostedLevel() == prevHp + 1
					&& event.getBoostedLevel() <= event.getLevel())
				{
					self.setHpRegenTicksRemaining(100);
				}

				// HP XP delta drives the pending-hit animation only; the actual
				// combat log entry comes from onHitsplatApplied on the opponent.
				long newHpXp = event.getXp();
				if (prevHpXp > 0)
				{
					long xpDelta = newHpXp - prevHpXp;
					if (xpDelta > 0)
					{
						int damage = (int) Math.round(xpDelta * 3.0 / 4.0);
						OpponentState opp = hudState.getOpponent();
						if (damage > 0 && opp.isTracked())
							opp.setPendingHit(damage);
					}
				}
				prevHpXp = newHpXp;
				break;
			}
			case PRAYER:
				self.setCurrentPrayer(event.getBoostedLevel());
				self.setMaxPrayer(event.getLevel());
				break;
			default:
				break;
		}
	}

	private void recordStatDrain(SelfState self)
	{
		int period    = self.getStatDrainPeriod();
		int remaining = self.getStatDrainTicksRemaining();
		if (period > 0)
		{
			int elapsed = period - remaining;
			if (elapsed >= 10 && elapsed <= 120) self.setStatDrainPeriod(elapsed);
		}
		else
		{
			self.setStatDrainPeriod(40);
		}
		self.setStatDrainTicksRemaining(self.getStatDrainPeriod());
	}

	// ── Varbit / VarPlayer changes ────────────────────────────────────────────

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbitId = event.getVarbitId();
		int varpId   = event.getVarpId();
		int value    = event.getValue();

		if (varbitId == Varbits.VENGEANCE_ACTIVE)
		{
			hudState.getSelf().setVengActive(value == 1);
		}
		else if (varbitId == Varbits.TELEBLOCK)
		{
			hudState.getSelf().setTeleBlockTicksRemaining(value);
		}
		else if (varbitId == Varbits.DIVINE_SUPER_COMBAT)
		{
			hudState.getEffects().setDivineSupercombatTicks(value);
		}
		else if (varbitId == Varbits.DIVINE_RANGING)
		{
			hudState.getEffects().setDivineRangingTicks(value);
		}
		else if (varbitId == Varbits.DIVINE_MAGIC)
		{
			hudState.getEffects().setDivineMagicTicks(value);
		}
		else if (varbitId == Varbits.DIVINE_BASTION)
		{
			hudState.getEffects().setDivineBastionTicks(value);
		}
		else if (varbitId == Varbits.DIVINE_BATTLEMAGE)
		{
			hudState.getEffects().setDivineBattlemageTicks(value);
		}
		else if (varbitId == Varbits.MENAPHITE_REMEDY)
		{
			hudState.getEffects().setMenaphiteRemedyTicks(value);
		}
		else if (varpId == VarPlayer.SPECIAL_ATTACK_PERCENT)
		{
			int newSpec = value / 10;
			int oldSpec = hudState.getEffects().getSpecEnergy();
			hudState.getEffects().setSpecEnergy(newSpec);
			// Record regen timestamp when spec increases below full (passive regen, not at cap)
			if (newSpec > oldSpec && newSpec < 100)
				hudState.getEffects().setLastSpecRegenMs(System.currentTimeMillis());
		}
		else if (varpId == VarPlayer.POISON)
		{
			SelfState self = hudState.getSelf();
			self.setVenomed(value >= 1_000_000);
			self.setPoisoned(value > 0 && value < 1_000_000);
			// Negative POISON var = active anti-poison/anti-venom protection.
			// Values <= -500,000 indicate anti-venom potions (antidote++/anti-venom).
			self.setAntiVenomActive(value <= -500_000);
			self.setAntiPoisonActive(value < 0 && value > -500_000);
		}
	}

	// ── Self freeze detection ─────────────────────────────────────────────────

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Actor actor = event.getActor();

		if (actor == client.getLocalPlayer())
		{
			for (ActorSpotAnim sa : actor.getSpotAnims())
			{
				int ticks = freezeTicksForGraphic(sa.getId());
				if (ticks > 0)
				{
					hudState.getSelf().setFreezeTicksRemaining(ticks);
					hudState.getSelf().setFreezeSpriteId(freezeSpriteIdForGraphic(sa.getId()));
					return;
				}
			}
		}
		else if (actor instanceof Player)
		{
			// Detect opponent Vengeance cast via its spot-anim (reliable across weapon types).
			// Opponent freeze is NOT tracked — rejected by RuneLite review policy.
			OpponentState opp = hudState.getOpponent();
			if (!opp.isTracked() || actor.getName() == null
				|| !actor.getName().equalsIgnoreCase(opp.getName()))
			{
				return;
			}
			for (ActorSpotAnim sa : actor.getSpotAnims())
			{
				if (sa.getId() == SPOTANIM_VENGEANCE)
				{
					opp.setVengActive(true);
					return;
				}
			}
		}
	}

	// ── Game tick ─────────────────────────────────────────────────────────────

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();

		SelfState self = hudState.getSelf();
		if (self.getFreezeTicksRemaining() > 0)
		{
			self.setFreezeTicksRemaining(self.getFreezeTicksRemaining() - 1);
			if (self.getFreezeTicksRemaining() == 0) self.setFreezeSpriteId(0);
		}
		if (self.getHpRegenTicksRemaining() > 0)
			self.setHpRegenTicksRemaining(self.getHpRegenTicksRemaining() - 1);
		if (self.getStatDrainTicksRemaining() > 0)
			self.setStatDrainTicksRemaining(self.getStatDrainTicksRemaining() - 1);

		ActionClockState clock = hudState.getActionClock();
		if (clock.getAttackDelayTicks() > 0) clock.setAttackDelayTicks(clock.getAttackDelayTicks() - 1);
		if (clock.getEatCooldownTicks() > 0) clock.setEatCooldownTicks(clock.getEatCooldownTicks() - 1);
		if (clock.getPotCooldownTicks() > 0) clock.setPotCooldownTicks(clock.getPotCooldownTicks() - 1);

		// Expire stale fight session (no combat for ~30 s)
		PvpFightSession session = hudState.getCurrentSession();
		if (session != null && session.isStale(tick)) hudState.endSession();

		updateEnvironment();
		pollOpponentHealth();
	}

	// ── Menu clicks ───────────────────────────────────────────────────────────

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		ActionClockState clock = hudState.getActionClock();
		switch (event.getMenuOption())
		{
			case "Eat":   clock.setEatCooldownTicks(3); break;
			case "Drink": clock.setPotCooldownTicks(3); break;
		}
	}

	// ── Animation — attack delay and opponent veng detection ──────────────────

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer())
		{
			int anim = client.getLocalPlayer().getAnimation();
			if (anim != -1 && client.getLocalPlayer().getInteracting() != null)
				hudState.getActionClock().setAttackDelayTicks(getWeaponSpeed());
		}
		else if (actor instanceof Player)
		{
			OpponentState opp = hudState.getOpponent();
			if (!opp.isTracked() || actor.getName() == null
				|| !actor.getName().equalsIgnoreCase(opp.getName()))
			{
				return;
			}
			int anim = actor.getAnimation();
			for (int id : ANIM_VENGEANCE_IDS)
			{
				if (anim == id)
				{
					opp.setVengActive(true);
					break;
				}
			}
		}
	}

	// ── Interacting changed — fight session lifecycle ─────────────────────────

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer()) return;
		Actor target = event.getTarget();

		if (target instanceof Player)
		{
			String name = ((Player) target).getName();
			if (name == null) return;

			PvpFightSession session = hudState.getCurrentSession();

			// Re-targeting the same opponent (e.g. after a brief null) — keep session alive
			if (session != null && name.equals(session.getOpponentName()))
			{
				hudState.getOpponent().setName(name);
				return;
			}

			// Genuinely new opponent — start a fresh session
			hudState.getOpponent().reset();
			hudState.getOpponent().setName(name);
			hudState.beginSession(name, client.getTickCount());
		}
		// Null target: do nothing — session survives brief disengagement
	}

	// ── Hitsplat events — combat log and HP estimation ────────────────────────

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();

		if (actor == client.getLocalPlayer())
		{
			int dmg = event.getHitsplat().getAmount();
			if (dmg <= 0) return;

			hudState.getSelf().setLastIncomingDamageMs(System.currentTimeMillis());

			PvpFightSession session = hudState.getCurrentSession();
			if (session == null) return;

			// Determine prayer impact from opponent's active overhead
			PrayerEffect effect   = PrayerEffect.NONE;
			int prayerDrain       = 0;
			OpponentState opp     = hudState.getOpponent();
			if (opp.isSmiteActive())
			{
				effect      = PrayerEffect.SMITE;
				prayerDrain = (dmg + 3) / 4; // ceil(dmg/4)
			}

			session.onIncomingHit(
				CombatEvent.incoming(dmg, prayerDrain, 0, effect, System.currentTimeMillis()),
				client.getTickCount());
		}
		else if (actor instanceof Player)
		{
			// Outgoing hit: hitsplat on the tracked opponent
			OpponentState opp = hudState.getOpponent();
			if (!opp.isTracked()
				|| actor.getName() == null
				|| !actor.getName().equalsIgnoreCase(opp.getName()))
			{
				return;
			}

			int dmg = event.getHitsplat().getAmount();
			if (dmg <= 0) return;

			handleOutgoingHit(opp, dmg);
		}
	}

	private void handleOutgoingHit(OpponentState opp, int damage)
	{
		opp.setLastOutgoingHit(damage);
		// Consume opponent Vengeance — they had active veng, this hit triggers it
		if (opp.isVengActive()) opp.setVengActive(false);

		if (opp.getEstimatedHp() > 0)
			opp.setEstimatedHp(Math.max(0, opp.getEstimatedHp() - damage));

		PvpFightSession session = hudState.getCurrentSession();
		if (session == null) return;

		session.onOutgoingHit(
			CombatEvent.outgoing(damage, System.currentTimeMillis()),
			client.getTickCount());
	}

	// ── Environment polling ───────────────────────────────────────────────────

	private void updateEnvironment()
	{
		int wildLevel = 0;
		if (client.getVarbitValue(Varbits.IN_WILDERNESS) == 1)
		{
			Player local = client.getLocalPlayer();
			if (local != null)
			{
				WorldPoint loc = local.getWorldLocation();
				if (loc.getPlane() == 0)
				{
					wildLevel = (loc.getY() - 3520) / 8 + 1;
					wildLevel = Math.max(1, Math.min(56, wildLevel));
				}
			}
		}
		hudState.getContext().setWildernessLevel(wildLevel);
		hudState.getContext().setMultiCombat(client.getVarbitValue(Varbits.MULTICOMBAT_AREA) == 1);
	}

	private void pollOpponentHealth()
	{
		OpponentState opp = hudState.getOpponent();
		if (!opp.isTracked()) return;

		List<Player> players = client.getPlayers();
		if (players == null) return;

		for (Player p : players)
		{
			if (p == null || !p.getName().equals(opp.getName())) continue;

			opp.setOverheadPrayer(p.getOverheadIcon());

			int ratio = p.getHealthRatio();
			int scale = p.getHealthScale();
			if (ratio < 0 || scale <= 0) break;

			// Terminate fight session when opponent HP reaches 0
			if (ratio == 0)
			{
				hudState.endSession();
				break;
			}

			PvpFightSession session = hudState.getCurrentSession();
			int totalDealt = session != null ? session.getTotalOutgoing() : 0;

			if (opp.getMaxHp() <= 0)
			{
				if (ratio < scale && totalDealt > 0)
				{
					double missingFraction = 1.0 - (double) ratio / scale;
					if (missingFraction > 0.01)
					{
						int estimated = (int) Math.round(totalDealt / missingFraction);
						if (estimated >= 60 && estimated <= 200)
						{
							opp.setMaxHp(estimated);
							opp.setEstimatedHp((int) Math.round(estimated * (double) ratio / scale));
						}
					}
				}
			}
			else
			{
				opp.setEstimatedHp((int) Math.round(opp.getMaxHp() * (double) ratio / scale));
			}
			break;
		}
	}

	// ── Weapon / freeze helpers ───────────────────────────────────────────────

	private int getWeaponSpeed()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) return 4;
		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null) return 4;
		ItemStats stats = itemManager.getItemStats(weapon.getId(), false);
		if (stats == null) return 4;
		ItemEquipmentStats eq = stats.getEquipment();
		if (eq == null) return 4;
		int speed = eq.getAspeed();
		return speed > 0 ? speed : 4;
	}

	private static int freezeTicksForGraphic(int graphicId)
	{
		switch (graphicId)
		{
			case GraphicID.ICE_RUSH:    return 8;
			case GraphicID.ICE_BURST:   return 16;
			case GraphicID.ICE_BLITZ:   return 24;
			case GraphicID.ICE_BARRAGE: return 32;
			default:                    return 0;
		}
	}

	private static int freezeSpriteIdForGraphic(int graphicId)
	{
		switch (graphicId)
		{
			case GraphicID.ICE_RUSH:    return SpriteID.SPELL_ICE_RUSH;
			case GraphicID.ICE_BURST:   return SpriteID.SPELL_ICE_BURST;
			case GraphicID.ICE_BLITZ:   return SpriteID.SPELL_ICE_BLITZ;
			case GraphicID.ICE_BARRAGE: return SpriteID.SPELL_ICE_BARRAGE;
			default:                    return SpriteID.SPELL_ICE_BARRAGE;
		}
	}

	@Provides
	PvpHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpHudConfig.class);
	}
}
