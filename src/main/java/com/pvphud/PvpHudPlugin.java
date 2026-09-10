package com.pvphud;

import com.google.inject.Provides;
import com.pvphud.state.ActionClockState;
import com.pvphud.state.BoostState;
import com.pvphud.state.CombatEvent;
import com.pvphud.state.CombatEventType;
import com.pvphud.state.EffectState;
import com.pvphud.state.ManualTimerState;
import com.pvphud.state.OpponentState;
import com.pvphud.state.PeriodicEffect;
import com.pvphud.state.PoisonState;
import com.pvphud.state.PrayerEffect;
import com.pvphud.state.ProtectionState;
import com.pvphud.state.PvpFightSession;
import com.pvphud.state.SelfState;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.GraphicID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.HeadIcon;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.CanvasSizeChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ResizeableChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

@Slf4j
@PluginDescriptor(
	name = "PvP HUD",
	description = "A coherent PvP combat dashboard in the chatbox region",
	tags = {"pvp", "hud", "combat", "wilderness"}
)
public class PvpHudPlugin extends Plugin
{
	@Inject private Client         client;
	@Inject private PvpHudConfig   config;
	@Inject private ConfigManager  configManager;
	@Inject private OverlayManager overlayManager;
	@Inject private PvpHudOverlay  overlay;
	@Inject private ItemManager    itemManager;
	@Inject private KeyManager     keyManager;
	@Inject private HiscoreClient  hiscoreClient;
	@Inject private ClientThread   clientThread;

	@Getter
	private final PvpHudState hudState = new PvpHudState();

	/** Names already queued for a hiscores lookup this session (case-lowered). */
	private final Set<String> lookedUp = new HashSet<>();

	/**
	 * Candidate opponent waiting for combat confirmation.
	 * Set by InteractingChanged; cleared when confirmed into a session or when
	 * the local player retargets a non-player. A session is only started when
	 * one of: Attack menu option used, outgoing hitsplat on this candidate, or
	 * incoming hitsplat while this candidate is interacting with the local player.
	 */
	private String pendingOpponentName  = null;
	private Player pendingOpponentActor = null;

	/**
	 * Most recent freeze-spell graphic seen on the local player this session.
	 * Set in onGraphicChanged, consumed in onChatMessage when "You have been frozen!"
	 * arrives on the same tick so the chat-message handler can identify which spell.
	 */
	private int lastFreezeGraphicId   = -1;
	private int lastFreezeGraphicTick = -1;

	/** Last known Menaphite Remedy varbit value; -1 = not yet observed. */
	private int prevMenaphiteVarbit = -1;

	/** Player position from the previous game tick; used to detect movement while frozen. */
	private WorldPoint lastPlayerPos = null;

	// SPOTANIM_VENGEANCE and ANIM_VENGEANCE_IDS removed — Vengeance tracking deferred post-v0.1.

	private final HotkeyListener hudToggleListener = new HotkeyListener(() -> config.hudToggleKey())
	{
		@Override
		public void hotkeyPressed()
		{
			configManager.setConfiguration("pvp-hud", "hudVisible", !config.hudVisible());
		}
	};

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
		lookedUp.clear();
		pendingOpponentName   = null;
		pendingOpponentActor  = null;
		prevMenaphiteVarbit   = -1;
		lastFreezeGraphicId   = -1;
		lastFreezeGraphicTick = -1;
		lastPlayerPos         = null;
		hudState.fullReset();
		applyOverlayPosition();
		overlay.invalidateInventoryHugAnchor();
		overlay.loadIcons();
		overlayManager.add(overlay);
		keyManager.registerKeyListener(hudToggleListener);
		keyManager.registerKeyListener(timer1Listener);
		keyManager.registerKeyListener(timer2Listener);
		clientThread.invoke(this::rehydrateFromClient);
		log.info("PvP HUD started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(hudToggleListener);
		keyManager.unregisterKeyListener(timer1Listener);
		keyManager.unregisterKeyListener(timer2Listener);
		overlayManager.remove(overlay);
		hudState.fullReset();
		log.info("PvP HUD stopped");
	}

