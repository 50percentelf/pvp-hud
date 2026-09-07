package com.pvphud;

import com.google.inject.Provides;
import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.EffectState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
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
import net.runelite.api.InventoryID;
import java.util.List;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
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
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = "PvP HUD",
	description = "A coherent PvP combat dashboard in the chatbox region",
	tags = {"pvp", "hud", "combat", "wilderness"}
)
public class PvpHudPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PvpHudConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PvpHudOverlay overlay;

	@Inject
	private ItemManager itemManager;

	@Inject
	private KeyManager keyManager;

	@Getter
	private final PvpHudState hudState = new PvpHudState();

	private long prevHpXp;

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
		// All modes use BOTTOM_LEFT so the overlay framework translates the
		// graphics context correctly. CHAT_LOCKED pins the location each frame
		// via renderChatLocked; float modes let the user drag freely.
		overlay.setPosition(OverlayPosition.BOTTOM_LEFT);
	}

	private void initSelfState()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

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
		self.setPoisoned(poisonVal > 0);
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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("pvp-hud"))
		{
			return;
		}
		hudState.getContext().setMode(config.hudMode());
		hudState.getContext().setPvpActive(config.hudVisible());
		// Only touch overlay position when the layout setting itself changes —
		// other config toggles must not reset the user's dragged position.
		if ("hudLayout".equals(event.getKey()))
		{
			applyOverlayPosition();
		}
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
				// +1 increase that doesn't exceed max is the natural regen tick
				if (event.getBoostedLevel() == prevHp + 1
					&& event.getBoostedLevel() <= event.getLevel())
				{
					self.setHpRegenTicksRemaining(100);
				}

				// Outgoing damage from HP XP delta (all combat styles use same 4/3 HP XP rate)
				long newHpXp = event.getXp();
				if (prevHpXp > 0)
				{
					long xpDelta = newHpXp - prevHpXp;
					if (xpDelta > 0)
					{
						int damage = (int) Math.round(xpDelta * 3.0 / 4.0);
						if (damage > 0) handleOutgoingHit(damage);
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

	/**
	 * Called whenever a combat stat drains 1 point back toward base. Calibrates
	 * the drain period from the observed interval between consecutive drains.
	 */
	private void recordStatDrain(SelfState self)
	{
		int period    = self.getStatDrainPeriod();
		int remaining = self.getStatDrainTicksRemaining();
		if (period > 0)
		{
			int elapsed = period - remaining;
			// Only update estimate if the interval is plausible (10–120 ticks)
			if (elapsed >= 10 && elapsed <= 120)
			{
				self.setStatDrainPeriod(elapsed);
			}
		}
		else
		{
			// First drain observed — seed with a 40-tick (~24 s) estimate
			self.setStatDrainPeriod(40);
		}
		self.setStatDrainTicksRemaining(self.getStatDrainPeriod());
	}

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
			hudState.getEffects().setSpecEnergy(value / 10);
		}
		else if (varpId == VarPlayer.POISON)
		{
			hudState.getSelf().setVenomed(value >= 1_000_000);
			hudState.getSelf().setPoisoned(value > 0);
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		for (ActorSpotAnim sa : event.getActor().getSpotAnims())
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

	@Subscribe
	public void onGameTick(GameTick event)
	{
		SelfState self = hudState.getSelf();

		if (self.getFreezeTicksRemaining() > 0)
		{
			self.setFreezeTicksRemaining(self.getFreezeTicksRemaining() - 1);
			if (self.getFreezeTicksRemaining() == 0)
			{
				self.setFreezeSpriteId(0);
			}
		}

		if (self.getHpRegenTicksRemaining() > 0)
		{
			self.setHpRegenTicksRemaining(self.getHpRegenTicksRemaining() - 1);
		}

		if (self.getStatDrainTicksRemaining() > 0)
		{
			self.setStatDrainTicksRemaining(self.getStatDrainTicksRemaining() - 1);
		}

		ActionClockState clock = hudState.getActionClock();
		if (clock.getAttackDelayTicks() > 0)
			clock.setAttackDelayTicks(clock.getAttackDelayTicks() - 1);
		if (clock.getEatCooldownTicks() > 0)
			clock.setEatCooldownTicks(clock.getEatCooldownTicks() - 1);
		if (clock.getPotCooldownTicks() > 0)
			clock.setPotCooldownTicks(clock.getPotCooldownTicks() - 1);

		pollOpponentHealth();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		ActionClockState clock = hudState.getActionClock();
		switch (event.getMenuOption())
		{
			case "Eat":
				clock.setEatCooldownTicks(3);
				break;
			case "Drink":
				clock.setPotCooldownTicks(3);
				break;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer()) return;
		int anim = client.getLocalPlayer().getAnimation();
		if (anim == -1) return;
		if (client.getLocalPlayer().getInteracting() == null) return;
		hudState.getActionClock().setAttackDelayTicks(getWeaponSpeed());
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer()) return;
		Actor target = event.getTarget();
		if (target instanceof Player)
		{
			String name = ((Player) target).getName();
			OpponentState opp = hudState.getOpponent();
			if (name != null && !name.equals(opp.getName()))
			{
				opp.reset();
				opp.setName(name);
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			int dmg = event.getHitsplat().getAmount();
			if (dmg > 0)
			{
				hudState.getCombatEvent().post(
					new CombatEvent(CombatEventType.INCOMING_HIT, dmg, 0), 2500);
			}
		}
	}

	private void handleOutgoingHit(int damage)
	{
		OpponentState opp = hudState.getOpponent();
		opp.setLastOutgoingHit(damage);
		hudState.getCombatEvent().post(
			new CombatEvent(CombatEventType.OUTGOING_HIT, damage, 0), 2500);
		if (opp.getEstimatedHp() > 0)
			opp.setEstimatedHp(Math.max(0, opp.getEstimatedHp() - damage));
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
			int ratio = p.getHealthRatio();
			int scale = p.getHealthScale();
			if (ratio < 0 || scale <= 0) break;

			// If opponent was just acquired at full health, use XP damage to derive max HP.
			// Otherwise use health-bar % + accumulated damage to refine the estimate.
			if (opp.getMaxHp() <= 0 && ratio == scale)
			{
				// Full health — can't estimate max yet; wait for first damage observation.
				break;
			}
			if (opp.getMaxHp() <= 0 && opp.getLastOutgoingHit() > 0)
			{
				// We dealt damage; health bar now shows < 100 %. Back-calculate max HP.
				double pct = (double) ratio / scale;
				// totalDamage ≈ maxHp * (1 - pct)
				// Use last outgoing hit as proxy for total damage if estimatedHp not set.
				// A more accurate approach accumulates multiple hits.
				break;
			}
			if (opp.getMaxHp() > 0)
			{
				opp.setEstimatedHp((int) Math.round(opp.getMaxHp() * (double) ratio / scale));
			}
			break;
		}
	}

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
