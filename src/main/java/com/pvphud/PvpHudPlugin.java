package com.pvphud;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
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

	@Override
	protected void startUp() throws Exception
	{
		log.info("PvP HUD started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("PvP HUD stopped");
	}

	@Provides
	PvpHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpHudConfig.class);
	}
}