	private void applyOverlayPosition()
	{
		// DYNAMIC overlays are excluded from RuneLite's Alt+drag overlay editor.
		// Float layouts need a non-DYNAMIC position so the user can drag them.
		switch (config.hudLayout())
		{
			case HORIZONTAL_FLOAT:
			case VERTICAL_FLOAT:
				overlay.setPosition(OverlayPosition.TOP_LEFT);
				break;
			default:
				overlay.setPosition(OverlayPosition.DYNAMIC);
				break;
		}
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
		SelfState   self  = hudState.getSelf();
		EffectState efxI  = hudState.getEffects();
		applyPoisonVarp(poisonVal);
		self.setCurrentHp(client.getBoostedSkillLevel(Skill.HITPOINTS));
		self.setMaxHp(client.getRealSkillLevel(Skill.HITPOINTS));
		self.setCurrentPrayer(client.getBoostedSkillLevel(Skill.PRAYER));
		self.setMaxPrayer(client.getRealSkillLevel(Skill.PRAYER));
		// client.getEnergy() returns 0-10000; divide by 100 for 0-100%
		self.setRunEnergy(client.getEnergy() / 100);
		// Vengeance tracking deferred post-v0.1 — do not initialise vengActive.
		self.setTeleBlockTicksRemaining(decodeTeleblockTicks(client.getVarbitValue(Varbits.TELEBLOCK)));

		EffectState fx   = hudState.getEffects();
		int         tick = client.getTickCount();
		fx.setDivineSupercombatTicks(client.getVarbitValue(Varbits.DIVINE_SUPER_COMBAT));
		fx.setDivineRangingTicks(client.getVarbitValue(Varbits.DIVINE_RANGING));
		fx.setDivineMagicTicks(client.getVarbitValue(Varbits.DIVINE_MAGIC));
		fx.setDivineBastionTicks(client.getVarbitValue(Varbits.DIVINE_BASTION));
		fx.setDivineBattlemageTicks(client.getVarbitValue(Varbits.DIVINE_BATTLEMAGE));
		int menVal = client.getVarbitValue(Varbits.MENAPHITE_REMEDY);
		fx.getMenaphite().setTotalTicksRemaining(menVal * 25);
		prevMenaphiteVarbit = menVal;
		// nextProcTicks stays 0 — we don't know where in the 25-tick cycle we joined.

		// Antifire: if already active, assume the next boundary is one full interval away.
		// The varbit handler will correct the phase on the first observed decrement.
		int antifireVal = client.getVarbitValue(Varbits.ANTIFIRE);
		if (antifireVal > 0)
		{
			fx.setNextAntifireTick(tick + 30);
			fx.setAntifireVarbitValue(antifireVal);
		}
		int superAntifireVal = client.getVarbitValue(Varbits.SUPER_ANTIFIRE);
		if (superAntifireVal > 0)
		{
			fx.setNextSuperAntifireTick(tick + 20);
			fx.setSuperAntifireVarbitValue(superAntifireVal);
		}
		// Stamina effect is self-tracked (STAMINA_EFFECT is binary, not a countdown).
		// At startup we can't know remaining time, so we don't initialize the timer.
	}

