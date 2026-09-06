package com.pvphud;

import com.google.inject.Provides;
import com.pvphud.state.BoostState;
import com.pvphud.state.EffectState;
import com.pvphud.state.SelfState;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicID;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

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

	@Getter
	private final PvpHudState hudState = new PvpHudState();

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
		log.info("PvP HUD started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		hudState.fullReset();
		log.info("PvP HUD stopped");
	}

	private void applyOverlayPosition()
	{
		switch (config.hudLayout())
		{
			case CHAT_LOCKED:
				overlay.setPosition(OverlayPosition.DYNAMIC);
				break;
			case HORIZONTAL_FLOAT:
			case VERTICAL_FLOAT:
				overlay.setPosition(OverlayPosition.BOTTOM_LEFT);
				break;
		}
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
		self.setVengActive(client.getVarbitValue(Varbits.VENGEANCE_ACTIVE) == 1);
		self.setTeleBlockTicksRemaining(client.getVarbitValue(Varbits.TELEBLOCK));

		EffectState fx = hudState.getEffects();
		fx.setDivineSupercombatTicks(client.getVarbitValue(Varbits.DIVINE_SUPER_COMBAT));
		fx.setDivineRangingTicks(client.getVarbitValue(Varbits.DIVINE_RANGING));
		fx.setDivineMagicTicks(client.getVarbitValue(Varbits.DIVINE_MAGIC));
		fx.setDivineBastionTicks(client.getVarbitValue(Varbits.DIVINE_BASTION));
		fx.setDivineBattlemageTicks(client.getVarbitValue(Varbits.DIVINE_BATTLEMAGE));
		fx.setMenaphiteRemedyTicks(client.getVarbitValue(Varbits.MENAPHITE_REMEDY));
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
		applyOverlayPosition();
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
		switch (event.getSkill())
		{
			case ATTACK:
				boosts.setAttackReal(event.getLevel());
				boosts.setAttackBoosted(event.getBoostedLevel());
				break;
			case STRENGTH:
				boosts.setStrengthReal(event.getLevel());
				boosts.setStrengthBoosted(event.getBoostedLevel());
				break;
			case DEFENCE:
				boosts.setDefenceReal(event.getLevel());
				boosts.setDefenceBoosted(event.getBoostedLevel());
				break;
			case RANGED:
				boosts.setRangedReal(event.getLevel());
				boosts.setRangedBoosted(event.getBoostedLevel());
				break;
			case MAGIC:
				boosts.setMagicReal(event.getLevel());
				boosts.setMagicBoosted(event.getBoostedLevel());
				break;
			default:
				break;
		}
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
