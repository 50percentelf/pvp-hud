package com.pvphud;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PvpHudTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PvpHudPlugin.class);
		RuneLite.main(args);
	}
}
