package com.pvphud;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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

	@Getter
	private final PvpHudState hudState = new PvpHudState();

	@Override
	protected void startUp() throws Exception
	{
		hudState.fullReset();
		hudState.getContext().setMode(config.hudMode());
		log.info("PvP HUD started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		hudState.fullReset();
		log.info("PvP HUD stopped");
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

	@Provides
	PvpHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpHudConfig.class);
	}
}