	private void rehydrateFromClient()
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		updateEnvironment();
		initSelfState();
		hudState.getLayout().markDirty();
		overlay.invalidateInventoryHugAnchor();
	}

	// ── Config / game-state events ────────────────────────────────────────────

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("pvp-hud")) return;
		if ("hudLayout".equals(event.getKey()))
		{
			applyOverlayPosition();
			overlay.invalidateInventoryHugAnchor();
		}
		hudState.getLayout().markDirty();
	}

	@Subscribe
	public void onCanvasSizeChanged(CanvasSizeChanged event)
	{
		overlay.onGameframeChanged();
	}

	@Subscribe
	public void onResizeableChanged(ResizeableChanged event)
	{
		overlay.onGameframeChanged();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Invalidate when any top-level gameframe interface is (re)loaded.
		// Fixed viewport (548), resizable stretch (161), resizable pre-EOC (164).
		int g = event.getGroupId();
		if (g == InterfaceID.TOPLEVEL
			|| g == InterfaceID.TOPLEVEL_OSRS_STRETCH
			|| g == InterfaceID.TOPLEVEL_PRE_EOC)
		{
			overlay.onGameframeChanged();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN
			|| event.getGameState() == GameState.HOPPING)
		{
			lookedUp.clear();
			pendingOpponentName  = null;
			pendingOpponentActor = null;
			prevMenaphiteVarbit  = -1;
			lastPlayerPos        = null;
			hudState.fullReset();
			overlay.invalidateInventoryHugAnchor();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			rehydrateFromClient();
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
				int prev    = boosts.getAttackBoosted();
				int current = event.getBoostedLevel();
				int base    = event.getLevel();
				boosts.setAttackReal(base);
				boosts.setAttackBoosted(current);
				handleStatCycle(self, prev, current, base);
				break;
			}
			case STRENGTH:
			{
				int prev    = boosts.getStrengthBoosted();
				int current = event.getBoostedLevel();
				int base    = event.getLevel();
				boosts.setStrengthReal(base);
				boosts.setStrengthBoosted(current);
				handleStatCycle(self, prev, current, base);
				break;
			}
			case DEFENCE:
			{
				int prev    = boosts.getDefenceBoosted();
				int current = event.getBoostedLevel();
				int base    = event.getLevel();
				boosts.setDefenceReal(base);
				boosts.setDefenceBoosted(current);
				handleStatCycle(self, prev, current, base);
				break;
			}
			case RANGED:
			{
				int prev    = boosts.getRangedBoosted();
				int current = event.getBoostedLevel();
				int base    = event.getLevel();
				boosts.setRangedReal(base);
				boosts.setRangedBoosted(current);
				handleStatCycle(self, prev, current, base);
				break;
			}
			case MAGIC:
			{
				int prev    = boosts.getMagicBoosted();
				int current = event.getBoostedLevel();
				int base    = event.getLevel();
				boosts.setMagicReal(base);
				boosts.setMagicBoosted(current);
				handleStatCycle(self, prev, current, base);
				break;
			}
			case HITPOINTS:
			{
				int prevHp = self.getCurrentHp();
				self.setCurrentHp(event.getBoostedLevel());
				self.setMaxHp(event.getLevel());
				if (event.getBoostedLevel() == 0 && prevHp > 0)
				{
					// Local player died — terminate the fight immediately.
					hudState.endSession();
					hudState.getOpponent().reset();
					pendingOpponentName  = null;
					pendingOpponentActor = null;
				}
				else if (event.getBoostedLevel() == prevHp + 1
					&& event.getBoostedLevel() <= event.getLevel())
				{
					self.setHpRegenTicksRemaining(100);
				}
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

	private void handleStatCycle(SelfState self, int prev, int current, int base)
	{
		if (isNaturalBoostDecay(prev, current, base))
			self.getBoostDecay().sync(client.getTickCount());
		else if (isNaturalDebuffRestore(prev, current, base))
			self.getDebuffRestore().sync();
	}

	/** Natural boost decay: exactly -1 step, landing at or above base. */
	static boolean isNaturalBoostDecay(int prev, int current, int base)
	{
		return current == prev - 1 && current >= base;
	}

	/** Natural debuff restoration: exactly +1 step, landing at or below base. */
	static boolean isNaturalDebuffRestore(int prev, int current, int base)
	{
		return current == prev + 1 && current <= base;
	}

	// ── Varbit / VarPlayer changes ────────────────────────────────────────────

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbitId = event.getVarbitId();
		int varpId   = event.getVarpId();
		int value    = event.getValue();

		if (varbitId == Varbits.TELEBLOCK)
		{
			hudState.getSelf().setTeleBlockTicksRemaining(decodeTeleblockTicks(value));
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
			PeriodicEffect men = hudState.getEffects().getMenaphite();
			if (value == 0)
			{
				men.setTotalTicksRemaining(0);
				men.setNextProcTicks(0);
			}
			else
			{
				men.setTotalTicksRemaining(value * 25);
				if (prevMenaphiteVarbit > 0 && value < prevMenaphiteVarbit)
					men.setNextProcTicks(25); // proc just fired; next restore in 25 ticks
			}
			prevMenaphiteVarbit = value;
		}
		else if (varbitId == Varbits.ANTIFIRE)
		{
			int tick = client.getTickCount();
			EffectState efx = hudState.getEffects();
			if (value == 0)
			{
				efx.setNextAntifireTick(-1);
			}
			else if (efx.getNextAntifireTick() - tick <= 0)
			{
				// Phase unknown or expired — set the next boundary to one full interval away.
				efx.setNextAntifireTick(tick + 30);
			}
			efx.setAntifireVarbitValue(value);
		}
		else if (varbitId == Varbits.SUPER_ANTIFIRE)
		{
			int tick = client.getTickCount();
			EffectState efx = hudState.getEffects();
			if (value == 0)
			{
				efx.setNextSuperAntifireTick(-1);
			}
			else if (efx.getNextSuperAntifireTick() - tick <= 0)
			{
				efx.setNextSuperAntifireTick(tick + 20);
			}
			efx.setSuperAntifireVarbitValue(value);
		}
		else if (varbitId == Varbits.STAMINA_EFFECT)
		{
			// STAMINA_EFFECT is not a countdown varbit — it's a binary flag (1 = active, 0 = done).
			// We self-track the duration: 200 ticks (2 min) per dose, reset on each activation.
			if (value > 0)
				hudState.getEffects().setStaminaEffectTicks(200);
			else
				hudState.getEffects().setStaminaEffectTicks(0);
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
			applyPoisonVarp(value);
		}
	}

	// ── Self freeze detection ─────────────────────────────────────────────────

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Actor actor = event.getActor();

		if (actor == client.getLocalPlayer())
		{
			int       tick = client.getTickCount();
			SelfState self = hudState.getSelf();
			for (ActorSpotAnim sa : actor.getSpotAnims())
			{
				int id = sa.getId();
				if (id == SpotanimID.BIND_IMPACT
					|| id == SpotanimID.SNARE_IMPACT
					|| id == SpotanimID.ENTANGLE_IMPACT)
				{
					// Standard binding spells: start timer directly from impact graphic,
					// matching RuneLite's TimersAndBuffsPlugin behaviour.
					self.applyFreeze(tick, freezeTicksForGraphic(id), freezeSpriteIdForGraphic(id));
					return;
				}
				if (freezeTicksForGraphic(id) > 0)
				{
					// Ice spells: cache for same-tick chat-message confirmation.
					lastFreezeGraphicId   = id;
					lastFreezeGraphicTick = tick;
					break;
				}
			}
			return;
		}
		// Opponent Vengeance detection removed — deferred post-v0.1.
	}

	// ── Game tick ─────────────────────────────────────────────────────────────

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();

		SelfState self = hudState.getSelf();

		// Movement while frozen proves the freeze has expired — clear immediately.
		// Guard against the tick the freeze was applied (position may be stale).
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			WorldPoint pos = localPlayer.getWorldLocation();
			if (self.isFrozen(tick) && lastPlayerPos != null && !pos.equals(lastPlayerPos)
				&& tick != self.getFreezeStartTick())
				self.clearFreeze();
			lastPlayerPos = pos;
		}

		if (self.getHpRegenTicksRemaining() > 0)
			self.setHpRegenTicksRemaining(self.getHpRegenTicksRemaining() - 1);
		self.getBoostDecay().update(tick, client.isPrayerActive(Prayer.PRESERVE));
		self.getDebuffRestore().tick();

		EffectState fxTick = hudState.getEffects();
		if (fxTick.getStaminaEffectTicks() > 0)
			fxTick.setStaminaEffectTicks(fxTick.getStaminaEffectTicks() - 1);
		fxTick.getMenaphite().tick();
		hudState.getPoison().tick();

		ActionClockState clock = hudState.getActionClock();
		if (clock.getAttackDelayTicks() > 0) clock.setAttackDelayTicks(clock.getAttackDelayTicks() - 1);
		if (clock.getEatCooldownTicks() > 0) clock.setEatCooldownTicks(clock.getEatCooldownTicks() - 1);
		if (clock.getPotCooldownTicks() > 0) clock.setPotCooldownTicks(clock.getPotCooldownTicks() - 1);

		// Run energy: polled each tick (no reliable varbit event fires for it)
		hudState.getSelf().setRunEnergy(client.getEnergy() / 100);

		hudState.getProtection().tick();

		// Expire stale fight session (no combat for ~30 s)
		PvpFightSession session = hudState.getCurrentSession();
		if (session != null && session.isStale(tick))
		{
			hudState.endSession();
			hudState.getOpponent().reset();
		}

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
			case "Attack":
			{
				// Confirm combat immediately on an explicit Attack click on a player.
				Actor menuActor = event.getMenuEntry().getActor();
				if (menuActor instanceof Player)
				{
					Player p = (Player) menuActor;
					if (p.getName() != null) confirmCombatCandidate(p.getName(), p);
				}
				break;
			}
		}
	}

	// ── Animation — attack delay ─────────────────────────────────────────────

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer())
		{
			int anim = client.getLocalPlayer().getAnimation();
			if (anim != -1 && client.getLocalPlayer().getInteracting() != null)
			{
				int speed = getWeaponSpeed();
				hudState.getActionClock().setAttackDelayTicks(speed);
				hudState.getActionClock().setLastWeaponSpeedTicks(speed);
			}
		}
		// Opponent Vengeance animation detection removed — deferred post-v0.1.
	}

	// ── Interacting changed — combat candidate tracking ──────────────────────

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		Actor source = event.getSource();
		Actor target = event.getTarget();

		if (source == client.getLocalPlayer())
		{
			if (!(target instanceof Player))
			{
				// Targeting a non-player (NPC, nothing) — drop the pending candidate.
				// Do NOT clear the candidate on null target so the session survives brief
				// disengagement (e.g. movement flickers between attacks).
				if (target != null)
				{
					pendingOpponentName  = null;
					pendingOpponentActor = null;
				}
				return;
			}

			Player targetPlayer = (Player) target;
			String name = targetPlayer.getName();
			if (name == null) return;

			PvpFightSession session = hudState.getCurrentSession();

			// Re-targeting the current session opponent — keep session alive.
			if (session != null && name.equalsIgnoreCase(session.getOpponentName()))
			{
				hudState.getOpponent().setName(name);
				hudState.getOpponent().setCachedActor(targetPlayer);
				return;
			}

			// New target — record as candidate; wait for combat confirmation.
			pendingOpponentName  = name;
			pendingOpponentActor = targetPlayer;
		}
		else if (source instanceof Player && target == client.getLocalPlayer())
		{
			// Another player targeted us — record as candidate only if no session exists.
			// Combat is confirmed when a hitsplat from them lands on us.
			if (hudState.getCurrentSession() != null) return;
			Player sourcePlayer = (Player) source;
			String name = sourcePlayer.getName();
			if (name == null) return;
			pendingOpponentName  = name;
			pendingOpponentActor = sourcePlayer;
		}
	}

	/**
	 * Starts (or refreshes) a fight session for the given player.
	 * Safe to call multiple times for the same opponent.
	 */
	private void confirmCombatCandidate(String name, Player actor)
	{
		PvpFightSession session = hudState.getCurrentSession();
		if (session != null && name.equalsIgnoreCase(session.getOpponentName()))
		{
			hudState.getOpponent().setName(name);
			hudState.getOpponent().setCachedActor(actor);
			return;
		}
		hudState.getOpponent().reset();
		hudState.getOpponent().setName(name);
		hudState.getOpponent().setCachedActor(actor);
		hudState.beginSession(name, client.getTickCount());
		enqueueHiscoresLookup(name);
		pendingOpponentName  = null;
		pendingOpponentActor = null;
	}

	// ── Hitsplat events — combat log and HP estimation ────────────────────────

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();

		if (actor == client.getLocalPlayer())
		{
			int dmg = event.getHitsplat().getAmount();

			// Incoming hitsplat confirms the pending candidate if they are currently
			// interacting with the local player (i.e., they are the attacker).
			if (pendingOpponentName != null && hudState.getCurrentSession() == null
				&& pendingOpponentActor != null
				&& pendingOpponentActor.getInteracting() == client.getLocalPlayer())
			{
				confirmCombatCandidate(pendingOpponentName, pendingOpponentActor);
			}

			// PJ safe, logout lock, and under-attack timers refresh on ANY incoming
			// attack — including 0-damage splashes (OSRS uses attacks, not damage).
			hudState.getProtection().onAttackExchanged(getPjTimerTicks());
			hudState.getProtection().onIncomingHit();
			hudState.getSelf().setLastIncomingDamageMs(System.currentTimeMillis());

			if (dmg <= 0) return;

			PvpFightSession session = hudState.getCurrentSession();
			if (session == null) return;

			// Smite drains floor(dmg / 4) — 25% rounded down
			PrayerEffect effect   = PrayerEffect.NONE;
			int prayerDrain       = 0;
			OpponentState opp     = hudState.getOpponent();
			if (opp.isSmiteActive())
			{
				effect      = PrayerEffect.SMITE;
				prayerDrain = dmg / 4;
			}

			int     tick  = client.getTickCount();
			boolean stack = session.recordAndCheckIncomingStack(tick);

			session.onIncomingHit(
				new CombatEvent(CombatEventType.INCOMING_HIT, dmg,
					prayerDrain, 0, effect, false, stack, System.currentTimeMillis()),
				tick);
		}
		else if (actor instanceof Player)
		{
			String actorName = actor.getName();
			if (actorName == null) return;

			// Outgoing hitsplat on the pending candidate confirms the session.
			if (pendingOpponentName != null && actorName.equalsIgnoreCase(pendingOpponentName))
			{
				Player pending = pendingOpponentActor != null ? pendingOpponentActor : (Player) actor;
				confirmCombatCandidate(pendingOpponentName, pending);
			}

			// Outgoing hit: hitsplat on the tracked opponent
			OpponentState opp = hudState.getOpponent();
			if (!opp.isTracked() || !actorName.equalsIgnoreCase(opp.getName()))
			{
				return;
			}

			// PJ safe timer refreshes on ANY outgoing attack, including 0-damage.
			hudState.getProtection().onAttackExchanged(getPjTimerTicks());

			int dmg = event.getHitsplat().getAmount();
			if (dmg <= 0) return;

			handleOutgoingHit(opp, dmg);
		}
	}

	private void handleOutgoingHit(OpponentState opp, int damage)
	{
		opp.setLastOutgoingHit(damage);

		if (opp.getEstimatedHp() > 0)
			opp.setEstimatedHp(Math.max(0, opp.getEstimatedHp() - damage));

		PvpFightSession session = hudState.getCurrentSession();
		if (session == null) return;

		// Outgoing Smite: local player's overhead icon determines if Smite is active
		PrayerEffect outEffect      = PrayerEffect.NONE;
		int          outPrayerDrain = 0;
		Player       local          = client.getLocalPlayer();
		if (local != null && local.getOverheadIcon() == HeadIcon.SMITE)
		{
			outEffect      = PrayerEffect.SMITE;
			outPrayerDrain = damage / 4;
		}

		int     tick  = client.getTickCount();
		boolean stack = session.recordAndCheckOutgoingStack(tick);

		session.onOutgoingHit(
			new CombatEvent(CombatEventType.OUTGOING_HIT, damage,
				outPrayerDrain, 0, outEffect, false, stack, System.currentTimeMillis()),
			tick);
	}

	// ── HUD visibility predicate ─────────────────────────────────────────────

	/**
	 * Single source of truth for whether the HUD should render.
	 * Extracted as a static method so it is unit-testable without a live Client.
	 */
	static boolean computeShouldShow(boolean enabled, HudMode mode, boolean inPvpZone, boolean hasSession)
	{
		if (!enabled) return false;
		switch (mode)
		{
			case MANUAL:   return true;
			case PVP_AREA: return inPvpZone;
			case AUTO:     return hasSession;
			default:       return false;
		}
	}

	boolean shouldShowHud()
	{
		return computeShouldShow(
			config.hudVisible(),
			config.hudMode(),
			hudState.getContext().isInPvpZone(),
			hudState.getCurrentSession() != null);
	}

	// ── Environment polling ───────────────────────────────────────────────────

	private void updateEnvironment()
	{
		boolean inWilderness = client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
		java.util.Set<WorldType> worldTypes = client.getWorldType();
		boolean onPvpWorld   = worldTypes != null && worldTypes.contains(WorldType.PVP);
		hudState.getContext().setInPvpZone(inWilderness || onPvpWorld);
		int wildLevel = 0;
		if (inWilderness)
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

		// Use cached Player reference; fall back to a full scan if it is stale
		Player p = opp.getCachedActor();
		if (p == null || p.getName() == null || !p.getName().equalsIgnoreCase(opp.getName()))
		{
			p = null;
			List<Player> players = client.getPlayers();
			if (players != null)
			{
				for (Player candidate : players)
				{
					if (candidate != null && candidate.getName() != null
						&& candidate.getName().equalsIgnoreCase(opp.getName()))
					{
						p = candidate;
						opp.setCachedActor(p);
						break;
					}
				}
			}
			if (p == null) return;
		}

		opp.setOverheadPrayer(p.getOverheadIcon());

		int ratio = p.getHealthRatio();
		int scale = p.getHealthScale();
		if (ratio < 0 || scale <= 0) return;

		// Terminate fight session when opponent HP reaches 0
		if (ratio == 0)
		{
			hudState.getProtection().onAttackExchanged(getPjTimerTicks());
			if (isInLms()) hudState.getProtection().onKillInLms();
			hudState.endSession();
			hudState.getOpponent().reset();
			return;
		}

		// Hiscores HP is authoritative when available.
		int hiscoresHp = opp.getStats().getHitpoints();
		if (hiscoresHp > 0)
		{
			opp.setMaxHp(hiscoresHp);
			opp.setEstimatedHp((int) Math.round(hiscoresHp * (double) ratio / scale));
			return;
		}

		// Fall back to back-calculation from damage dealt until hiscores load.
		PvpFightSession session   = hudState.getCurrentSession();
		int             totalDealt = session != null ? session.getTotalOutgoing() : 0;

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

	/**
	 * Decode raw {@code Varbits.TELEBLOCK} value to active TB ticks remaining.
	 * Raw 0      = not active, no immunity.
	 * Raw 1..100 = post-TB reapplication immunity only (TB itself has expired).
	 * Raw 101+   = TB active; remaining = raw - 100.
	 * Mirrors RuneLite TimersAndBuffsPlugin: {@code event.getValue() - 100}.
	 */
	static int decodeTeleblockTicks(int raw)
	{
		return Math.max(0, raw - 100);
	}

	static int freezeTicksForGraphic(int graphicId)
	{
		switch (graphicId)
		{
			case GraphicID.ICE_RUSH:           return 8;
			case GraphicID.ICE_BURST:          return 16;
			case GraphicID.ICE_BLITZ:          return 24;
			case GraphicID.ICE_BARRAGE:        return 32;
			case SpotanimID.BIND_IMPACT:       return 8;
			case SpotanimID.SNARE_IMPACT:      return 16;
			case SpotanimID.ENTANGLE_IMPACT:   return 24;
			default:                           return 0;
		}
	}

	static int freezeSpriteIdForGraphic(int graphicId)
	{
		switch (graphicId)
		{
			case GraphicID.ICE_RUSH:           return SpriteID.SPELL_ICE_RUSH;
			case GraphicID.ICE_BURST:          return SpriteID.SPELL_ICE_BURST;
			case GraphicID.ICE_BLITZ:          return SpriteID.SPELL_ICE_BLITZ;
			case GraphicID.ICE_BARRAGE:        return SpriteID.SPELL_ICE_BARRAGE;
			case SpotanimID.BIND_IMPACT:       return SpriteID.SPELL_BIND;
			case SpotanimID.SNARE_IMPACT:      return SpriteID.SPELL_SNARE;
			case SpotanimID.ENTANGLE_IMPACT:   return SpriteID.SPELL_ENTANGLE;
			default:                           return SpriteID.SPELL_ICE_BARRAGE;
		}
	}

	// ── Freeze detection — chat message is the authoritative trigger ──────────────

	/**
	 * "You have been frozen!" is a SPAM-type game message that fires exactly once
	 * when a freeze successfully applies. Using it (rather than the graphic) as the
	 * trigger ensures repeated ice hits while already frozen do not extend the timer.
	 *
	 * The same-tick graphic (captured in onGraphicChanged or read from live spot
	 * anims) is used only to identify which spell was cast so the correct base
	 * duration can be selected — it does not drive the freeze by itself.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SPAM) return;
		String msg = Text.removeTags(event.getMessage());
		if (!msg.equals("You have been frozen!") && !msg.equals("You are frozen.")) return;

		int       tick  = client.getTickCount();
		SelfState self  = hudState.getSelf();

		// Try live spot anims first (most reliable when graphic fires before message)
		int graphicId = -1;
		Player local = client.getLocalPlayer();
		if (local != null)
		{
			for (ActorSpotAnim sa : local.getSpotAnims())
			{
				if (freezeTicksForGraphic(sa.getId()) > 0)
				{
					graphicId = sa.getId();
					break;
				}
			}
		}
		// Fall back to the cached graphic if it landed on the same tick
		if (graphicId < 0 && lastFreezeGraphicTick == tick)
		{
			graphicId = lastFreezeGraphicId;
		}

		int baseTicks = graphicId >= 0 ? freezeTicksForGraphic(graphicId) : 32;
		int duration  = adjustedFreezeTicks(graphicId, baseTicks);
		int spriteId  = graphicId >= 0 ? freezeSpriteIdForGraphic(graphicId) : SpriteID.SPELL_ICE_BARRAGE;

		self.applyFreeze(tick, duration, spriteId);
	}

	private int adjustedFreezeTicks(int graphicId, int baseTicks)
	{
		return baseTicks;
	}

	// ── Protection helpers ────────────────────────────────────────────────────────

	/** PvP worlds use 16-tick PJ protection (since 25 Mar 2026); Wilderness uses 20. */
	private int getPjTimerTicks()
	{
		return client.getWorldType().contains(WorldType.PVP) ? 16 : 20;
	}

	/** Returns true when the player is inside Last Man Standing. */
	private boolean isInLms()
	{
		// TODO: detect LMS via region check or varbit
		return false;
	}

	// ── Poison / venom / immunity ─────────────────────────────────────────────────

	/** Mirrors RuneLite's TimersAndBuffsPlugin threshold. */
	private static final int VENOM_VALUE_CUTOFF = -38;

	/**
	 * Applies the POISON VarPlayer value to PoisonState using RuneLite's
	 * phase-aware nextPoisonTick formula.
	 * Positive = poisoned/venomed; negative = immunity.
	 * Values strictly below VENOM_VALUE_CUTOFF (-38) are anti-venom;
	 * values from -1 to VENOM_VALUE_CUTOFF (-38) inclusive are anti-poison.
	 */
	private void applyPoisonVarp(int value)
	{
		PoisonState ps  = hudState.getPoison();
		int         now = client.getTickCount();
		ps.setVenomed(value >= 1_000_000);
		ps.setPoisoned(value > 0 && value < 1_000_000);
		if (value < VENOM_VALUE_CUTOFF)
		{
			ps.setAntiVenomActive(true);
			ps.setAntiPoisonActive(false);
			ps.setPoisonVarpValue(value);
			ps.setNextPoisonTick(now + PoisonState.POISON_TICK_LENGTH);
		}
		else if (value < 0)
		{
			ps.setAntiPoisonActive(true);
			ps.setAntiVenomActive(false);
			ps.setPoisonVarpValue(value);
			ps.setNextPoisonTick(now + PoisonState.POISON_TICK_LENGTH);
		}
		else
		{
			ps.setAntiVenomActive(false);
			ps.setAntiPoisonActive(false);
			ps.setPoisonVarpValue(0);
			ps.setNextPoisonTick(-1);
		}
	}

	// ── Hiscores lookup ───────────────────────────────────────────────────────────

	private void enqueueHiscoresLookup(String playerName)
	{
		String key = playerName.toLowerCase();
		if (!lookedUp.add(key)) return; // already queued

		hiscoreClient.lookupAsync(playerName, HiscoreEndpoint.NORMAL)
			.whenComplete((result, ex) ->
			{
				if (ex != null || result == null)
				{
					log.debug("Hiscores lookup failed for {}: {}", playerName,
						ex != null ? ex.getMessage() : "null result");
					return;
				}
				applyHiscoresResult(playerName, result);
			});
	}

	private void applyHiscoresResult(String playerName, HiscoreResult result)
	{
		int attack    = skillLevel(result, HiscoreSkill.ATTACK);
		int defence   = skillLevel(result, HiscoreSkill.DEFENCE);
		int strength  = skillLevel(result, HiscoreSkill.STRENGTH);
		int hitpoints = skillLevel(result, HiscoreSkill.HITPOINTS);
		int ranged    = skillLevel(result, HiscoreSkill.RANGED);
		int magic     = skillLevel(result, HiscoreSkill.MAGIC);

		clientThread.invoke(() ->
		{
			OpponentState opp = hudState.getOpponent();
			if (!opp.isTracked() || !playerName.equalsIgnoreCase(opp.getName())) return;
			opp.getStats().setAttack(attack);
			opp.getStats().setDefence(defence);
			opp.getStats().setStrength(strength);
			opp.getStats().setHitpoints(hitpoints);
			opp.getStats().setRanged(ranged);
			opp.getStats().setMagic(magic);
			// Seed maxHp from hiscores — overrides the rough back-calculation.
			if (hitpoints > 0) opp.setMaxHp(hitpoints);
		});
	}

	private static int skillLevel(HiscoreResult result, HiscoreSkill skill)
	{
		net.runelite.client.hiscore.Skill s = result.getSkill(skill);
		return s != null && s.getLevel() > 0 ? s.getLevel() : -1;
	}

	@Provides
	PvpHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpHudConfig.class);
	}
}
